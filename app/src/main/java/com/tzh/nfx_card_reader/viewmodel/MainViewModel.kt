package com.tzh.nfx_card_reader.viewmodel

import android.app.Application
import android.hardware.usb.UsbDevice
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tzh.nfx_card_reader.usb.UsbNfcManager
import com.tzh.nfx_card_reader.util.ParsedTagInfo
import com.tzh.nfx_card_reader.util.TagParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ConnectionStatus {
    Connected,
    Disconnected,
    AwaitingPermission,
    Error
}

data class DeviceInfo(
    val vendorId: Int = 0x0B95,
    val productId: Int = 0x1790,
    val deviceName: String = "STYL TC63CUT021",
    val serialNumber: String = "SN-TC63CUT021",
    val connectionType: String = "USB Smart Card / CCID",
)

data class CardData(
    val atrHex: String = "",
    val cardUidHex: String = "",
    val lastApduResponse: String = "",
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "MainViewModel"
    private val usbNfcManager = UsbNfcManager(application)

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _deviceInfo = MutableStateFlow(DeviceInfo())
    val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo.asStateFlow()

    private val _cardData = MutableStateFlow(CardData())
    val cardData: StateFlow<CardData> = _cardData.asStateFlow()

    private val _currentTagInfo = MutableStateFlow<ParsedTagInfo?>(null)
    val currentTagInfo: StateFlow<ParsedTagInfo?> = _currentTagInfo.asStateFlow()

    private val _scanHistory = MutableStateFlow<List<ParsedTagInfo>>(emptyList())
    val scanHistory: StateFlow<List<ParsedTagInfo>> = _scanHistory.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _discoveredDevice = MutableStateFlow<UsbDevice?>(null)
    val discoveredDevice: StateFlow<UsbDevice?> = _discoveredDevice.asStateFlow()

    private var pollingJob: Job? = null

    init {
        // Collect real-time received serial data bytes from UsbNfcManager and log them
        viewModelScope.launch(Dispatchers.IO) {
            usbNfcManager.serialDataFlow.collect { bytes ->
                if (bytes.isNotEmpty()) {
                    val hexData = bytes.joinToString("") { "%02X".format(it) }
                    addLog("Received serial data (${bytes.size} bytes): $hexData")
                }
            }
        }
        refreshDevices()
    }

    private fun addLog(message: String) {
        val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val timestamp = timeFormat.format(Date())
        val logEntry = "[$timestamp] $message"
        Log.d(tag, message)
        _logs.update { current -> (current + logEntry).takeLast(200) }
    }

    fun refreshDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val device = usbNfcManager.findConnectedCcidDevice()
                _discoveredDevice.value = device
                if (device != null) {
                    val vendorHex = "0x${device.vendorId.toString(16).padStart(4, '0').uppercase()}"
                    val productHex = "0x${device.productId.toString(16).padStart(4, '0').uppercase()}"
                    if (usbNfcManager.isAsixDevice(device) || usbNfcManager.isSerialDevice(device)) {
                        addLog("Found USB Serial / CDC-ACM device: ${device.deviceName} (Vendor: $vendorHex, Product: $productHex)")
                    } else {
                        addLog("Found CCID device: ${device.deviceName} (Vendor: $vendorHex, Product: $productHex)")
                    }
                } else {
                    addLog("No USB devices found.")
                }
            } catch (e: Exception) {
                addLog("Error finding USB device: ${e.message}")
            }
        }
    }

    fun requestPermission(device: UsbDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _connectionStatus.value = ConnectionStatus.AwaitingPermission
                addLog("Requesting USB permission for device ${device.deviceName}...")
                val granted = usbNfcManager.requestPermission(device)
                if (granted) {
                    addLog("USB permission granted for ${device.deviceName}")
                    connectDevice(device)
                } else {
                    _connectionStatus.value = ConnectionStatus.Error
                    addLog("USB permission denied for ${device.deviceName}")
                }
            } catch (e: Exception) {
                _connectionStatus.value = ConnectionStatus.Error
                addLog("Error requesting USB permission: ${e.message}")
            }
        }
    }

    fun connectDevice(device: UsbDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                addLog("Connecting to USB device ${device.deviceName}...")
                val result = usbNfcManager.connect(device)
                if (result.isSuccess) {
                    _connectionStatus.value = ConnectionStatus.Connected
                    val serial = device.serialNumber ?: "SN-TC63CUT021"
                    val isSerial = usbNfcManager.isSerialMode()
                    val connType = if (isSerial) "USB Serial / CDC-ACM (115200 8N1)" else "CCID Bulk Transfer"

                    _deviceInfo.value = DeviceInfo(
                        vendorId = device.vendorId,
                        productId = device.productId,
                        deviceName = device.deviceName.ifBlank { "STYL TC63CUT021" },
                        serialNumber = serial,
                        connectionType = connType,
                    )

                    if (isSerial) {
                        addLog("Connected via USB Serial / CDC-ACM (115200 8N1)")
                    } else {
                        addLog("Connected via CCID Bulk Transfer mode")
                    }
                    startCardPollingLoop()
                } else {
                    _connectionStatus.value = ConnectionStatus.Error
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown connection error"
                    addLog("Failed to connect: $errorMsg")
                }
            } catch (e: Exception) {
                _connectionStatus.value = ConnectionStatus.Error
                addLog("Exception during connect: ${e.message}")
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                pollingJob?.cancel()
                pollingJob = null
                usbNfcManager.disconnect()
                _connectionStatus.value = ConnectionStatus.Disconnected
                _deviceInfo.value = DeviceInfo()
                _cardData.value = CardData()
                addLog("Disconnected from USB device.")
            } catch (e: Exception) {
                addLog("Error during disconnect: ${e.message}")
            }
        }
    }

    fun scanCard(onComplete: (ParsedTagInfo) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val readerMeta = "${_deviceInfo.value.deviceName} (0x${_deviceInfo.value.vendorId.toString(16).padStart(4, '0').uppercase()}:0x${_deviceInfo.value.productId.toString(16).padStart(4, '0').uppercase()})"
            try {
                if (_connectionStatus.value == ConnectionStatus.Connected) {
                    val modeStr = if (usbNfcManager.isSerialMode()) "USB Serial" else "CCID bulk transfer"
                    addLog("Powering on card ($modeStr)...")
                    val result = usbNfcManager.powerOnCard()
                    if (result.isSuccess) {
                        val atrBytes = result.getOrNull() ?: ByteArray(0)
                        val atrHex = byteArrayToHex(atrBytes)
                        _cardData.value = _cardData.value.copy(atrHex = atrHex)

                        val uidResult = usbNfcManager.readUid()
                        val uidHex = if (uidResult.isSuccess) uidResult.getOrNull() ?: "" else ""
                        _cardData.value = _cardData.value.copy(cardUidHex = uidHex)

                        if (uidHex.isNotBlank() && uidHex != "N/A" && uidHex != "NO CARD DETECTED") {
                            val parsed = TagParser.parse(uidHex, atrHex, readerMeta)
                            updateParsedTag(parsed)
                            addLog("Scanned NFC Tag: ${parsed.icName} (UID: ${parsed.uidHex})")
                            onComplete(parsed)
                            return@launch
                        } else {
                            addLog("Power-on successful, but no card UID read in field.")
                        }
                    } else {
                        addLog("Failed to power on card (No card present or device busy).")
                    }
                } else {
                    addLog("USB reader not connected. Please connect reader to scan real NFC tags.")
                }

                val emptyParsed = TagParser.parse("", "", readerMeta)
                onComplete(emptyParsed)
            } catch (e: Exception) {
                addLog("Exception during card scan: ${e.message}")
                val emptyParsed = TagParser.parse("", "", readerMeta)
                onComplete(emptyParsed)
            }
        }
    }

    private fun updateParsedTag(tagInfo: ParsedTagInfo) {
        _currentTagInfo.value = tagInfo
        _scanHistory.update { current ->
            (listOf(tagInfo) + current.filter { it.uidHex != tagInfo.uidHex }).take(50)
        }
    }

    fun powerOnCard() {
        scanCard {}
    }

    fun selectHistoryTag(tagInfo: ParsedTagInfo) {
        _currentTagInfo.value = tagInfo
    }

    fun clearHistory() {
        _scanHistory.value = emptyList()
    }

    fun sendApduCommand(hexCommand: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val modeStr = if (usbNfcManager.isSerialMode()) "USB Serial" else "CCID bulk transfer"
                addLog("Sending APDU command ($modeStr): $hexCommand")
                val apduBytes = hexToByteArray(hexCommand)
                val result = usbNfcManager.transmitApdu(apduBytes)
                if (result.isSuccess) {
                    val respBytes = result.getOrNull() ?: ByteArray(0)
                    val respHex = byteArrayToHex(respBytes)
                    _cardData.value = _cardData.value.copy(lastApduResponse = respHex)
                    addLog("APDU Response ($modeStr): $respHex")
                } else {
                    val err = result.exceptionOrNull()?.message ?: "APDU command failed"
                    addLog("APDU transmission failed ($modeStr): $err")
                }
            } catch (e: Exception) {
                addLog("Exception in sendApduCommand: ${e.message}")
            }
        }
    }

    private fun startCardPollingLoop() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            addLog("Background card polling loop started.")
            while (isActive) {
                try {
                    // Poll card status only in CCID mode
                    if (!usbNfcManager.isSerialMode()) {
                        val powerResult = usbNfcManager.powerOnCard()
                        if (powerResult.isSuccess) {
                            val atrBytes = powerResult.getOrNull() ?: ByteArray(0)
                            val atrHex = byteArrayToHex(atrBytes)
                            if ((atrHex.isNotEmpty()) && (atrHex != _cardData.value.atrHex)) {
                                _cardData.value = _cardData.value.copy(atrHex = atrHex)
                                val uidResult = usbNfcManager.readUid()
                                val uidHex = if (uidResult.isSuccess) uidResult.getOrNull() ?: "" else ""
                                if (uidHex.isNotBlank() && uidHex != "N/A" && uidHex != "NO CARD DETECTED") {
                                    addLog("Poll: Card detected! ATR: $atrHex")
                                    val readerMeta = "${_deviceInfo.value.deviceName} (0x${_deviceInfo.value.vendorId.toString(16).padStart(4, '0').uppercase()}:0x${_deviceInfo.value.productId.toString(16).padStart(4, '0').uppercase()})"
                                    val parsed = TagParser.parse(uidHex, atrHex, readerMeta)
                                    updateParsedTag(parsed)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Suppress frequent polling exceptions when no card is present
                }
                delay(2500)
            }
        }
    }

    private fun byteArrayToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }

    private fun hexToByteArray(hex: String): ByteArray {
        val cleanHex = hex.replace("\\s".toRegex(), "")
        val len = cleanHex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(cleanHex[i], 16) shl 4) +
                    Character.digit(cleanHex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    override fun onCleared() {
        disconnect()
    }
}
