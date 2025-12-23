You are an Android Kotlin UI developer.

Task:
Refactor ONLY the UI layout and visual style of the existing “No Device / Add Device” screen
to match the provided iOS-style reference screenshot.

STRICT RULES:
- DO NOT modify any existing business logic, BLE scan logic, ViewModel, state flow, or data sources
- DO NOT rename or remove existing click handlers or callbacks
- Only update layout, composables, XML, styles, colors, spacing, and text presentation
- If a button already has an onClick, reuse it exactly

Target UI Description:


1. Main “Add Your First Device” Card
- Centered horizontally
- Large rounded rectangle card
- Corner radius: large (≈ 20–24dp)
- Card background: white with subtle gradient or shadow
- Dashed border around the card (stroke style)
- Border color: light blue / teal
- Padding inside card: generous (24–32dp)

Card Content (vertical alignment, centered):
- Top: circular blue button with white “+” icon
  - Circle diameter ≈ 48–56dp
  - Blue color: primary brand blue
- Title text below icon:
  - Text: “Add Your First Device”
  - Font: medium / semibold
  - Size: ~18–20sp
  - Color: near-black
- Subtitle text:
  - Text: “Tap to add your EMS device”
  - Smaller font (~14sp)
  - Color: gray

The entire card should be clickable and trigger the existing “add device / scan” action.

2. Getting Started Section (below card)
- Title text: “Getting Started”
  - Centered
  - Font: semibold
- Instruction text (smaller, gray, multiline):
  1. Ensure the device is powered on
  2. A flashing blue light indicates the device is waiting for Bluetooth connection
- Line spacing slightly increased for readability
- Center aligned text

3. Top Right Add Icon
- Keep the existing “+” icon in the top-right corner
- Do not change its click behavior
- Adjust only spacing or tint if needed

4. Bottom Navigation
- Keep existing bottom navigation (Home / Settings icons)
- Do not modify logic
- Only adjust icon tint or spacing if required to match light theme

Implementation Notes:
- Prefer Jetpack Compose if the project already uses Compose
- If XML is used, update only layout XML and styles
- Use MaterialTheme colors if available
- All sizes should be responsive (dp/sp, no hard px)

Output:
- Provide updated Kotlin Compose code OR XML layout code only
- Do NOT include explanations
- Do NOT include business logic
