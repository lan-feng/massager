You are an Android Kotlin UI developer.

Task:
Optimize ONLY the UI layout and text presentation of the existing “communication error / connection timeout”
dialog to match the provided iOS-style reference screenshot.

STRICT RULES:
- DO NOT change any BLE connection logic, timeout logic, retry logic, or error codes
- DO NOT modify ViewModel, use cases, repositories, or exception sources
- DO NOT introduce new business logic
- Only update dialog UI layout, typography, spacing, colors, shapes, and displayed text
- Reuse the existing error trigger and confirm button callback (OK button)

Target Dialog Style (iOS-like alert):

1. Dialog Container
- Centered modal dialog
- Rounded rectangle with large corner radius (~20–24dp)
- Background color: white
- Soft shadow / elevation
- Fixed max width (mobile-friendly), height wraps content

2. Title
- Text: “Communication Error”
- Font: semibold
- Size: ~18sp
- Color: near-black
- Center aligned
- Margin bottom: ~12–16dp

3. Main Error Message
- Text (single paragraph):
  “Device connection timed out (10s). Please try again.”
- Font: regular
- Size: ~14–15sp
- Color: dark gray
- Center aligned
- Line spacing slightly increased for readability

4. Instruction List (secondary guidance)
- Displayed as a numbered list, centered:
    1. Ensure the device is powered on
    2. A flashing blue light indicates the device is waiting for Bluetooth connection
- Font: regular
- Size: ~13–14sp
- Color: medium gray
- Line spacing increased
- Add top margin to separate from main message

5. Divider
- Thin horizontal divider line above the confirm button
- Color: very light gray
- Full width of dialog

6. Action Button
- Single button: “OK”
- Centered horizontally
- Text color: system blue / primary brand blue
- Font: medium
- Height similar to iOS alert button
- Reuse existing onClick callback (dismiss / retry logic unchanged)

7. Background Mask
- Dim the background behind the dialog using a semi-transparent scrim
- Optional slight blur effect on background if supported
- Ensure only the dialog is fully clear and in focus

8. Accessibility & Adaptation
- Text should wrap correctly on small screens
- Use dp/sp only
- Support dark mode automatically if theme exists (do not hardcode colors if theme colors are available)

Implementation Notes:
- Prefer Jetpack Compose AlertDialog / custom Dialog if project uses Compose
- If XML is used, modify only dialog layout XML and styles
- Keep the dialog behavior modal (outside click disabled unless already allowed)

Output Requirements:
- Output ONLY the updated UI code (Compose or XML)
- Do NOT include explanations
- Do NOT include business logic
- Do NOT modify exception handling