package com.tzh.nfx_card_reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun NfcTagSummaryScreen(
    tagInfo: ParsedTagInfo?,
    onClose: () -> Unit,
    onNavigateDetails: () -> Unit
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { onClose() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = "Close",
                            tint = NxpCyan
                        )
                        Text(
                            text = "Close",
                            color = NxpCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Header: NDEF Records Summary Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NxpDarkSlate,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NxpDivider)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Nfc,
                        contentDescription = null,
                        tint = NxpCyan,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = tag.ndefSummary,
                        color = NxpWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "UID: ${tag.uidHex}",
                        color = NxpMutedGray,
                        fontSize = 13.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            // Section Header: TAG DETAILS
            Text(
                text = "TAG DETAILS",
                color = NxpMutedGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            // Clickable row displaying tag summary leading to full details screen
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateDetails() },
                color = NxpDarkSlate,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NxpDivider)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = tag.icName,
                            color = NxpCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${tag.family} • ${tag.icManufacturer}",
                            color = NxpMutedGray,
                            fontSize = 13.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "View Details",
                        tint = NxpCyan
                    )
                }
            }
        }
    }
}
