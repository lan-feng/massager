You are improving the UI layout of the Login Screen in a Jetpack Compose Android project.

IMPORTANT:
Do NOT change any business logic, ViewModel code, user authentication logic,
guest login logic, or navigation behavior.  
Only adjust the UI layout, styling, spacing, and visual structure.

Use the attached screenshot as the UI design reference:
/mnt/data/9a8b7b0b-5b9b-4214-ab97-aff66ad3ee59.png

--------------------------------
### UI Optimization Requirements
--------------------------------

### 1. Page Structure
- The login screen must remain vertically scrollable using `Column` + `verticalScroll`.
- Maintain existing login, register, forgot password, guest login, and Google/Facebook login features.
- Visually center the main content, but allow natural scrolling on small screens.
- Ensure large top whitespace is optimized: keep enough breathing space, but not excessive.

### 2. Logo Section
- Keep the existing logo; only improve layout:
  - Center horizontally.
  - Add a subtle drop-shadow.
  - Space it evenly from the input fields (approx 32–48dp).

### 3. Input Fields (Email + Password)
- Do NOT modify any validation logic or onValueChange handling.
- Improve UI:
  - Rounded corners (12–16dp)
  - Soft light background (`#F6F6F6`)
  - Add inner padding (16dp)
  - Icon on the left with consistent size + alignment
  - Use Material3 text styles for labels and text
  - Increase vertical spacing between fields

### 4. "Register" & "Forgot Password?" Row
- Keep original click actions.
- Improve styling:
  - Match brand color (teal/green tone from screenshot)
  - Increase font size slightly
  - Place them on opposite ends with proper baseline alignment
  - Add top spacing (~8–12dp)

### 5. Login Button
- Keep login logic as-is.
- Improve UI:
  - Full width button
  - Rounded corners (20–24dp)
  - Gradient or solid brand color background
  - Increase vertical padding for better tap target
  - Use bold text with improved contrast

### 6. Guest Login Button
- Keep existing guest-login business logic unchanged.
- Style improvements:
  - Outline button with brand color border
  - Slightly thicker border (1.5–2dp)
  - Rounded corners (20–24dp)
  - Increase vertical padding
  - Ensure contrast against background

### 7. Social Login Section (Google / Facebook)
- Keep original onClick behaviors.
- Improve UI:
  - Even spacing between logos
  - Add muted dividing text “OR”
  - Use subtle horizontal divider lines
  - Slight shadow around circular icons

### 8. Footer Section (User Agreement / Privacy Policy)
- Preserve navigation routing as-is.
- Improve the UI:
  - Centered horizontal layout
  - Add vertical spacing from social buttons
  - Increase touch target spacing
  - Match brand color and typography

### 9. Color + Typography Consistency
- Adopt a unified color palette consistent with screenshot (green/teal).
- Use Material3 typography (`titleMedium`, `bodyMedium`, etc.).
- Keep consistent spacing increments: 8dp, 12dp, 16dp, 24dp.

### 10. Code Requirements
- Modify only `LoginScreen.kt` UI Composables.
- No changes to:
  - ViewModel
  - State objects
  - Navigation
  - Authentication logic
  - Guest login logic
- All IDs, state names, and function names must remain unchanged.

--------------------------------
### Output
--------------------------------
Refactor the Compose UI code to match the updated layout and styling above.
Do NOT modify logic. Only UI enhancements.
