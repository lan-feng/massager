You are an Android Kotlin UI developer.

Task:
Optimize ONLY the UI layout and displayed content of the LOWER SECTION
of the Bluetooth scan screen when NO devices are found,
based on the provided iOS reference screenshot.

IMPORTANT SCOPE LIMIT:
- DO NOT modify the top scanning radar area, scan animation, or title (“Add Device / Ready to scan”)
- DO NOT modify BLE scan logic, timers, retries, ViewModel, or state detection
- DO NOT change when this empty state appears; assume the condition already exists
- ONLY adjust the lower content area (below the radar)
- UI and text changes only

Target Area to Modify (Lower Half Only):

1. Status Icon (Optional enhancement)
- Centered simple Bluetooth / signal icon above the text
- Light gray or brand light teal color
- Minimal and soft, not dominant

2. Section Title
- Text: “Getting Started”
- Font: semibold
- Size: ~16–18sp
- Color: near-black
- Center aligned
- Spacing above and below increased for clarity

3. Instruction Text (Empty State Guidance)
- Display as a clear, readable numbered list:
  1. Ensure the device is powered on
  2. A flashing blue light indicates the device is waiting for Bluetooth connection
- Font: regular
- Size: ~14sp
- Color: medium gray
- Line height increased
- Center aligned or visually centered block
- Improve spacing between lines compared to default Android text

4. Primary Action Button
- Button text: “Scan Again”
- Rounded rectangle button
- Background color: light teal / brand primary light
- Text color: darker teal or white (depending on theme)
- Padding: generous horizontal padding
- Positioned clearly below the instructions
- Reuse the existing “scan again” click callback (do not modify logic)

5. Visual Tone
- Calm, friendly, instructional
- No error or warning colors (this is not a failure, just no devices found)
- Use plenty of whitespace

6. Responsiveness & Theme
- Use dp/sp only
- Respect existing MaterialTheme / color scheme
- Support dark mode automatically if theme exists

Implementation Notes:
- Prefer Jetpack Compose if the project already uses Compose
- If XML-based, modify only the lower layout container
- Do NOT add new composables that affect scan logic
- Do NOT introduce loading or progress indicators

Output Requirements:
- Output ONLY the updated UI code (Compose or XML)
- No explanations
- No business logic
- No BLE code
