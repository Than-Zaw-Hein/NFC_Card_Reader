package com.tzh.nfx_card_reader.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.coroutines.resume

class UsbNfcManager(private val context: Context) {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    // CCID fields
    private var currentConnection: UsbDeviceConnection? = null
    private var currentInterface: UsbInterface? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null
    private var sequenceNumber: Byte = 0

    // USB Serial fields
    private var serialPort: UsbSerialPort? = null
    private var serialIoManager: SerialInputOutputManager? = null
    private var isSerialMode: Boolean = false
    private val receivedBuffer = ByteArrayOutputStream()

    // Listeners and Flows for continuous data reading
    private var onSerialDataReceivedListener: ((ByteArray) -> Unit)? = null
    private val _serialDataFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val serialDataFlow: SharedFlow<ByteArray> = _serialDataFlow.asSharedFlow()

    companion object {
        const val ACTION_USB_PERMISSION = "com.tzh.nfx_card_reader.USB_PERMISSION"
        private const val TAG = "UsbNfcManager"
        private const val ASIX_VENDOR_ID = 0x0B95
        private const val ASIX_PRODUCT_ID = 0x1790
        private const val CCID_CLASS = 11 // USB_CLASS_CSID / Smart Card (0x0B)
        private const val MSG_PC_TO_RDR_ICC_POWER_ON = 0x62.toByte()
        private const val TIMEOUT_MS = 5000
        private const val SERIAL_BAUD_RATE = 115200
    }

    /**
     * Checks if a device is an ASIX USB-to-UART bridge (0x0B95 / 0x1790).
     */
    fun isAsixDevice(device: UsbDevice): Boolean {
        return device.vendorId == ASIX_VENDOR_ID && device.productId == ASIX_PRODUCT_ID
    }

    /**
     * Checks if a device has a CDC-ACM interface or class.
     */
    fun isCdcAcmDevice(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_COMM) return true
        for (i in 0 until device.interfaceCount) {
            val usbInterface = device.getInterface(i)
            if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_COMM ||
                usbInterface.interfaceClass == 0x0A // USB_CLASS_CDC_DATA
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Checks if a device is recognized as a USB Serial device.
     */
    fun isSerialDevice(device: UsbDevice): Boolean {
        if (isAsixDevice(device) || isCdcAcmDevice(device)) return true
        val customTable = ProbeTable()
        customTable.addProduct(ASIX_VENDOR_ID, ASIX_PRODUCT_ID, CdcAcmSerialDriver::class.java)
        val customProber = UsbSerialProber(customTable)
        if (customProber.probeDevice(device) != null) return true
        return UsbSerialProber.getDefaultProber().probeDevice(device) != null
    }

    /**
     * Checks if a device is a CCID / Smart Card device.
     */
    fun isCcidDevice(device: UsbDevice): Boolean {
        if (device.deviceClass == CCID_CLASS) return true
        for (i in 0 until device.interfaceCount) {
            val usbInterface = device.getInterface(i)
            if (usbInterface.interfaceClass == CCID_CLASS || usbInterface.interfaceClass == 0xFF) {
                return true
            }
        }
        return false
    }

    /**
     * Finds connected CCID or USB Serial devices.
     */
    fun findConnectedCcidDevice(): UsbDevice? {
        val deviceList = usbManager.deviceList
        // Priority 1: ASIX USB-to-UART bridge
        for (device in deviceList.values) {
            if (isAsixDevice(device)) {
                return device
            }
        }
        // Priority 2: Any USB Serial / CDC-ACM device
        for (device in deviceList.values) {
            if (isSerialDevice(device)) {
                return device
            }
        }
        // Priority 3: CCID Smart Card device
        for (device in deviceList.values) {
            if (isCcidDevice(device)) {
                return device
            }
        }
        // Fallback: Any connected USB device
        return deviceList.values.firstOrNull()
    }

    /**
     * Requests USB permission for the given device using a coroutine.
     */
    suspend fun requestPermission(device: UsbDevice): Boolean = suspendCancellableCoroutine { continuation ->
        if (usbManager.hasPermission(device)) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), flags)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == ACTION_USB_PERMISSION) {
                    try {
                        context.unregisterReceiver(this)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error unregistering receiver", e)
                    }
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    continuation.resume(granted)
                }
            }
        }

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )

        usbManager.requestPermission(device, permissionIntent)

        continuation.invokeOnCancellation {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver on cancellation", e)
            }
        }
    }

    /**
     * Connects to the USB device. First attempts USB Serial / CDC-ACM connection
     * (with custom ProbeTable for ASIX 0x0B95 / 0x1790), then falls back to standard CCID bulk transfers.
     */
    suspend fun connect(device: UsbDevice): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!usbManager.hasPermission(device)) {
                return@withContext Result.failure(IOException("USB permission not granted"))
            }

            // Release any previous connection
            disconnect()

            Log.i(TAG, "--- USB Device Inspection ---")
            Log.i(TAG, "Device Name: ${device.deviceName}")
            Log.i(TAG, "Vendor ID: 0x${device.vendorId.toString(16)} (${device.vendorId}), Product ID: 0x${device.productId.toString(16)} (${device.productId})")
            Log.i(TAG, "Device Class: ${device.deviceClass}, Subclass: ${device.deviceSubclass}, Protocol: ${device.deviceProtocol}")
            Log.i(TAG, "Total Interfaces: ${device.interfaceCount}")

            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                Log.i(TAG, "  Interface [$i]: id=${iface.id}, class=${iface.interfaceClass}, subclass=${iface.interfaceSubclass}, protocol=${iface.interfaceProtocol}, endpointCount=${iface.endpointCount}")
                for (j in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(j)
                    val typeStr = when (ep.type) {
                        UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
                        UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "CONTROL"
                        UsbConstants.USB_ENDPOINT_XFER_INT -> "INT"
                        UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISOC"
                        else -> "UNKNOWN (${ep.type})"
                    }
                    val dirStr = if (ep.direction == UsbConstants.USB_DIR_IN) "IN" else "OUT"
                    Log.i(TAG, "    Endpoint [$j]: address=0x${ep.address.toString(16)}, number=${ep.endpointNumber}, direction=$dirStr, type=$typeStr, maxPacketSize=${ep.maxPacketSize}")
                }
            }
            Log.i(TAG, "-----------------------------")

            // 1. Try USB Serial / CDC-ACM mode if applicable
            if (isSerialDevice(device) || isAsixDevice(device)) {
                Log.i(TAG, "Attempting USB Serial / CDC-ACM connection for Vendor ID 0x${device.vendorId.toString(16)}, Product ID 0x${device.productId.toString(16)}")
                val serialResult = connectSerial(device)
                if (serialResult.isSuccess) {
                    return@withContext serialResult
                }
                Log.w(TAG, "USB Serial connection failed. Falling back to CCID bulk transfer mode...")
            }

            // 2. Fallback: Standard CCID bulk transfer mode
            return@withContext connectCcid(device)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during connect", e)
            Result.failure(e)
        }
    }

    /**
     * Establishes a USB Serial / CDC-ACM connection using usb-serial-for-android.
     */
    private fun connectSerial(device: UsbDevice): Result<Unit> {
        try {
            // Step 1: Probe using default prober
            var driver: UsbSerialDriver? = UsbSerialProber.getDefaultProber().probeDevice(device)

            // Step 2: Register custom ProbeTable for ASIX Vendor ID 0x0B95 / Product ID 0x1790
            if (driver == null) {
                val customTable = ProbeTable()
                customTable.addProduct(ASIX_VENDOR_ID, ASIX_PRODUCT_ID, CdcAcmSerialDriver::class.java)
                val customProber = UsbSerialProber(customTable)
                driver = customProber.probeDevice(device)
            }

            // Step 3: Direct driver instantiation fallback for CDC-ACM
            if (driver == null) {
                Log.i(TAG, "Prober returned null. Instantiating CdcAcmSerialDriver directly for device 0x${device.vendorId.toString(16)}:0x${device.productId.toString(16)}")
                driver = CdcAcmSerialDriver(device)
            }

            val connection = usbManager.openDevice(driver.device)
                ?: return Result.failure(IOException("Failed to open USB device connection for serial driver"))

            val port = driver.ports.firstOrNull()
                ?: run {
                    connection.close()
                    return Result.failure(IOException("No serial ports found on driver"))
                }

            port.open(connection)
            // Configure serial parameters: 115200 baud rate, 8 data bits, 1 stop bit, no parity
            port.setParameters(
                SERIAL_BAUD_RATE,
                UsbSerialPort.DATABITS_8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )

            synchronized(receivedBuffer) {
                receivedBuffer.reset()
            }

            // Start continuous background reading of serial data
            val ioListener = object : SerialInputOutputManager.Listener {
                override fun onNewData(data: ByteArray) {
                    if (data.isNotEmpty()) {
                        val hexStr = data.joinToString("") { "%02X".format(it) }
                        Log.d(TAG, "Continuous Serial RX (${data.size} bytes): $hexStr")
                        synchronized(receivedBuffer) {
                            receivedBuffer.write(data)
                        }
                        _serialDataFlow.tryEmit(data)
                        onSerialDataReceivedListener?.invoke(data)
                    }
                }

                override fun onRunError(e: Exception) {
                    Log.e(TAG, "Serial IO Manager error: ${e.message}")
                }
            }

            val ioManager = SerialInputOutputManager(port, ioListener)
            serialIoManager = ioManager
            Executors.newSingleThreadExecutor().submit(ioManager)

            serialPort = port
            currentConnection = connection
            isSerialMode = true

            Log.i(TAG, "Successfully opened USB Serial connection (115200 baud, 8N1) for device ${device.deviceName}")
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish USB Serial connection", e)
            return Result.failure(e)
        }
    }

    /**
     * Connects via standard CCID bulk transfers.
     */
    private fun connectCcid(device: UsbDevice): Result<Unit> {
        val connection = usbManager.openDevice(device)
            ?: return Result.failure(IOException("Failed to open USB device connection"))

        var targetInterface: UsbInterface? = null
        var inEp: UsbEndpoint? = null
        var outEp: UsbEndpoint? = null

        // First pass: look specifically for CCID class interface (11 / 0x0B) or vendor-specific smart card interface (0xFF)
        for (i in 0 until device.interfaceCount) {
            val usbInterface = device.getInterface(i)
            if (usbInterface.interfaceClass == CCID_CLASS || usbInterface.interfaceClass == 0xFF) {
                var tempIn: UsbEndpoint? = null
                var tempOut: UsbEndpoint? = null

                for (j in 0 until usbInterface.endpointCount) {
                    val ep = usbInterface.getEndpoint(j)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.direction == UsbConstants.USB_DIR_IN) {
                            tempIn = ep
                        } else if (ep.direction == UsbConstants.USB_DIR_OUT) {
                            tempOut = ep
                        }
                    }
                }

                if (tempIn != null && tempOut != null) {
                    targetInterface = usbInterface
                    inEp = tempIn
                    outEp = tempOut
                    break
                }
            }
        }

        // Fallback pass: check any interface with valid bulk IN/OUT endpoints
        if (targetInterface == null || inEp == null || outEp == null) {
            for (i in 0 until device.interfaceCount) {
                val usbInterface = device.getInterface(i)
                var tempIn: UsbEndpoint? = null
                var tempOut: UsbEndpoint? = null

                for (j in 0 until usbInterface.endpointCount) {
                    val ep = usbInterface.getEndpoint(j)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.direction == UsbConstants.USB_DIR_IN) {
                            tempIn = ep
                        } else if (ep.direction == UsbConstants.USB_DIR_OUT) {
                            tempOut = ep
                        }
                    }
                }

                if (tempIn != null && tempOut != null) {
                    targetInterface = usbInterface
                    inEp = tempIn
                    outEp = tempOut
                    break
                }
            }
        }

        val finalInterface = targetInterface
        val finalInEp = inEp
        val finalOutEp = outEp

        if (finalInterface == null || finalInEp == null || finalOutEp == null) {
            connection.close()
            return Result.failure(IOException("Could not find CCID bulk IN/OUT endpoints"))
        }

        if (!connection.claimInterface(finalInterface, true)) {
            connection.close()
            return Result.failure(IOException("Failed to claim USB interface ${finalInterface.id}"))
        }

        try {
            val setIntfSuccess = connection.setInterface(finalInterface)
            Log.i(TAG, "connection.setInterface for interface ${finalInterface.id} returned: $setIntfSuccess")
        } catch (e: Exception) {
            Log.w(TAG, "connection.setInterface threw exception: ${e.message}")
        }

        clearEndpointHalt(connection, finalOutEp)
        clearEndpointHalt(connection, finalInEp)

        currentConnection = connection
        currentInterface = finalInterface
        endpointIn = finalInEp
        endpointOut = finalOutEp
        isSerialMode = false

        Log.i(TAG, "Successfully connected to CCID device interface ${finalInterface.id} with IN endpoint 0x${finalInEp.address.toString(16)} and OUT endpoint 0x${finalOutEp.address.toString(16)}")
        return Result.success(Unit)
    }

    private fun clearEndpointHalt(connection: UsbDeviceConnection, endpoint: UsbEndpoint) {
        try {
            val result = connection.controlTransfer(
                0x02, // USB_RECIP_ENDPOINT
                1,    // USB_REQ_CLEAR_FEATURE
                0,    // ENDPOINT_HALT
                endpoint.address,
                null,
                0,
                TIMEOUT_MS
            )
            Log.i(TAG, "ClearHalt for endpoint 0x${endpoint.address.toString(16)} result: $result")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear endpoint halt for 0x${endpoint.address.toString(16)}: ${e.message}")
        }
    }

    /**
     * Sends data over USB Serial connection.
     */
    suspend fun sendSerialData(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val port = serialPort
            ?: return@withContext Result.failure(IOException("USB Serial port not connected"))
        try {
            Log.d(TAG, "Sending USB Serial data (${data.size} bytes): ${data.joinToString("") { "%02X".format(it) }}")
            port.write(data, TIMEOUT_MS)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to USB Serial port", e)
            Result.failure(e)
        }
    }

    /**
     * Sets listener for continuous serial data reading.
     */
    fun setOnSerialDataListener(listener: ((ByteArray) -> Unit)?) {
        onSerialDataReceivedListener = listener
    }

    /**
     * Returns true if currently connected in USB Serial mode.
     */
    fun isSerialMode(): Boolean = isSerialMode

    /**
     * Gets and flushes the current received serial buffer bytes.
     */
    fun getReceivedSerialBytes(): ByteArray {
        return synchronized(receivedBuffer) {
            val bytes = receivedBuffer.toByteArray()
            receivedBuffer.reset()
            bytes
        }
    }

    /**
     * Powers on card. In CCID mode sends PC_TO_RDR_IccPowerOn (0x62); in USB Serial mode sends wake command or returns response bytes.
     */
    suspend fun powerOnCard(): Result<ByteArray> = withContext(Dispatchers.IO) {
        if (isSerialMode) {
            val port = serialPort
                ?: return@withContext Result.failure(IOException("USB Serial port not connected"))
            try {
                Log.d(TAG, "Serial powerOnCard called")
                synchronized(receivedBuffer) {
                    receivedBuffer.reset()
                }
                val wakeCmd = byteArrayOf(0x55.toByte(), 0x55.toByte(), 0x00, 0x00, 0x00)
                port.write(wakeCmd, TIMEOUT_MS)

                delay(100)
                val response = synchronized(receivedBuffer) {
                    val bytes = receivedBuffer.toByteArray()
                    receivedBuffer.reset()
                    bytes
                }
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Exception in serial powerOnCard", e)
                Result.failure(e)
            }
        } else {
            powerOnCardCcid()
        }
    }

    private fun powerOnCardCcid(): Result<ByteArray> {
        val connection = currentConnection
            ?: return Result.failure(IOException("Not connected to USB device"))
        val outEp = endpointOut
            ?: return Result.failure(IOException("Bulk OUT endpoint not available"))
        val inEp = endpointIn
            ?: return Result.failure(IOException("Bulk IN endpoint not available"))

        return try {
            val seq = sequenceNumber++
            val command = ByteArray(10)
            command[0] = MSG_PC_TO_RDR_ICC_POWER_ON
            command[1] = 0x00
            command[2] = 0x00
            command[3] = 0x00
            command[4] = 0x00
            command[5] = 0x00 // bSlot = 0
            command[6] = seq   // bSeq
            command[7] = 0x01 // bPowerSelect: 0x01 (5V) or 0x02 (3V)
            command[8] = 0x00
            command[9] = 0x00

            Log.d(TAG, "Sending PC_TO_RDR_IccPowerOn (seq=$seq, size=${command.size}) to endpoint 0x${outEp.address.toString(16)}")
            var transferredOut = connection.bulkTransfer(outEp, command, 0, command.size, TIMEOUT_MS)

            if (transferredOut < 0) {
                Log.w(TAG, "bulkTransfer OUT returned $transferredOut (-1). Attempting clearHalt and retry...")
                clearEndpointHalt(connection, outEp)
                transferredOut = connection.bulkTransfer(outEp, command, 0, command.size, TIMEOUT_MS)
            }

            if (transferredOut != command.size) {
                Result.failure(IOException("Failed to send PC_TO_RDR_IccPowerOn bulk transfer (transferred: $transferredOut, expected: ${command.size})"))
            } else {
                val responseBuffer = ByteArray(271)
                Log.d(TAG, "Reading response from IN endpoint 0x${inEp.address.toString(16)}...")
                var transferredIn = connection.bulkTransfer(inEp, responseBuffer, 0, responseBuffer.size, TIMEOUT_MS)

                if (transferredIn < 0) {
                    Log.w(TAG, "bulkTransfer IN returned $transferredIn (-1). Attempting clearHalt and retry...")
                    clearEndpointHalt(connection, inEp)
                    transferredIn = connection.bulkTransfer(inEp, responseBuffer, 0, responseBuffer.size, TIMEOUT_MS)
                }

                if (transferredIn < 10) {
                    Result.failure(IOException("Invalid or empty response from reader (transferred: $transferredIn)"))
                } else {
                    val dataLength = ((responseBuffer[1].toInt() and 0xFF)) or
                            ((responseBuffer[2].toInt() and 0xFF) shl 8) or
                            ((responseBuffer[3].toInt() and 0xFF) shl 16) or
                            ((responseBuffer[4].toInt() and 0xFF) shl 24)
                    val status = responseBuffer[7]
                    val error = responseBuffer[8]

                    if (status != 0.toByte()) {
                        val cmdStatus = (status.toInt() and 0x03)
                        if (cmdStatus == 1) {
                            return Result.failure(IOException("CCID command failed with status: 0x${status.toString(16)}, error: 0x${error.toString(16)}"))
                        }
                    }

                    if (dataLength > 0 && transferredIn >= 10 + dataLength) {
                        val atrBytes = responseBuffer.copyOfRange(10, 10 + dataLength)
                        Result.success(atrBytes)
                    } else if (dataLength == 0) {
                        Result.success(ByteArray(0))
                    } else {
                        Result.failure(IOException("Truncated CCID data block response (expected ${10 + dataLength}, got $transferredIn)"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in powerOnCardCcid", e)
            Result.failure(e)
        }
    }

    /**
     * Transmits APDU command or raw frame over USB Serial or CCID bulk transfer.
     */
    suspend fun transmitApdu(apduCommand: ByteArray): Result<ByteArray> = withContext(Dispatchers.IO) {
        if (isSerialMode) {
            val port = serialPort
                ?: return@withContext Result.failure(IOException("USB Serial port not connected"))
            try {
                synchronized(receivedBuffer) {
                    receivedBuffer.reset()
                }
                Log.d(TAG, "Sending command over USB Serial (${apduCommand.size} bytes)...")
                port.write(apduCommand, TIMEOUT_MS)

                // Poll for response bytes up to TIMEOUT_MS
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < TIMEOUT_MS) {
                    delay(50)
                    val currentSize = synchronized(receivedBuffer) { receivedBuffer.size() }
                    if (currentSize > 0) {
                        delay(50) // Allow complete frame to land
                        break
                    }
                }

                val respBytes = synchronized(receivedBuffer) {
                    val bytes = receivedBuffer.toByteArray()
                    receivedBuffer.reset()
                    bytes
                }
                Log.d(TAG, "Read ${respBytes.size} bytes response from USB Serial")
                Result.success(respBytes)
            } catch (e: Exception) {
                Log.e(TAG, "Exception in serial transmitApdu", e)
                Result.failure(e)
            }
        } else {
            transmitApduCcid(apduCommand)
        }
    }

    private fun transmitApduCcid(apduCommand: ByteArray): Result<ByteArray> {
        val connection = currentConnection
            ?: return Result.failure(IOException("Not connected to USB device"))
        val outEp = endpointOut
            ?: return Result.failure(IOException("Bulk OUT endpoint not available"))
        val inEp = endpointIn
            ?: return Result.failure(IOException("Bulk IN endpoint not available"))

        return try {
            val seq = sequenceNumber++
            val dataLen = apduCommand.size
            val header = ByteArray(10)
            header[0] = 0x6F.toByte() // PC_TO_RDR_XfrBlock
            header[1] = (dataLen and 0xFF).toByte()
            header[2] = ((dataLen shr 8) and 0xFF).toByte()
            header[3] = ((dataLen shr 16) and 0xFF).toByte()
            header[4] = ((dataLen shr 24) and 0xFF).toByte()
            header[5] = 0x00 // Slot 0
            header[6] = seq
            header[7] = 0x00 // BWI
            header[8] = 0x00
            header[9] = 0x00

            val packet = ByteArray(10 + dataLen)
            System.arraycopy(header, 0, packet, 0, 10)
            System.arraycopy(apduCommand, 0, packet, 10, dataLen)

            Log.d(TAG, "Sending PC_TO_RDR_XfrBlock (seq=$seq, apduSize=$dataLen, totalPacketSize=${packet.size}) to endpoint 0x${outEp.address.toString(16)}")
            var transferredOut = connection.bulkTransfer(outEp, packet, 0, packet.size, TIMEOUT_MS)

            if (transferredOut < 0) {
                Log.w(TAG, "bulkTransfer OUT returned $transferredOut (-1). Attempting clearHalt and retry...")
                clearEndpointHalt(connection, outEp)
                transferredOut = connection.bulkTransfer(outEp, packet, 0, packet.size, TIMEOUT_MS)
            }

            if (transferredOut != packet.size) {
                Result.failure(IOException("Failed to send PC_TO_RDR_XfrBlock bulk transfer (transferred: $transferredOut, expected: ${packet.size})"))
            } else {
                val responseBuffer = ByteArray(512)
                Log.d(TAG, "Reading APDU response from IN endpoint 0x${inEp.address.toString(16)}...")
                var transferredIn = connection.bulkTransfer(inEp, responseBuffer, 0, responseBuffer.size, TIMEOUT_MS)

                if (transferredIn < 0) {
                    Log.w(TAG, "bulkTransfer IN returned $transferredIn (-1). Attempting clearHalt and retry...")
                    clearEndpointHalt(connection, inEp)
                    transferredIn = connection.bulkTransfer(inEp, responseBuffer, 0, responseBuffer.size, TIMEOUT_MS)
                }

                if (transferredIn < 10) {
                    Result.failure(IOException("Invalid response from reader for APDU (transferred: $transferredIn)"))
                } else {
                    val status = responseBuffer[7]
                    val error = responseBuffer[8]
                    if (status != 0.toByte()) {
                        val cmdStatus = (status.toInt() and 0x03)
                        if (cmdStatus == 1) {
                            return Result.failure(IOException("APDU command failed with status: 0x${status.toString(16)}, error: 0x${error.toString(16)}"))
                        }
                    }

                    val respDataLen = ((responseBuffer[1].toInt() and 0xFF)) or
                            ((responseBuffer[2].toInt() and 0xFF) shl 8) or
                            ((responseBuffer[3].toInt() and 0xFF) shl 16) or
                            ((responseBuffer[4].toInt() and 0xFF) shl 24)

                    if (respDataLen > 0 && transferredIn >= 10 + respDataLen) {
                        val respData = responseBuffer.copyOfRange(10, 10 + respDataLen)
                        Result.success(respData)
                    } else {
                        Result.success(ByteArray(0))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in transmitApduCcid", e)
            Result.failure(e)
        }
    }

    /**
     * Reads card UID using GET DATA command.
     */
    suspend fun readUid(): Result<String> {
        val getUidApdu = byteArrayOf(0xFF.toByte(), 0xCA.toByte(), 0x00, 0x00, 0x00)
        val result = transmitApdu(getUidApdu)
        return result.map { resp ->
            if (resp.isNotEmpty()) {
                if (resp.size >= 2 && resp[resp.size - 2] == 0x90.toByte() && resp[resp.size - 1] == 0x00.toByte()) {
                    val uidBytes = resp.copyOfRange(0, resp.size - 2)
                    uidBytes.joinToString("") { "%02X".format(it) }
                } else {
                    resp.joinToString("") { "%02X".format(it) }
                }
            } else {
                ""
            }
        }
    }

    /**
     * Safely disconnects and cleans up USB Serial and CCID resources.
     */
    fun disconnect() {
        try {
            serialIoManager?.let {
                it.stop()
                Log.i(TAG, "Stopped Serial IO Manager")
            }
            serialPort?.let {
                it.close()
                Log.i(TAG, "Closed USB Serial port")
            }
            currentConnection?.let { conn ->
                currentInterface?.let { iface ->
                    val released = conn.releaseInterface(iface)
                    Log.i(TAG, "Released interface ${iface.id}: $released")
                }
                conn.close()
                Log.i(TAG, "Closed USB connection")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        } finally {
            serialIoManager = null
            serialPort = null
            currentConnection = null
            currentInterface = null
            endpointIn = null
            endpointOut = null
            isSerialMode = false
            synchronized(receivedBuffer) {
                receivedBuffer.reset()
            }
        }
    }
}
