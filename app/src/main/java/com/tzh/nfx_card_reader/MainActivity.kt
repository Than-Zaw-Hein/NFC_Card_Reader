package com.tzh.nfx_card_reader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.tzh.nfx_card_reader.ui.MainScreen
import com.tzh.nfx_card_reader.ui.theme.NFX_Card_ReaderTheme
import com.tzh.nfx_card_reader.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    viewModel.refreshDevices()
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    if (device != null) {
                        viewModel.requestPermission(device)
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    viewModel.disconnect()
                    viewModel.refreshDevices()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleUsbIntent(intent)

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(usbReceiver, filter)
        }

        setContent {
            NFX_Card_ReaderTheme {
                val connectionStatus by viewModel.connectionStatus.collectAsState()
                val deviceInfo by viewModel.deviceInfo.collectAsState()
                val cardData by viewModel.cardData.collectAsState()
                val currentTagInfo by viewModel.currentTagInfo.collectAsState()
                val scanHistory by viewModel.scanHistory.collectAsState()
                val logs by viewModel.logs.collectAsState()
                val discoveredDevice by viewModel.discoveredDevice.collectAsState()

                MainScreen(
                    connectionStatus = connectionStatus,
                    deviceInfo = deviceInfo,
                    cardData = cardData,
                    currentTagInfo = currentTagInfo,
                    scanHistory = scanHistory,
                    logs = logs,
                    discoveredDevice = discoveredDevice,
                    onRefresh = { viewModel.refreshDevices() },
                    onRequestPermission = { device -> viewModel.requestPermission(device) },
                    onConnect = { device -> viewModel.connectDevice(device) },
                    onDisconnect = { viewModel.disconnect() },
                    onScanCard = { onComplete -> viewModel.scanCard(onComplete) },
                    onSelectHistoryTag = { tagInfo -> viewModel.selectHistoryTag(tagInfo) },
                    onClearHistory = { viewModel.clearHistory() },
                    onSendApdu = { cmd -> viewModel.sendApduCommand(cmd) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUsbIntent(intent)
    }

    private fun handleUsbIntent(intent: Intent?) {
        if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            viewModel.refreshDevices()
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
            if (device != null) {
                viewModel.requestPermission(device)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(usbReceiver)
        } catch (_: Exception) {}
    }
}
