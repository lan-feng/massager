You are an Android Kotlin UI developer.

Task:
Modify ONLY the UI layout and presentation of the EMS device list screen to implement
an iOS-style “selected EMS + floating action panel + dim/blur overlay” effect based on the provided screenshot.

STRICT RULES (must follow):
- DO NOT modify BLE scanning/connection logic, repository, ViewModel business logic, or data models
- DO NOT change existing click / long-press callbacks behavior (reuse them exactly)
- DO NOT introduce new domain logic; only UI state wiring using existing selected state
- Only change UI composables/layout, modifiers, shapes, colors, elevation, overlay, blur, and animations
- The new floating action panel MUST REPLACE the old bottom sheet / bottom popup UI (remove it from UI layer)
- Ensure the screen shows at most ONE selected EMS device at a time AND only ONE floating panel

Required UI Behavior:
1) Single selection:
- At any time, only one EMS device item can be “selected/expanded”.
- Reuse existing state: e.g. selectedDeviceId / expandedDeviceId / currentSelected / isItemExpanded.
- If current code supports multi-expanded, update UI layer so it renders only for the currently selected id.

2) Replace bottom popup:
- The original bottom sheet / bottom bar action popup should no longer be shown.
- Instead, show a floating action panel directly under the selected EMS card (like iOS screenshot).

3) Overlay + focus effect:
- When a device is selected (panel visible):
  - Display a full-screen dim overlay (scrim) above the list background.
  - Blur or soften EVERYTHING behind the focus layer (non-selected content + background).
  - Keep the selected EMS card and the floating action panel ABOVE the overlay (fully clear, no blur).
- Only the selected EMS card and the floating panel are highlighted (focus mode).

4) Dismiss behavior (UI only):
- Tapping on the dim overlay (outside the card/panel) should call the existing “collapse/deselect” handler if available.
- If no handler exists, reuse the same callback currently used to close the bottom popup.
- No new business logic; just call existing callbacks.

Target UI Layout (match screenshot):

A. Background:
- Light gray gradient background (top slightly darker gray to bottom lighter gray).
- Very subtle.

B. Selected EMS Card (top focus):
- Large white rounded rectangle card, corner radius ~22-26dp
- Stronger shadow/elevation (soft)
- Inner layout: Row
  - Left: device image/icon inside rounded square container (light gray)
  - Middle: Column
     - Title: "BLE_EMS" (semibold, 16-18sp, near-black)
     - Subtitle: "SerialNo 272b9a66" (14sp, gray)
  - Right: chevron arrow ">" (tint light gray)

C. Floating Action Panel (under selected card):
- Rounded rectangle panel, corner radius ~18-22dp
- Background: very light gray / near-white
- Slight elevation
- Two rows:
  1) Rename row:
     - Left text "Rename" (dark)
     - Right pencil icon (teal/brand color)
  Divider: thin line, light gray
  2) Delete row:
     - Left text "Delete" (red)
     - Right trash icon (teal or matching accent; but text must be red like screenshot)
- Panel width similar to card but slightly narrower
- Panel aligned centered under the selected EMS card
- Spacing between card and panel ~16-20dp

D. Focus-only rendering:
- When selected:
  - The selected EMS card should appear in its original position (top of list or where it is),
    but it must visually float above the dimmed background.
  - The rest of list items should appear behind dim overlay and optionally blurred.

Implementation Notes (Compose preferred):
- Use a root Box:
  - Layer 1: normal screen content (background + list)
  - Layer 2: if selected -> draw scrim overlay (semi-transparent black/gray) + optional blur on underlying layer
  - Layer 3: focused content (selected card + floating panel) drawn above scrim
- Blur:
  - If Compose supports Modifier.blur(radius), apply blur to the background content layer when selected.
  - If blur is not available due to minSdk, use only scrim dimming (still acceptable).
- Animations:
  - Add subtle fade-in + scale-in for the floating panel (UI only).
  - Do not change any logic.

Output Requirements:
- Output ONLY the updated Compose UI code (or XML if project is XML-based)
- Do NOT include explanations
- Do NOT modify business logic files
- Keep existing function names/callbacks and simply reuse them in onClick/onLongClick.
