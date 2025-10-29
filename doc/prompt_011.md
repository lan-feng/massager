Profile Optimization

Reference swagger.yaml 
### Data:
 Personal Information and HeaderSection use interface "/auth/v1/user/info" .
 EditName use interface "/auth/v1/user/update" to update name.
 Change Avatar use interface "/common/v1/files/upload" to upload avatar,and use interface "/auth/v1/user/update" to update avatar.
 Account and Security use interface "/auth/v1/user/change-password" to Set Password.
 Delete account use interface "/auth/v1/user/del" to delete account.

### 
Build a "Set New Password" screen for the ComfyTemp app using Kotlin Jetpack Compose (or Flutter).

### 🎯 Page Purpose
Allow the user to update their account password securely.
Includes input validation, error hints, and submission handling.



### Layout Structure

#### App Bar
- Title: “Set new password”
- Left: Back arrow (←)
- Right: “Submit” button (text, red accent #E54335)
 - Disabled until all fields are valid.

#### Input Fields (three rows)
Each input field should use a rounded rectangular outline style with clear labels and placeholders.

1. **Old Password**
 - Label: “Old Password”
 - Placeholder: “Please enter your password”
 - Type: password (masked, with visibility toggle icon)

2. **New Password**
 - Label: “New Password”
 - Placeholder: “Please enter your password”
 - Type: password (masked)
 - Inline validation:
  - Must be 6–12 characters
  - Must include both letters and numbers
  - Cannot be purely numeric

3. **Confirm Password**
 - Label: “Confirm Password”
 - Placeholder: “Please enter your password”
 - Type: password (masked)
 - Inline validation: must match the “New Password” field.

Below input fields, display a small gray text reminder:
> “Passwords must be 6–12 characters with a mix of letters and numbers (no pure numbers).”

#### Footer Section
- Text button: “Forget your password” (red color #E54335)
 - Navigates to “Forgot Password” screen.

---

### ⚙️ Validation & Behavior

**Validation Rules**
- Old password not empty.
- New password matches pattern:
 - Regex example: `^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,12}$`
- Confirm password matches new password.

**Submit Action**
- When “Submit” clicked:
 1. Validate all fields.
 2. If valid → call mock function `changePassword(old, new)` (simulate backend request).
 3. Show loading spinner or progress indicator.
 4. use interface "/auth/v1/user/change-password" to Set Password
 5. Navigate back to “Account and Security” screen.

**Error Handling**
- If old password is incorrect (simulate API error) → show red Snackbar “Old password incorrect”.
- If new password invalid → highlight text field border in red.

---

### 💡 UI Design Notes
- Background color: #FAFAFA (light neutral)
- Input card background: white (#FFFFFF)
- Accent color: #E54335 (ComfyTemp Red)
- Font: Medium weight, 16sp for labels, 14sp for placeholders.
- Submit button text color: red (#E54335) with pressed alpha.
- Smooth transitions between fields when focusing.
- Keyboard “Done” triggers next field focus or submit.

---

### 🌐 Internationalization
Provide both English and Simplified Chinese string resources.

**English (values/strings.xml)**


