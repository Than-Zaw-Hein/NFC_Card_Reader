package com.tzh.nfx_card_reader.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ParsedTagInfo(
    val uidHex: String = "No card detected",
    val family: String = "N/A",
    val icManufacturer: String = "N/A",
    val icType: String = "N/A",
    val icName: String = "No card detected",
    val rawAtrHex: String = "N/A",
    val readerInfo: String = "N/A",
    val ndefSummary: String = "No NDEF Records",
    val scanTimestamp: Long = System.currentTimeMillis()
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(scanTimestamp))
        }
}

object TagParser {

    /**
     * Parses a card UID hex string and raw ATR hex string into structured ParsedTagInfo.
     */
    fun parse(
        uidHexRaw: String,
        atrHexRaw: String = "",
        readerMetadata: String = "STYL TC63CUT021 (0x0B95:0x1790)"
    ): ParsedTagInfo {
        val cleanUid = uidHexRaw.replace("0x", "", ignoreCase = true)
            .replace("\\s".toRegex(), "")
            .uppercase()
        val cleanAtr = atrHexRaw.replace("0x", "", ignoreCase = true)
            .replace("\\s".toRegex(), "")
            .uppercase()

        if (cleanUid.isEmpty() || cleanUid == "NO CARD DETECTED") {
            return ParsedTagInfo(
                uidHex = "N/A",
                family = "Unknown / No Tag",
                icManufacturer = "Unknown",
                icType = "No IC Detected",
                icName = "Empty Tag",
                rawAtrHex = cleanAtr.ifEmpty { "N/A" },
                readerInfo = readerMetadata,
                ndefSummary = "No NDEF Records"
            )
        }

        val uidFormatted = "0x$cleanUid"
        val bytes = hexToBytes(cleanUid)
        val len = bytes.size
        val mfgByte = if (len > 0) bytes[0].toInt() and 0xFF else 0

        val manufacturer = when (mfgByte) {
            0x04, 0x25 -> "NXP Semiconductors"
            0x05 -> "Infineon Technologies"
            0x02 -> "STMicroelectronics"
            0x07 -> "Texas Instruments"
            0x1D -> "Microchip / Atmel"
            0x01 -> "Motorola"
            0x03 -> "Hitachi"
            0x16 -> "Sony"
            else -> "Generic (0x${"%02X".format(mfgByte)})"
        }

        var family: String
        var icType: String
        var icName: String
        val ndefSummary = "No NDEF Records"

        when {
            // NXP MIFARE Ultralight / NTAG (7-byte UID starting with 0x04)
            len == 7 && mfgByte == 0x04 -> {
                family = "MIFARE"
                icType = "Ultralight / NTAG"
                val subtypeByte = if (bytes.size > 1) bytes[1].toInt() and 0xFF else 0
                icName = when (subtypeByte % 3) {
                    0 -> "MIFARE Ultralight Hospitality"
                    1 -> "NTAG213 / NTAG215 / NTAG216"
                    else -> "MIFARE Ultralight C"
                }
            }
            // MIFARE Classic (4-byte UID starting with 0x04)
            len == 4 && mfgByte == 0x04 -> {
                family = "MIFARE"
                icType = "MIFARE Classic 1K / 4K"
                icName = "MIFARE Classic S50 / S70"
            }
            // ISO 14443-3A Tag (7-byte UID)
            len == 7 -> {
                family = "ISO 14443-3A"
                icType = "Type 2 / Type 4 Tag"
                icName = "ISO 14443-3A Contactless Tag"
            }
            // ISO 14443-3A Tag (4-byte UID)
            len == 4 -> {
                family = "ISO 14443-3A"
                icType = "Type 1 / Type 2 Tag"
                icName = "ISO 14443-3A 4-byte Tag"
            }
            // ISO 15693 Vicinity Tag (8-byte UID)
            len == 8 -> {
                family = "ISO/IEC 15693"
                icType = "ICODE SLIX"
                icName = "NXP ICODE SLIX Vicinity Tag"
            }
            else -> {
                family = "ISO 14443-A"
                icType = "Generic NFC Tag"
                icName = "Contactless Smart Card"
            }
        }

        return ParsedTagInfo(
            uidHex = uidFormatted,
            family = family,
            icManufacturer = manufacturer,
            icType = icType,
            icName = icName,
            rawAtrHex = cleanAtr.ifEmpty { "3B6F0000000000" },
            readerInfo = readerMetadata,
            ndefSummary = ndefSummary
        )
    }

    private fun hexToBytes(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            val index = i * 2
            if (index + 2 <= hex.length) {
                val item = hex.substring(index, index + 2)
                result[i] = item.toIntOrNull(16)?.toByte() ?: 0
            }
        }
        return result
    }
}
