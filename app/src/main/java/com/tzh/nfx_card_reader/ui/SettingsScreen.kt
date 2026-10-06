package com.tzh.nfx_card_reader.ui

import android.hardware.usb.UsbDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tzh.nfx_card_reader.ui.theme.NxpCyan
import com.tzh.nfx_card_reader.ui.theme.NxpDarkSlate
import com.tzh.nfx_card_reader.ui.theme.NxpDivider
import com.tzh.nfx_card_reader.ui.theme.NxpMutedGray
import com.tzh.nfx_card_reader.ui.theme.NxpWhite
import com.tzh.nfx_card_reader.viewmodel.CardData
import com.tzh.nfx_card_reader.viewmodel.ConnectionStatus
import com.tzh.nfx_card_reader.viewmodel.DeviceInfo

@Composable
fun SettingsScreen(
    connectionStatus: ConnectionStatus,
    deviceInfo: DeviceInfo,
    discoveredDevice: UsbDevice?,
    cardData: CardData,
    logs: List<String>,
    onRefresh: () -> Unit,
    onRequestPermission: (UsbDevice) -> Unit,
    onConnect: (UsbDevice) -> Unit,
    onDisconnect: () -> Unit,
    onSendApdu: (String) -> Unit,
    onBack: () -> Unit
) {
    var apduInput by remember { mutableStateOf("FFCA000000") }
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.clickable { onBack() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = "Back",
                            tint = NxpCyan
                        )
                        Text(
                            text = "Back",
                            color = NxpCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Reader Settings & Logs",
                        color = NxpWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh Devices",
                            tint = NxpCyan
                        )
                    }
                }
            }
        },
        containerColor = Color.Black
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Connection Status
            item {
                Text(
                    text = "USB READER STATUS",
                    color = NxpMutedGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NxpDarkSlate,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NxpDivider)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Status",
                                color = NxpMutedGray,
                                fontSize = 14.sp
                            )
                            val statusStr = when (connectionStatus) {
                                ConnectionStatus.Connected -> "Connected"
                                ConnectionStatus.Disconnected -> "Disconnected"
                                ConnectionStatus.AwaitingPermission -> "Awaiting Permission"
                                ConnectionStatus.Error -> "Error"
                            }
                            Text(
                                text = statusStr,
                                color = if (connectionStatus == ConnectionStatus.Connected) NxpCyan else Color(0xFFFF3B30),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(color = NxpDivider)

                        if (connectionStatus == ConnectionStatus.Connected) {
                            DetailRow(label = "Device Name", value = deviceInfo.deviceName)
                            HorizontalDivider(color = NxpDivider)
                            DetailRow(label = "Vendor ID", value = "0x${deviceInfo.vendorId.toString(16).padStart(4, '0').uppercase()}", isMonospace = true)
                            HorizontalDivider(color = NxpDivider)
                            DetailRow(label = "Product ID", value = "0x${deviceInfo.productId.toString(16).padStart(4, '0').uppercase()}", isMonospace = true)
                            HorizontalDivider(color = NxpDivider)
                            DetailRow(label = "Connection Mode", value = deviceInfo.connectionType)
                            HorizontalDivider(color = NxpDivider)
                            DetailRow(label = "Serial Number", value = deviceInfo.serialNumber, isMonospace = true)

                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onDisconnect,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF3B30).copy(alpha = 0.2f),
                                    contentColor = Color(0xFFFF3B30)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Disconnect Reader")
                            }
                        } else {
                            if (discoveredDevice != null) {
                                Text(
                                    text = "Found: ${discoveredDevice.deviceName}",
                                    color = NxpWhite,
                                    fontSize = 14.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onRequestPermission(discoveredDevice) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = NxpCyan, contentColor = Color.Black)
                                    ) {
                                        Text("Grant Permission", fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onConnect(discoveredDevice) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = NxpCyan, contentColor = Color.Black)
                                    ) {
                                        Text("Connect", fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Text(
                                    text = "No USB smart card or serial device detected.",
                                    color = NxpMutedGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // APDU Tester
            item {
                Text(
                    text = "APDU COMMAND TESTER",
                    color = NxpMutedGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NxpDarkSlate,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NxpDivider)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = apduInput,
                            onValueChange = { apduInput = it },
                            label = { Text("APDU Hex Command") },
                            placeholder = { Text("FFCA000000") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NxpCyan,
                                unfocusedBorderColor = NxpDivider,
                                focusedLabelColor = NxpCyan,
                                unfocusedLabelColor = NxpMutedGray
                            )
                        )

                        Button(
                            onClick = { onSendApdu(apduInput) },
                            enabled = connectionStatus == ConnectionStatus.Connected && apduInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NxpCyan, contentColor = Color.Black)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send APDU", fontWeight = FontWeight.Bold)
                        }

                        if (cardData.lastApduResponse.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Response:",
                                color = NxpCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = Color.Black,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = cardData.lastApduResponse,
                                    color = NxpWhite,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Diagnostic Logs
            item {
                Text(
                    text = "DIAGNOSTIC LOGS",
                    color = NxpMutedGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    color = NxpDarkSlate,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NxpDivider)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        if (logs.isEmpty()) {
                            Text(
                                text = "No logs yet...",
                                color = NxpMutedGray,
                                fontSize = 13.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(logs) { log ->
                                    Text(
                                        text = log,
                                        color = NxpWhite,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
