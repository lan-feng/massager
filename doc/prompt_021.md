You will enhance the existing Android Kotlin + Jetpack Compose + MVVM codebase.

The current page layout resembles the screenshot provided (BLE_EMS control interface).
You must implement an optimized multi-device “combination control” (combo) UI and logic.

The following requirements must be implemented exactly.

====================================================
# 1. Device Info Component (Scrollable, Multi-Device Carousel)
====================================================

Replace the current static device info block with a horizontally scrollable row.
Requirements:

(1) Each device is represented by a “device card”:
    - Shows device name
    - Shows connection status
    - Shows battery level
    - Shows mute/unmute icon
    - Shows refresh/reconnect icon
    - Shows device image
    - Has rounded rectangular container
    - Has elevation and shadow similar to screenshot

(2) When multiple devices exist in combo_info:
    - All devices appear as multiple horizontal cards
    - The scroll area must support horizontal scrolling (LazyRow)
    - Card width must be controlled to ensure good readability on phone screens

(3) The “+” dashed box (add device) must appear at the end of the scroll list

(4) When only one device exists:
    - Only show the single device card + add button

====================================================
# 2. Device Card Selection Switching
====================================================

User must be able to tap a device card to select it.

When a card is selected:
- The card becomes highlighted with selected styling (border, elevation, color, etc.)
- The bottom control panel updates to control THIS selected device ONLY
- BLE connection, GATT notifications, and heartbeats must switch to the selected device

Expose:

var selectedDeviceId: String

ViewModel must provide:
fun selectDevice(deviceId: String)

Selecting a device MUST:
- Unsubscribe from previous device BLE notifications
- Subscribe to the newly selected device BLE notifications
- Refresh BLE live data for UI (battery, connect state, etc.)
- Refresh mode/intensity/timer values for that device

====================================================
# 3. Add Device via “+” (Append to combo_info, not replace)
====================================================

When user taps the “+” card:

→ Trigger pairing workflow (LED confirm → scan → device selected → bind)

After successful binding:
- Append the new device into the combo_info list
- DO NOT overwrite or remove existing devices
- Only extend combo_info.devices with the new entry
- If the new device is the same model, allow it
- Set selectedDeviceId = the newly added device

The updated combo_info must persist in ViewModel and local storage layer where needed.

ViewModel function required:
fun addDeviceToCombo(newDevice: MassagerDevice)

Implementation:
- comboInfo.devices = comboInfo.devices + newDevice

====================================================
# 4. Remove Device from Combination
====================================================

Each device card (except the main/root device) should have a “Remove from group” option.

Requirements:
- Remove device from combo_info
- Update local storage where combo_info is saved
- If the removed device == selectedDeviceId:
      → auto switch selectedDeviceId to first remaining device
- Unsubscribe BLE for removed device

ViewModel must expose:
fun removeDeviceFromCombo(deviceId: String)

====================================================
# 5. combo_info Decides What Devices Appear in UI
====================================================

Do NOT hardcode device card count.

The list of devices must be rendered strictly from:

combo_info.devices : List<MassagerDevice>

UI = LazyRow(combo_info.devices + AddCard)

Combo order must reflect combo_info order.

====================================================
# 6. BLE Switching When Selecting Device
====================================================

When user switches between device cards:
- The active BLE session must also switch.

ViewModel responsibilities:
- Stop current GATT connection or at minimum stop its listeners
- Start new GATT listener for the selected device (or ensure existing connection is used)
- All control commands (mode, intensity, timer, start/stop) must send to the selected device ONLY
- Heartbeat / notify callback must update selected device state
- UI state flows must refresh based on selected device

Provide functions:

fun switchBleTarget(deviceId: String)
fun observeBle(deviceId: String)
fun stopObserveBle(deviceId: String)

====================================================
# 7. Output Requirements
====================================================

Generate:

### (A) UI Layer
1. LazyRow of device cards + “+” add card
2. DeviceCard composable with selected/unselected styles
3. AddDeviceCard composable
4. Click handlers

### (B) ViewModel
1. combo_info model handling (append/remove/update)
2. selectedDeviceId handling
3. BLE switching logic
4. Data updating per device (battery, mute, connection)

### (C) Data Model
MassagerDevice must include:
- deviceId
- name
- mac
- battery
- isConnected
- muteState
- mode
- intensity
- timerValue

combo_info must include:
- mainDeviceId
- devices: List<MassagerDevice>

====================================================
# 8. Strict Instructions
====================================================

- DO NOT modify existing single-device protocol or command logic.
- DO NOT combine device control into group broadcast.
- Only switch control target based on selected card.
- Keep UI styling consistent with screenshot (rounded containers, soft colors).
- Do not remove any existing features; only enhance.

====================================================
# END OF PROMPT
====================================================
