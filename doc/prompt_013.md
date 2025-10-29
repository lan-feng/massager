Build a "Forget Password" screen for the ComfyTemp app using Kotlin Jetpack Compose (or Flutter).

### Page Purpose
Allow the user to reset their account password by email verification.

---

### 🏗 Layout Structure

#### 1️⃣ App Bar
- Title: “Forget your password”
- Left: Back arrow (←)
- No right-side actions.

#### 2️⃣ Logo Area
- Centered app logo (red “C” icon with gradient #E54335 → #D22020)
- Add subtle drop shadow and fade-in animation.

#### 3️⃣ Input Fields (three)
Each field should use an outlined or underlined text style with clear placeholder text.

1. **Email Input**
   - Placeholder: “Please enter your email”
   - Type: email, with validation (must contain “@”)
   - Icon: envelope or mail outline (optional)

2. **Verification Code Input**
   - Placeholder: “Please enter the verification code”
   - Right side: “Get code” button
     - Style: text button, red accent (#E54335)
     - On press: 
       - Send verification code to email.
       - Start 60s countdown timer (disable button while counting).

3. **New Password Input**
   - Placeholder: “Please enter your password”
   - Type: password (masked with toggle icon)
   - Below: text rule in red:
     > “Passwords must be 6–12 characters with a mix of letters and numbers (no pure numbers)”

#### 4️⃣ Submit Button
- Large red rounded button (“Submit”)
- Full width, padding 16dp, radius 50dp.
- Disabled until all fields are valid.
- On click:
  - Validate all fields.
  - Simulate backend request (2s delay).
  - Show toast “Password reset successfully!” and navigate to Login screen.

---

### ⚙️ Behavior Logic

#### Validation Rules:
- Email: valid format.
- Verification code: not empty.
- Password: matches pattern  
  `^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,12}$`

#### Verification Flow:
1. User enters email → clicks “Get code”.
2. Show toast “Verification code sent”.
3. Countdown (60s): button text shows “Resend in 59s …”.
4. Enable button again after countdown.

#### Submit Flow:
1. Validate inputs.
2. If valid → call `resetPassword(email, code, newPassword)` (mock function).
3. Show progress indicator.
4. On success → show Snackbar “Password updated successfully” → navigate to login screen.

---

### 💡 UI Design
- Background: white (#FFFFFF)
- Accent color: ComfyTemp Red (#E54335)
- Font: sans-serif-medium, 16sp for labels, 14sp for hints.
- Input underline color: light gray (#E0E0E0)
- Rounded corners for buttons.
- Align everything vertically centered with sufficient spacing (24dp).
- Keyboard “Done” key triggers submit if valid.

---

### 🌐 Internationalization
Provide English and Simplified Chinese strings.

**English (values/strings.xml)**
<string name="forget_password_title">Forget your password</string>
<string name="email_hint">Please enter your email</string>
<string name="verification_hint">Please enter the verification code</string>
<string name="get_code">Get code</string>
<string name="password_hint">Please enter your password</string>
<string name="password_rule">Passwords must be 6–12 characters with a mix of letters and numbers (no pure numbers)</string>
<string name="submit">Submit</string>
<string name="code_sent">Verification code sent</string>
<string name="password_reset_success">Password reset successfully!</string>
<string name="invalid_email">Invalid email format</string>

**Simplified Chinese (values-zh/strings.xml)**
<string name="forget_password_title">忘记密码</string>
<string name="email_hint">请输入邮箱</string>
<string name="verification_hint">请输入验证码</string>
<string name="get_code">获取验证码</string>
<string name="password_hint">请输入密码</string>
<string name="password_rule">密码需为6–12位且包含字母和数字（不能为纯数字）</string>
<string name="submit">提交</string>
<string name="code_sent">验证码已发送</string>
<string name="password_reset_success">密码重置成功！</string>
<string name="invalid_email">邮箱格式不正确</string>

### Data:
Get code use interface "/auth/v1/register/send-code" .
Submit use interface "/auth/v1/password/reset" .
