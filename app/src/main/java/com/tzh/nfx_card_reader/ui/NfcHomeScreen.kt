package com.tzh.nfx_card_reader.ui

import android.hardware.usb.UsbDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tzh.nfx_card_reader.ui.theme.NxpCyan
import com.tzh.nfx_card_reader.ui.theme.NxpCyanBright
import com.tzh.nfx_card_reader.ui.theme.NxpDarkSlate
import com.tzh.nfx_card_reader.ui.theme.NxpMutedGray
import com.tzh.nfx_card_reader.ui.theme.NxpWhite
import com.tzh.nfx_card_reader.viewmodel.ConnectionStatus
import com.tzh.nfx_card_reader.viewmodel.DeviceInfo

@Composable
fun NfcHomeScreen(
    connectionStatus: ConnectionStatus,
    deviceInfo: DeviceInfo,
    discoveredDevice: UsbDevice?,
    onNavigateHistory: () -> Unit,
    onNavigateSettings: () -> Unit,
    onScanAndLaunch: () -> Unit,
    onScanAndShow: () -> Unit,
    onConnectDevice: (UsbDevice) -> Unit,
    onRequestPermission: (UsbDevice) -> Unit,
    onInfoClick: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                color = Color.Black,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // History (Cyan)
                    Text(
                        text = "History",
                        color = NxpCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateHistory() }
                    )

                    // NFX Card Reader Title (White)
                    Text(
                        text = "NFX Card Reader",
                        color = NxpWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Settings (Cyan)
                    Text(
                        text = "Settings",
                        color = NxpCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateSettings() }
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Shopping cart / store action */ }) {
                        Icon(
                            imageVector = Icons.Rounded.ShoppingCart,
                            contentDescription = "Store",
                            tint = NxpMutedGray
                        )
                    }

                    // NFX Card Reader Branding footer
                    Text(
                        text = "NFX CARD READER",
                        color = NxpMutedGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    IconButton(onClick = onInfoClick) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Help / Info",
                            tint = NxpCyan
                        )
                    }
                }
            }
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Connection Status Badge Card
            ConnectionBadgeCard(
                connectionStatus = connectionStatus,
                deviceInfo = deviceInfo,
                discoveredDevice = discoveredDevice,
                onConnectDevice = onConnectDevice,
                onRequestPermission = onRequestPermission
            )

            // Center Custom NFC Phone-to-Card Vector Graphic Illustration
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(NxpCyan.copy(alpha = 0.15f), Color.Transparent)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Phone Icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(NxpDarkSlate, RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhoneAndroid,
                                contentDescription = "Phone",
                                tint = NxpCyan,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // NFC Radio Wave lines
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.size(4.dp).background(NxpCyanBright, CircleShape))
                            Box(modifier = Modifier.size(8.dp).background(NxpCyan.copy(alpha = 0.7f), CircleShape))
                            Box(modifier = Modifier.size(12.dp).background(NxpCyan.copy(alpha = 0.4f), CircleShape))
                        }

                        // Card / Tag Icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(NxpDarkSlate, RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CreditCard,
                                contentDescription = "NFC Tag",
                                tint = NxpCyanBright,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Ready to Scan",
                        color = NxpWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Place NFC tag near USB reader or phone antenna",
                        color = NxpMutedGray,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // "Scan & Launch" Cyan Filled Rounded Button
                Button(
                    onClick = onScanAndLaunch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NxpCyan,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Nfc,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Scan & Launch",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // "Scan & Show" Cyan Text Button
                TextButton(
                    onClick = onScanAndShow,
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = "Scan & Show",
                        color = NxpCyanBright,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionBadgeCard(
    connectionStatus: ConnectionStatus,
    deviceInfo: DeviceInfo,
    discoveredDevice: UsbDevice?,
    onConnectDevice: (UsbDevice) -> Unit,
    onRequestPermission: (UsbDevice) -> Unit
) {
    val isConnected = connectionStatus == ConnectionStatus.Connected
    val badgeTitle = if (isConnected) {
        "Connected: ${deviceInfo.deviceName}"
    } else if (discoveredDevice != null) {
        "Device Found: ${discoveredDevice.deviceName}"
    } else {
        "USB Reader Disconnected"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NxpDarkSlate,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C2E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (isConnected) NxpCyanBright else Color(0xFFFF3B30),
                            shape = CircleShape
                        )
                )
                Text(
                    text = badgeTitle,
                    color = NxpWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            if (!isConnected && discoveredDevice != null) {
                TextButton(
                    onClick = {
                        if (discoveredDevice != null) {
                            onConnectDevice(discoveredDevice)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(text = "Connect", color = NxpCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
