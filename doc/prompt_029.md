You are a senior Android Jetpack Compose UI engineer.

Generate a reusable Jetpack Compose component named:

DeviceSwitchCard

This component represents the top Bluetooth device connection card. It must strictly follow the visual and interaction behavior described below. Do NOT include any business logic related to BLE. Only UI + state mapping.

-------------------------
✅ Input Parameters
-------------------------

name: String                    // device display name
subtitle: String                // device subtitle
isSelected: Boolean             // whether this card is selected
connectionState: ConnectionState  // IDLE | CONNECTING | READY | DISCONNECTED
batteryLevel: Int?              // 0–4, nullable
buzzerOn: Boolean               // buzzer switch state
isInteractive: Boolean          // whether user interaction is allowed

onReconnect: () -> Unit         // triggered only in DISCONNECTED state
onBuzzerToggle: (Boolean) -> Unit
onCardTap: () -> Unit           // triggered only in READY or CONNECTING state

-------------------------
✅ Connection State Rules
-------------------------

enum class ConnectionState {
    IDLE,
    CONNECTING,
    READY,
    DISCONNECTED
}

-------------------------
✅ Visual State Mapping
-------------------------

READY:
- Background: glassmorphism (semi-transparent white)
- Border: primary color, 1dp
- Shadow: soft shadow
- Content: battery indicator + buzzer toggle visible
- Badge: green "Connected"

CONNECTING:
- Background: light neutral gray
- Border: light gray 1dp
- No battery, no buzzer
- Badge: loading spinner

IDLE:
- Background: very light gray
- Border: gray 1dp
- No shadow
- All interactions disabled
- No badge

DISCONNECTED:
- Background: red tint with 10% opacity
- Border: red 2dp
- Shadow: red glow
- Footer text: "Tap to Reconnect"
- Badge: red "Disconnected" with refresh icon

-------------------------
✅ Click Behavior
-------------------------

If state == DISCONNECTED → entire card click triggers onReconnect  
If state == READY or CONNECTING → entire card click triggers onCardTap  
If state == IDLE → no click allowed  

-------------------------
✅ Buzzer Toggle
-------------------------

- Only visible when state == READY
- Disabled when isInteractive == false
- When toggled → call onBuzzerToggle(newValue)

-------------------------
✅ Battery Indicator
-------------------------

- Only visible when state == READY
- Render 0–4 bars visually
- If batteryLevel == null → hide

-------------------------
✅ Disconnect Pulse Animation
-------------------------

Only when state == DISCONNECTED:

Scale animation:
1.0 → 1.02 → 1.0
Duration: 1500ms
Easing: EaseInOut
Loop infinitely

-------------------------
✅ Output Requirements
-------------------------

- Use pure Jetpack Compose
- Use Material3
- Provide:
  - Main @Composable DeviceSwitchCard
  - Internal BatteryIndicator composable
  - Internal StatusBadge composable
- No fake BLE logic
- No preview required
- Only output valid Kotlin Compose code
