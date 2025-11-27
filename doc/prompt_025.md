You are modifying the UI of the Device Control screen in our Android Jetpack Compose project.
Do NOT change any business logic, Bluetooth logic, state handling, timers, or ViewModel code.
ONLY improve the UI layout, spacing, alignment, and visual grouping.

Use the following screenshot as the UI reference:
[screenshot: /mnt/data/screen.png]

## UI Optimization Requirements

### 1. Device Info Card + Add Device Card
- Keep existing device connection logic unchanged.
- Improve UI layout to look more modern:
  - Rounded corners (12–16dp)
  - Subtle shadows / elevation
  - Better spacing between elements
  - Tighten icon alignment (battery, sound, BLE icon)
- Two cards should appear in a horizontal row at top:
  - Left: Device Info (connected device)
  - Right: "Add Device" dashed border card
- Ensure the row is responsive:
  - When screen width is small, allow horizontal scroll for multiple device cards (future compatibility)
- Keep all click handlers unchanged.

### 2. Main Timer Circular Component
- Maintain all existing countdown logic.
- Improve UI visual:
  - Center the circle
  - Increase whitespace around the circle
  - Make the ring thickness visually aligned with screenshot
  - Ensure text is centered and uses improved typography
  - Keep “Tap to Stop” behavior unchanged.

### 3. Intensity Slider
- Keep intensity level business logic EXACTLY as is.
- Improve UI only:
  - Rounded track
  - Custom thumb (slightly larger with subtle shadow)
  - Clearer min and max markers (“1” and “20”)
  - Align “Level: X” text to the right, bold, modern style
  - Ensure Slider width uses full screen width minus horizontal padding

### 4. Mode Selection Grid
- Do NOT modify mode selection logic or dispatched BLE commands.
- Improve visual grouping:
  - Use 2–3 columns grid layout
  - Each mode button:
    - Rounded 12dp
    - Shadow or border
    - Uniform height
    - Icon + label centered
  - Selected mode uses filled background (brand color)
  - Unselected mode uses light gray background
- Maintain the existing onClick callbacks.

### 5. Body Part Selection
- Keep internal logic unchanged.
- Improve UI consistency with Mode Selection:
  - Same button style
  - Same spacing
  - Same selected/unselected behavior
  - Ensure grid layout aligns visually

### 6. Overall Layout Improvements
- Add consistent horizontal padding (16–20dp)
- Use vertical spacing increments of 12–20dp
- Use typography styles from Material3 to unify text appearances
- Ensure all components scroll properly within a `LazyColumn`
- Apply a clean, modern color scheme close to the screenshot
- Avoid modifying ViewModel state names, flow, logic, or BLE operations

## Deliverables
- Updated `DeviceControlScreen.kt` with improved Composables
- Minimal or no changes to `DeviceControlViewModel.kt`
- All state management and onClick logic must remain intact

Only perform UI refactoring. No logic changes allowed.
