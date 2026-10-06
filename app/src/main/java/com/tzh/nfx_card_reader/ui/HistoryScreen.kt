package com.tzh.nfx_card_reader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
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
fun HistoryScreen(
    scanHistory: List<ParsedTagInfo>,
    onSelectTag: (ParsedTagInfo) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit
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
                        text = "Scan History",
                        color = NxpWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (scanHistory.isNotEmpty()) {
                        IconButton(onClick = onClearHistory) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Clear History",
                                tint = NxpCyan
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }
            }
        },
        containerColor = Color.Black
    ) { innerPadding ->
        if (scanHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = NxpMutedGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No scan history yet",
                        color = NxpMutedGray,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(scanHistory) { tag ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTag(tag) },
                        color = NxpDarkSlate,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NxpDivider)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tag.icName,
                                    color = NxpCyan,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tag.formattedDate,
                                    color = NxpMutedGray,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "UID: ${tag.uidHex}",
                                color = NxpWhite,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${tag.family} • ${tag.icManufacturer}",
                                color = NxpMutedGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
