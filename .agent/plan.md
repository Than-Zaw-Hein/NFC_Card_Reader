# Project Plan

An Android application using Native Kotlin and Jetpack Compose that connects to an external USB NFC Reader (specifically CCID-compatible USB readers like the STYL Solutions / Toppan Forms TC63CUT021) via USB Host / OTG mode. Includes USB device discovery, permission requests, connection management, CCID APDU command communication (IccPowerOn / ATR / Card UID), and a clean MVVM UI with Material 3.

## Project Brief

# Project Brief: NFX Card Reader (Android MVP)

## Features

1. **USB Device Discovery & Permission Management**
   - Automatically detects connected CCID-compatible USB NFC readers (e.g., STYL Solutions / Toppan Forms TC63CUT021) via Android USB Host / OTG mode.
   - Handles USB device enumeration, connection intent filters, and runtime user permission requests.

2. **CCID Connection & Power Management**
   - Establishes USB interface configuration, claim interfaces, and bulk transfer endpoints for CCID protocol communication.
   - Executes `IccPowerOn` commands to initialize card communication and captures the Answer To Reset (ATR) response.

3. **Card UID & APDU Command Interface**
   - Automatically detects contactless cards and retrieves the card Unique Identifier (UID).
   - Provides a developer interface to send raw APDU commands and inspect response APDUs (sw1/sw2 status words and data payload).

4. **Responsive Material 3 Dashboard**
   - Implements a clean MVVM UI with Material 3 styling presenting real-time connection status, reader details, card logs, and diagnostic output.
   - Leverages **Jetpack Navigation 3** for state-driven screen flow and **Compose Material Adaptive** for responsive layouts across phones, foldables, and tablets.

---

## High-Level Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel) with Kotlin Coroutines & Flow for asynchronous USB bulk transfers and event handling.
- **USB Communication**: Android USB Host API (`android.hardware.usb`) implementing standard CCID protocol control and bulk transfer messaging.
- **Navigation**: Jetpack Navigation 3 (State-driven navigation).
- **Adaptive Layouts**: Compose Material Adaptive library (`androidx.compose.material3.adaptive`) for multi-pane and responsive form factors.

## Implementation Steps
**Total Duration:** 16m 22s

### Task_1_USB_Discovery_And_CCID_Core: Implement USB device discovery, permission management, and CCID connection layer for NFC/smart card readers.
- **Status:** COMPLETED
- **Updates:** Implemented USB device discovery, device_filter.xml, AndroidManifest.xml USB integration, and UsbNfcManager with CCID bulk transfers, IccPowerOn, ATR extraction, and APDU support. Verified build success.
- **Acceptance Criteria:**
  - USB permission handling works
  - Bulk IN/OUT endpoints configured
  - IccPowerOn and ATR successfully retrieved
  - project builds successfully
- **Duration:** 8m 10s

### Task_2_Card_UID_And_APDU_Interface: Implement Card UID retrieval and APDU command interface with repository and ViewModel architecture.
- **Status:** COMPLETED
- **Updates:** Implemented MainViewModel with StateFlows for connection status, device info, card data, and diagnostic logs. Integrated UsbNfcManager with coroutines and background polling. Verified successful build.
- **Acceptance Criteria:**
  - APDU command builder and parser implemented
  - Card UID retrieval functioning correctly
  - MVVM state management connected
  - project builds successfully
- **Duration:** 1m 39s

### Task_3_Material3_Dashboard_UI: Build the responsive Material 3 Dashboard in Jetpack Compose showing reader connection status, ATR, Card UID, and APDU transaction logs.
- **Status:** COMPLETED
- **Updates:** Implemented MainScreen with Material 3, connection status card, device details, card UID/ATR display, APDU command input, and scrollable diagnostic logs. Updated MainActivity to host UI and register USB broadcast receivers. Verified build success.
- **Acceptance Criteria:**
  - Material 3 dashboard UI implemented with Compose
  - Real-time reader status and card data displayed
  - Adaptive layout supporting various screen sizes
  - project builds successfully
- **Duration:** 1m 53s

### Task_4_Run_And_Verify: Perform final verification of the NFX Card Reader application, ensuring stability, zero crashes, and complete feature integration.
- **Status:** COMPLETED
- **Updates:** Successfully verified project build, unit tests, code structure, USB CCID integration, and Material 3 responsive UI. All criteria met without crashes or errors.
- **Acceptance Criteria:**
  - build pass
  - make sure all existing tests pass
  - app does not crash
  - instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements, and report critical UI issues
- **Duration:** 4m 40s

