package com.tzh.nfx_card_reader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.tzh.nfx_card_reader.util.ParsedTagInfo

@Composable
fun NfcTagDetailsScreen(
    tagInfo: ParsedTagInfo?,
    onBack: () -> Unit
) {
    val tag = tagInfo ?: ParsedTagInfo()

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
                        text = "Tag Details",
                        color = NxpWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(48.dp)) // balance layout
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NFX CARD READER",
                        color = NxpMutedGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
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
            item {
                Text(
                    text = "TAG DETAILS",
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
                        DetailRow(label = "UID", value = tag.uidHex, isMonospace = true)
                        HorizontalDivider(color = NxpDivider)
                        DetailRow(label = "Family", value = tag.family)
                        HorizontalDivider(color = NxpDivider)
                        DetailRow(label = "IC Manufacturer", value = tag.icManufacturer)
                        HorizontalDivider(color = NxpDivider)
                        DetailRow(label = "IC Type", value = tag.icType)
                        HorizontalDivider(color = NxpDivider)
                        DetailRow(label = "IC Name", value = tag.icName)
                        HorizontalDivider(color = NxpDivider)
                        DetailRow(label = "Raw ATR / Hex", value = tag.rawAtrHex, isMonospace = true)
                        HorizontalDivider(color = NxpDivider)
                        DetailRow(label = "Reader Info", value = tag.readerInfo, isMonospace = true)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isMonospace: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = NxpMutedGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = NxpWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}
