# NFX Card Reader

<p align="center">
  <img src="app/src/main/res/mipmap-xxhdpi/ic_launcher.webp" alt="NFX Card Reader Logo" width="128" height="128">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin-blue.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Design-Material%203-purple.svg" alt="Material 3">
  <img src="https://img.shields.io/badge/USB-OTG%20%2F%20CCID-red.svg" alt="USB OTG / CCID">
</p>

---

## 📖 Project Overview

**NFX Card Reader** is a high-performance Android application built with native Kotlin and Jetpack Compose that connects to external USB NFC and Smart Card readers (such as the **STYL Solutions / Toppan Forms TC63CUT021** and other CCID/CDC-ACM compatible hardware) via USB Host / OTG mode. 

Designed with a professional **TagInfo-inspired dark mode UI**, NFX Card Reader enables developers, security researchers, and engineers to discover USB smart card hardware, inspect NFC/Smart Card tags in real-time, analyze Answer to Reset (ATR) bytes, extract UIDs, and transmit raw APDU commands directly from an Android mobile device or tablet.

---

## ✨ Key Features

1. **USB OTG Device Discovery & Permission Management**
   - Automatic scanning of connected USB peripherals via Android USB Host API (`android.hardware.usb`).
   - Seamless USB permission request handling and broadcast receiver lifecycle management.
2. **Dual-Mode USB Communication**
   - **CCID Bulk Transfers**: Standard communication with PC/SC compliant USB smart card readers.
   - **USB Serial / CDC-ACM Support**: Robust handling of serial-based readers (such as ASIX chipset controllers with VID `0x0B95` / PID `0x1790`).
3. **Live NFC & Smart Card Analysis**
   - Real-time card presence detection, UID retrieval, and historical scan logging.
   - Deep inspection of Answer to Reset (ATR) bytes, historical bytes, ISO-7816 / ISO-14443 tag protocol decoding, and storage capability estimation.
4. **Interactive APDU Terminal & Settings**
   - Custom APDU command transmission interface for reading card memory, executing authentication challenges, or sending custom command-response pairs.
   - Configurable reader settings, baud rates, timeout intervals, and raw communication logs.
5. **TagInfo-Inspired Dark Mode UI**
   - Sleek, high-contrast dark theme optimized for readability in laboratory and field environments.
   - Comprehensive navigation across Home, Tag Summary, Tag Details, History, and Settings / APDU Terminal screens.

---

## 🏗️ Architecture & Tech Stack

- **Architecture Pattern**: Model-View-ViewModel (MVVM) leveraging Kotlin Coroutines and `StateFlow` for reactive, lifecycle-aware state management.
- **USB Hardware Layer**: Android USB Host API (`android.hardware.usb`) paired with `usb-serial-for-android` for serial/CDC-ACM peripheral bridging.
- **UI Framework**: Jetpack Compose adhering to Material 3 design guidelines with edge-to-edge support and dynamic color schemes.
- **Data Parsing & Utilities**: Custom `TagParser` engine for protocol identification, ATR parsing, and hexadecimal formatting.

---

## 📂 Project Structure

```text
com.tzh.nfx_card_reader/
├── MainActivity.kt               # Single-activity host with Edge-to-Edge & USB intent handling
├── ui/
│   ├── MainScreen.kt             # Root scaffold & bottom navigation controller
│   ├── NfcHomeScreen.kt          # Device connection status & live scan dashboard
│   ├── NfcTagSummaryScreen.kt    # High-level tag overview & UID/ATR summary
│   ├── NfcTagDetailsScreen.kt    # Deep protocol breakdown & raw payload inspection
│   ├── HistoryScreen.kt          # Log of previously scanned cards & timestamps
│   ├── SettingsScreen.kt         # APDU command terminal, reader configuration & logs
│   └── theme/                    # Material 3 Color, Type, and Theme definitions
├── usb/
│   └── UsbNfcManager.kt          # Core USB Host connection, CCID/CDC protocol & packet engine
├── util/
│   └── TagParser.kt              # ATR decoder, UID formatter & protocol analysis helpers
└── viewmodel/
    └── MainViewModel.kt          # Reactive ViewModel managing USB state, scanning & APDU flows
```

---

## 🛠️ Hardware Compatibility

NFX Card Reader is tested and designed for external USB smart card readers, including:
- **STYL Solutions / Toppan Forms TC63CUT021** (and equivalent CCID smart card terminals)
- USB-to-Serial adapters and CDC-ACM compliant RFID/NFC readers (e.g., ASIX chipsets with VID `0x0B95` / PID `0x1790`)
- Standard PC/SC USB Smart Card Readers supporting bulk IN/OUT endpoints.

---

## 🚀 How to Build and Run

### Prerequisites
- **Android Studio**: Iguana / Jellyfish or newer (Koala / Ladybug recommended).
- **Target Device**: Android 8.0 (API Level 26) or higher.
- **Hardware**: An Android device supporting **USB Host / OTG mode**, an OTG cable/adapter, and a compatible USB NFC/Smart Card reader.

### Build Commands
Clone the repository and build the debug APK via Gradle:

```bash
# Clone the repository
git clone https://github.com/your-username/nfx-card-reader.git
cd NFX_Card_Reader

# Build debug APK
./gradlew :app:assembleDebug
```

To run unit tests:
```bash
./gradlew :app:testDebugUnitTest
```

### Hardware Connection Guide
1. Connect your USB OTG adapter to your Android phone or tablet.
2. Plug the USB Smart Card Reader (e.g., TC63CUT021) into the OTG adapter.
3. Launch **NFX Card Reader** on your device.
4. Grant USB permission when prompted by the Android system dialog.
5. Tap **Connect** in the app to establish communication with the reader, then place an NFC card or smart card on the reader surface to view live data.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
