Create a "Device Management" screen for the Massager app using Kotlin Jetpack Compose (or Flutter).

### 🎯 Page Purpose
Allow users to select a device from their list and perform management actions such as **Rename** or **Remove device**.

---

### 🏗 Layout Structure

#### 1️⃣ Header Section
- Title: “Welcome to Massager”
- Right corner: “+” button for adding new devices.
- Subheading: “Common Device”
  - Font: medium, color #666666
  - Align left.

#### 2️⃣ Device Card List
- Display device cards with rounded corners and soft shadows.
- Each card shows:
  - Device image (e.g. TENS Unit icon)
  - Device name: “SmartPulse TENS Unit”
  - Subtitle: “SmartPulse TENS Device”
  - Selection state indicator:
    - Default: no overlay.
    - When selected: red check mark (✓) in top-right corner.
- Tap card to toggle selection.
- Allow multiple selections (optional).
- Animations:
  - Fade-in on appearance.
  - Scale-up when selected.

#### 3️⃣ Bottom Action Panel
Fixed bottom action bar with rounded top corners:
- Background: light gray with subtle elevation.
- Contains two primary action buttons:
  1. **Rename**
     - Icon: pencil or edit symbol.
     - Color: teal (#16A085)
     - On click → open rename dialog.
  2. **Remove device**
     - Icon: trash can.
     - Color: red (#E54335)
     - On click → open confirmation dialog.
- Below action buttons, centered text button “Cancel” to exit management mode.

---

### ⚙️ Functionality

#### ① Rename Device
When “Rename” is tapped:
- Open modal dialog with:
  - Title: “Rename Device”
  - TextField pre-filled with current name.
  - Placeholder: “Enter new device name”
  - Buttons:
    - “Cancel”
    - “Save” (disabled if field empty)
- Validation:
  - Name must be 2–30 characters.
  - No special symbols.
- On Save:
  - Update device name in list.
  - Show toast “Device renamed successfully”.

#### ② Remove Device
When “Remove device” is tapped:
- Show confirmation dialog:
  - Title: “Remove Device?”
  - Message: “Are you sure you want to remove this device from your list?”
  - Buttons:
    - “Cancel”
    - “Confirm” (red text)
- On confirm:
  - Remove device from list.
  - Show toast “Device removed successfully”.
- Optionally simulate network delay (1s–2s).

#### ③ Cancel Button
- Deselect all devices and exit management mode.

---

### 💡 UI Design
- Background: #FAFAFA (light neutral)
- Card background: #FFFFFF
- Accent color: Massager Red (#E54335)
- Typography:
  - Header: 20sp, semi-bold.
  - Device name: 16sp, medium.
  - Subtitle: 13sp, gray #777.
- Icons use Material Icons or CupertinoIcons for cross-platform compatibility.
- Bottom bar height: ~120dp with spacing between buttons.

---

### 🌐 Internationalization (i18n)
All UI strings must come from resource files.

**English**
<string name="rename">Rename</string>
<string name="remove_device">Remove device</string>
<string name="cancel">Cancel</string>
<string name="rename_device_title">Rename Device</string>
<string name="rename_placeholder">Enter new device name</string>
<string name="save">Save</string>
<string name="remove_device_confirm_title">Remove Device?</string>
<string name="remove_device_confirm_message">Are you sure you want to remove this device from your list?</string>
<string name="confirm">Confirm</string>
<string name="device_renamed">Device renamed successfully</string>
<string name="device_removed">Device removed successfully</string>

**Simplified Chinese**
<string name="rename">重命名</string>
<string name="remove_device">移除设备</string>
<string name="cancel">取消</string>
<string name="rename_device_title">重命名设备</string>
<string name="rename_placeholder">请输入新的设备名称</string>
<string name="save">保存</string>
<string name="remove_device_confirm_title">移除设备？</string>
<string name="remove_device_confirm_message">确定要从列表中移除此设备吗？</string>
<string name="confirm">确认</string>
<string name="device_renamed">设备重命名成功</string>
<string name="device_removed">设备已移除</string>

### Data:
Remove “Confirm” use interface "/device/v1/delById/{id}".
Rename “Save” use interface "/device/v1/update" .