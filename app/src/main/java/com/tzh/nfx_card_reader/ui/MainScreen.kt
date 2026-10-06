package com.tzh.nfx_card_reader.ui

import android.hardware.usb.UsbDevice
import androidx.compose.runtime.*
import com.tzh.nfx_card_reader.viewmodel.CardData
import com.tzh.nfx_card_reader.viewmodel.ConnectionStatus
import com.tzh.nfx_card_reader.viewmodel.DeviceInfo
import com.tzh.nfx_card_reader.util.ParsedTagInfo

sealed interface AppScreen {
    data object Home : AppScreen
    data object TagSummary : AppScreen
    data object TagDetails : AppScreen
    data object History : AppScreen
    data object Settings : AppScreen
}

@Composable
fun MainScreen(
    connectionStatus: ConnectionStatus,
    deviceInfo: DeviceInfo,
    cardData: CardData,
    currentTagInfo: ParsedTagInfo?,
    scanHistory: List<ParsedTagInfo>,
    logs: List<String>,
    discoveredDevice: UsbDevice?,
    onRefresh: () -> Unit,
    onRequestPermission: (UsbDevice) -> Unit,
    onConnect: (UsbDevice) -> Unit,
    onDisconnect: () -> Unit,
    onScanCard: (onComplete: (ParsedTagInfo) -> Unit) -> Unit,
    onSelectHistoryTag: (ParsedTagInfo) -> Unit,
    onClearHistory: () -> Unit,
    onSendApdu: (String) -> Unit
) {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }

    when (currentScreen) {
        AppScreen.Home -> {
            NfcHomeScreen(
                connectionStatus = connectionStatus,
                deviceInfo = deviceInfo,
                discoveredDevice = discoveredDevice,
                onNavigateHistory = { currentScreen = AppScreen.History },
                onNavigateSettings = { currentScreen = AppScreen.Settings },
                onScanAndLaunch = {
                    onScanCard { tag ->
                        if (tag.uidHex != "No card detected" && tag.uidHex != "N/A") {
                            currentScreen = AppScreen.TagSummary
                        }
                    }
                },
                onScanAndShow = {
                    onScanCard { tag ->
                        if (tag.uidHex != "No card detected" && tag.uidHex != "N/A") {
                            currentScreen = AppScreen.TagDetails
                        }
                    }
                },
                onConnectDevice = onConnect,
                onRequestPermission = onRequestPermission,
                onInfoClick = { currentScreen = AppScreen.Settings }
            )
        }
        AppScreen.TagSummary -> {
            NfcTagSummaryScreen(
                tagInfo = currentTagInfo,
                onClose = { currentScreen = AppScreen.Home },
                onNavigateDetails = { currentScreen = AppScreen.TagDetails }
            )
        }
        AppScreen.TagDetails -> {
            NfcTagDetailsScreen(
                tagInfo = currentTagInfo,
                onBack = {
                    currentScreen = AppScreen.TagSummary
                }
            )
        }
        AppScreen.History -> {
            HistoryScreen(
                scanHistory = scanHistory,
                onSelectTag = { tag ->
                    onSelectHistoryTag(tag)
                    currentScreen = AppScreen.TagSummary
                },
                onClearHistory = onClearHistory,
                onBack = { currentScreen = AppScreen.Home }
            )
        }
        AppScreen.Settings -> {
            SettingsScreen(
                connectionStatus = connectionStatus,
                deviceInfo = deviceInfo,
                discoveredDevice = discoveredDevice,
                cardData = cardData,
                logs = logs,
                onRefresh = onRefresh,
                onRequestPermission = onRequestPermission,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
                onSendApdu = onSendApdu,
                onBack = { currentScreen = AppScreen.Home }
            )
        }
    }
}
