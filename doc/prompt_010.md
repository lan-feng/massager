Build an "Add Device" screen for the Massager app using Kotlin Jetpack Compose (or Flutter).

### Screen purpose:
- Used to scan for nearby BLE (Bluetooth Low Energy) devices.
- Displays a radar-style animation while scanning.
- Allows users to manually add a device if not found.

### Layout structure:

#### Top Bar
- Title: “Add Device”
- Left: Back arrow (←)
- Right: (optional) Help icon or refresh button.

#### Scanning Visualization
- Centered animated radar circle indicating scanning process.
  - Soft gradient blue color (#DDE7FF → #A3C1FF)
  - Rotating sweep animation to simulate radar.
  - Ripple effect or wave pulse expanding from center.
- Label below animation: “Searching for nearby devices”
  - Center-aligned, light gray text (#888888).

#### Found Device List (appears dynamically)
- When devices are discovered, display them below the radar:
  - Rounded white cards with slight shadow.
  - Device name (e.g., "SmartPulse TENS Unit")
  - MAC address (optional)
  - Connection status (e.g., “Tap to connect”)
  - Icon: small Bluetooth logo or device thumbnail.
  - Tap → navigate to “Device Control Page”.

#### Bottom Button
- “Add Manually” button at bottom (always visible)
  - Full width, light blue (#A3C1FF) outline style.
  - Text color: #4B7BE5
  - Rounded corners (12dp)
  - On click → navigate to “Manual Add Device” screen.

### Behavior & Logic:

#### Bluetooth Scanning
- When screen opens:
  - Show radar animation + “Searching for nearby devices”
  - After 3–5 seconds, display mock discovered devices list.
- When a device is tapped:
- Show loading spinner “Connecting…” , call the interface "/device/v1/bind" and save device info.
- After 2s → “Connected successfully!”
- Navigate to Home Screen Page.
tip: Configure different device types through device names.Unified the reserved device-type and device-names relations.
 
#### Manual Add
- Navigates to a new screen with input fields:
- Device Name
- MAC Address
- Confirm button → adds mock device to list.

#### Optional States
- Empty state: “No devices found” after 10 seconds.
- Show illustration + “Try again” button.
- Error state: if Bluetooth is off → prompt to enable it.

### UI Design Notes
- Use minimalist white background (#FAFAFA)
- Main accent color: Massager Red (#E54335)
- Soft shadows and rounded edges for all elements.
- Radar animation area should be roughly 250–300dp wide.
- Use smooth Compose/Flutter animation APIs (infinite repeatable rotation).
- Transitions:
- Fade-in for device list.
- Scale-in for radar center circle.


###  Internationalization
All text strings must use resource files:
<string name="add_device_title">Add Device</string>
<string name="searching_devices">Searching for nearby devices</string>
<string name="add_manually">Add Manually</string>
<string name="no_devices_found">No devices found</string>
<string name="try_again">Try Again</string>
<string name="connecting">Connecting...</string>
<string name="connected_success">Connected successfully!</string>
Provide translations in `values-zh/strings.xml` for Simplified Chinese:
<string name="add_device_title">添加设备</string>
<string name="searching_devices">正在搜索附近设备</string>
<string name="add_manually">手动添加</string>
<string name="no_devices_found">未发现设备</string>
<string name="try_again">重试</string>
<string name="connecting">正在连接...</string>
<string name="connected_success">连接成功！</string>