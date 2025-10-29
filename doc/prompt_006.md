Build an "Account and Security" screen for the ComfyTemp app using Kotlin (Jetpack Compose) or Flutter.

### Layout requirements:
- Page title: “Account and Security”
- Top-left: Back button (←)
- Section 1: Basic account info
  - Mail: display user email (e.g., "lanfeng525@gmail.com"), not editable
  - Set Password: navigate to “Change Password” screen
- Section header: “Third-party account information”
- Section 2: Third-party account binding list
  1. Facebook — status text: “Go to binding” (gray color)
     - Facebook logo icon
  2. Google — status text: “Go to binding”
     - Google logo icon
  3. Apple — status text: “Go to binding”
     - Apple logo icon
  - When tapped, show a toast or dialog: “Third-party binding feature coming soon”
- Section 3: Delete account option
  - Row with text “Delete account” → navigate to confirmation page
- Bottom area: 
  - Large red “Log out” button with border and rounded corners
  - On click: show confirmation dialog “Are you sure you want to log out?”
- Background color: light gray (#F8F8F8)
- Use Material 3 ListItems with divider spacing between sections
- Add smooth transition animations when entering and leaving the screen

### Behavior requirements:
- Email should be loaded from user profile (mock data or shared preferences).
- Log out button clears user session and navigates back to Login screen.
- Handle dark mode automatically.
- Support internationalization (i18n):
  - res/values/strings.xml (English)
  - res/values-zh/strings.xml (Simplified Chinese)
- All text strings should come from resource files (no hardcoded text).

### Example string resources:
<string name="account_security_title">Account and Security</string>
<string name="email_label">Mail</string>
<string name="set_password">Set Password</string>
<string name="third_party_title">Third-party account information</string>
<string name="go_to_binding">Go to binding</string>
<string name="delete_account">Delete account</string>
<string name="logout">Log out</string>
<string name="logout_confirm">Are you sure you want to log out?</string>

### Mock data example:
{
  "userEmail": "lanfeng525@gmail.com",
  "facebookBound": false,
  "googleBound": false,
  "appleBound": false
}

### Optional enhancements:
Implement third-party account binding logic:
- When user taps "Facebook" / "Google" / "Apple", start OAuth login.
- After success, show status “Bound” and disable further binding.
- Store binding info in local preferences.


Create a "Delete Account" confirmation screen:
- Explain that deleting the account will erase all personal data.
- Require user to confirm by typing "DELETE".
- Add a final “Confirm Delete” red button.
