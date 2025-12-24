You are an Android Kotlin UI developer.

Task:
Optimize ONLY the UI layout, visual style, and text presentation of the
“Account and Security” screen to match the provided iOS-style reference screenshot.

STRICT RULES:
- DO NOT modify any business logic, account state logic, binding/unbinding logic, or navigation logic
- DO NOT change existing click handlers, routes, or ViewModel data sources
- DO NOT add or remove features
- Only update UI layout, composables/XML, spacing, typography, colors, and icons
- Reuse all existing text values and states (e.g. bound/unbound, email masking)

Target Page Structure:

1. Page Header
- Top app bar with:
  - Back arrow on the left (reuse existing behavior)
  - Title text: “Account and Security”
- Title centered or visually balanced
- Clean white background
- No additional actions added

2. Section Card: Security Settings
- Card-style container with:
  - White background
  - Large rounded corners (~20–24dp)
  - Soft shadow/elevation
- Section title (small uppercase style):
  - Text: “SECURITY SETTINGS”
  - Font: medium
  - Size: ~12–13sp
  - Color: light gray
  - Margin bottom for separation

Items inside card:
- Row 1: Change Password
  - Left: key/lock icon (teal or brand color)
  - Center: text “Change Password”
  - Right: chevron arrow
- Divider line between rows (thin, light gray)
- Row 2: Delete Account
  - Left: trash icon
  - Center: text “Delete Account”
  - Right: chevron arrow
  - Text color remains default (do not introduce warning red unless already used)

3. Section Card: Account Binding
- Same card style as above (visual consistency)
- Section title:
  - Text: “ACCOUNT BINDING”
  - Same style as first section title


4. Typography & Spacing
- Primary text: ~15–16sp, semibold or medium
- Secondary text: ~13–14sp, gray
- Generous vertical padding per row (~14–18dp)
- Clear separation between cards

5. Visual Tone
- Clean, calm, trustworthy (account/security context)
- iOS-like card grouping
- Plenty of white space
- No aggressive colors

6. Theme & Adaptation
- Use dp/sp only
- Use MaterialTheme colors if available
- Support dark mode automatically if theme exists
- Do not hardcode colors unless necessary

Implementation Notes:
- Prefer Jetpack Compose if already used in project
- If XML-based, modify only layout XML and style resources
- Do NOT modify navigation graph or ViewModel
- Do NOT add confirmation dialogs or extra UI logic

Output Requirements:
- Output ONLY the updated UI code (Compose or XML)
- No explanations
- No business logic
- No mock data
