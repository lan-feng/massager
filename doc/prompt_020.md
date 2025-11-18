You will enhance an existing Android Kotlin + Jetpack Compose + MVVM app.
The current app already has a single-device control page (DeviceControlScreen.kt),
which includes a UI with a device image and an empty dashed box with a “+” button.

We must now implement a “Device Combination” feature starting from this “+” entry.

====================================================
# 1. New Feature Summary — Device Combination (NOT group control)
====================================================
- User can add multiple same-type devices into a “combination”.
- The entry point MUST be the “+” button on the Device Control Page.
- When the user taps the "+" box:
  → Start a new combination pairing flow.
- The combination (group) contains multiple devices.
- Only ONE device is controlled at a time.
- When the user taps a device icon, the control panel switches to that device.
- This is NOT group-control. No broadcasting commands.

====================================================
# 2. Entry Point — The "+" Button (Important)
====================================================
In DeviceControlScreen.kt:
There is a "+" dashed box beside the main device image.
This triggers the combination pairing workflow.

User Flow:
DeviceControlScreen.kt
→ User taps “+”
→ Open “Combination Pairing Step 1” dialog
→ Step 2 scanning dialog
→ Device found → add to combination
→ After adding → navigate to DeviceCombinationControlScreen(groupId)

====================================================
# 3. Combination Pairing Workflow (from the images provided)
====================================================

### STEP 1: LED confirmation dialog
- Title: “Please confirm that the host light is flashing blue.”
- Device illustration image
- “Next” button

### STEP 2: BLE scanning dialog
- Title: “Search device”
- Rotating Bluetooth animation
- Text: “Searching device”
- “Cancel” button

When a device is found → show a device card and allow user to add it to the combination.

### Adding a device:
- Device name
- MAC
- Battery
- "Add" button

====================================================
# 4. Combination Control Page (DeviceCombinationControlScreen)
====================================================
The layout structure:

(1) Device Switcher Row (Top)
- Horizontal list of device icons.
- Each shows:
    - Device image
    - Battery %
    - Connection state
- Tapping a device:
  → selectDevice(deviceId)
  → refresh control panel with this device’s state

(2) Add-more-device "+" button in this row
- Opens the same pairing flow
- Added devices appear in the row

(3) Single-device control panel (same as existing DeviceControlScreen):
- Mode tabs 
- Mode 
- Intensity +/-
- Timer
- Start / Stop
- Device mute
- Battery display

The control panel always acts on the currently selected device.

====================================================
# 5. Data Model Requirements
====================================================
Create:

data class DeviceGroup(
val groupId: String,
val devices: List<MassagerDevice>,
val selectedDeviceId: String?
)

data class MassagerDevice(
val deviceId: String,
val name: String,
val mac: String,
val battery: Int,
val isConnected: Boolean,
val mode: Int,
val intensity: Int,
val timer: Int
)

====================================================
# 6. ViewModel Requirements — DeviceGroupControlViewModel
====================================================

### Must support:

# Pairing Flow
fun startPairing()
fun confirmLedFlashing()
fun startScan()
fun addDeviceToGroup(device: MassagerDevice)

# Group Device Management
fun selectDevice(deviceId: String)
fun removeDevice(deviceId: String)
fun renameDevice(deviceId: String)

# Device Control (Single Device Only)
fun updateMode(deviceId: String, mode: Int)
fun updateIntensity(deviceId: String, level: Int)
fun updateTimer(deviceId: String, minutes: Int)
fun start(deviceId: String)
fun stop(deviceId: String)

# BLE State Sync
fun subscribeDevice(deviceId: String)
fun updateBattery(deviceId: String, level: Int)
fun updateConnection(deviceId: String, connected: Boolean)

Important:
NO group broadcasts.
Every function must work only on one device at a time.

====================================================
# 7. BLE Layer Requirements
====================================================
Use the existing BLE implementation for each device.

Provide:
fun sendMode(deviceId, mode)
fun sendIntensity(deviceId, level)
fun sendStart(deviceId)
fun sendStop(deviceId)

NO loops, no broadcast commands.

====================================================
# 8. Navigation
====================================================
Add a new route:

Screen.DeviceCombinationControl(groupId: String)

User path:
DeviceControlScreen → "+" → pairing → DeviceCombinationControlScreen

====================================================
# 9. Code Output Requirements
====================================================
Generate ONLY the following:

1. Data models
2. DeviceGroupControlViewModel (full implementation)
3. Pairing dialogs (Step1 + Step2)
4. DeviceCombinationControlScreen (Compose)
5. Device switcher row (with "+" button)
6. Modified DeviceControlScreen "+" click handler
7. Updated navigation route

Do NOT rewrite existing DeviceControlScreen logic.
Only add combination logic and new screens.

====================================================
# End of specification
====================================================
