Build a "Device Control" screen for the Massager app using Kotlin (Jetpack Compose) or Flutter.

### Screen Title:
- "SmartPulse TENS Unit"
- Back arrow (←) on top left.
- Info button (ℹ️) on top right (opens a help or device info dialog).

### Layout sections:

#### 1️⃣ Device Display Area
- Centered device image (circular shadowed box with the TENS device icon).
- Next to it, a "+" placeholder area with a dotted border (for adding another device or channel).
- Below device image, show battery icon with charge level indicator.
- Mute button (speaker icon) on bottom-right corner to toggle sound feedback.

#### 2️⃣ Body Zone Tabs
A horizontal segmented tab control with 5 zones:
- SHLDR
- WAIST
- LEGS
- ARMS
- JC
Each represents a treatment area; selecting one highlights it (active tab has red underline or filled background).

#### 3️⃣ Mode Selection Grid
Under selected body zone, show a grid of buttons:
- Mode 1, Mode 2, Mode 3, Mode 4, Mode 5
- Use Material-style rounded rectangular buttons.
- Selected mode should highlight with red or gradient accent.

#### 4️⃣ Intensity Control
Below mode grid, a level control bar:
- Label: “Level”
- Center number showing current intensity (0–20)
- Two buttons: “–” (decrease) and “+” (increase)
- Add smooth animation when level changes.
- Disable buttons when min/max reached.

#### 5️⃣ Timer & Action Section
At the bottom:
- Left side: Timer selector
  - Small clock icon
  - Text: “0 min”
  - Tap to open a popup to select 5–60 minutes.
- Right side: Big yellow “STOP” button (rounded corners)
  - Toggles between “START” and “STOP” depending on state.
  - START → begins stimulation session
  - STOP → stops device output

#### 6️⃣ Feedback Elements
- Show toast or Snackbar for:
  - “Device started at Level 3, Mode 2”
  - “Session stopped”
  - “Battery low”
- Include vibration or sound feedback (optional) when increasing/decreasing intensity.

---

### Functional Behavior
- Maintain device state with ViewModel (or State in Flutter):
{
"zone": "SHLDR",
"mode": 1,
"level": 0,
"timer": 0,
"isRunning": false,
"isMuted": false
}
- When “START” pressed:
- Begin timer countdown (update UI every second).
- Disable zone/mode changes.
- Change STOP button to active red.
- When “STOP” pressed:
- Stop countdown and reset level to 0.
- Intensity level can only be changed while session is running.

### UI Style
- Soft, minimal, health-tech look.
- Rounded corners (16dp).
- Shadowed card background (#FFFFFF).
- Tabs and buttons use Massager’s brand red (#E54335).
- Animated transitions between modes.
- Responsive design (fits phones/tablets).
- Light & dark themes supported.

### Accessibility / Internationalization
- All texts and labels must come from string resources (no hardcoded strings).
- Provide both English and Simplified Chinese in:
- res/values/strings.xml
- res/values-zh/strings.xml
- Example:
<string name="device_title">SmartPulse TENS Unit</string>
<string name="level_label">Level</string>
<string name="start">START</string>
<string name="stop">STOP</string>
<string name="mode">Mode</string>
<string name="minutes">min</string>

---

### ⚙️ Optional Advanced Features
```plaintext
- Add BLE communication interface (mock only):
- sendCommand("SET_MODE", modeId)
- sendCommand("SET_LEVEL", intensity)
- sendCommand("START_SESSION", timer)
- sendCommand("STOP_SESSION")
- Simulate BLE responses with coroutine delay or Future.delayed().
- Save user’s last-used zone/mode/level to preferences.
- Allow real-time animation for “current intensity” (like glowing ring).
- Add safety timeout: stop session automatically after 60 minutes.
