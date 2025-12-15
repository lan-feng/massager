Role
You are a senior Android UI engineer. Your task is to visually restructure an existing BLE scan screen using Jetpack Compose.

🔴 CRITICAL REQUIREMENT (MUST FOLLOW)

⚠️ This is NOT a minor style tweak task.
⚠️ You MUST rebuild the page layout structure.

❗ The final UI MUST have:

A large radar scanning component at the TOP (independent composable)

A device card list BELOW the radar

If the radar is missing → the result is WRONG

📌 Context

Current Android page: simple BLE scan list (similar to Attachment 1)

Target page: iOS-style Add Device page (Attachment 2 & 3)

BLE scan logic already exists

You must NOT change BLE core logic

🧱 Mandatory Page Layout (STRUCTURE FIRST)
🔹 Overall Layout (STRICT)
Column (Full Screen)
 ├── TopAppBar ("Add Device")
 ├── RadarScanArea (Fixed height, centered)
 ├── ScanStatusText
 ├── DeviceList (LazyColumn)
 └── BottomActionButton (optional)


⚠️ Do NOT merge radar and list
⚠️ Radar must be a separate composable and visually dominant

🟢 1️⃣ RadarScanArea (MOST IMPORTANT)

You MUST implement a radar-style scanning UI using Canvas.

Visual requirements:

Size: 240dp – 300dp square

Centered horizontally

Elements:

Outer circle (thin stroke)

Inner dashed circle

Crosshair (horizontal + vertical)

Center dot or square

Rotating scan sector (fan-shaped sweep)

Animation rules:

While scanning:

Sweep rotates infinitely (360° loop)

When scan stops:

Animation stops

Center icon changes to magnifier icon

Implementation requirements:

Use:

Canvas

rememberInfiniteTransition

animateFloat

Name composable:

@Composable
fun RadarScanView(isScanning: Boolean)


⚠️ This component must be clearly visible and NOT subtle

🟢 2️⃣ Scan Status Text (Below Radar)

Centered text below radar:

Scanning:

Scanning for devices...


Scan stopped:

Found X devices


Font:

Medium / Title style

Clear spacing from radar

🟢 3️⃣ Device List (Below Radar Section)
Layout rules:

Must start below radar + status text

Use LazyColumn

Each item is a rounded card

Device Card Design:

Height: ~72–88dp

Rounded corners

White background

Soft shadow

Left

Fixed device image placeholder

Middle

Device name (bold)

Serial number (smaller, gray)

Right

RSSI signal icon (red)

RSSI value (e.g. -90)

Chevron >

Interaction:
onClick {
    onDeviceSelected(device)
}


⚠️ DO NOT inline device rows into the radar area

🟢 4️⃣ Scan Trigger Behavior (LIGHT LOGIC ONLY)

You MAY adjust click triggers but NOT logic:

Page enter → call startScan()

Radar tap → call rescan()

Bottom button:

Scanning → Stop Scanning

Idle → Rescan

Only call existing functions:

startScan()
stopScan()

🚫 Explicitly Forbidden

❌ No “small animation near title”
❌ No inline scan indicator inside list
❌ No reuse of old list-only layout
❌ No BLE logic rewrite
❌ No ViewModel refactor

📤 Expected Output (MANDATORY)

You MUST output:

AddDeviceScreen() – full page

RadarScanView() – Canvas-based radar

DeviceCard() – reusable device item

Clear comments:

// UI only – BLE logic unchanged


If radar is missing → output is invalid.