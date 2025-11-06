Create an "About" screen for the Massager app using Kotlin Jetpack Compose (or Flutter).

### 🎯 Page Purpose
Display app information such as version, update check, user agreement, and privacy policy.

---

### 🏗 Layout Structure

#### 1️⃣ App Bar
- Title: “About”
- Left: Back arrow (←)
- Right: none
- Background: white (#FFFFFF)
- Font: Medium 18sp, color #222222

---

#### 2️⃣ App Logo Section
- Center the Massager logo (red “M” icon with gradient #E54335 → #D22020).
- Add subtle shadow below.
- Below the logo, display version text:
  - Example: “V1.6.0”
  - Font size: 14sp, color #777777
- Add small fade-in animation for logo on page load.

---

#### 3️⃣ Info List Section
Display a simple list of setting-style items (white cards with dividers).

Items:
1. **Check for update**
   - Icon: update/refresh outline (optional)
   - On click → simulate version check:
     - Show loading spinner for 1.5s
     - Show toast or dialog:
       - “Already up to date” if on latest version
       - “New version available! (v1.7.0)” if mock update found

2. **User agreement**
   - On click → navigate to `UserAgreementScreen`
   - Content loaded via WebView or markdown viewer.

3. **Privacy policy**
   - On click → navigate to `PrivacyPolicyScreen`
   - Also display via WebView.

Each list item:
- Height ~56dp
- Text aligned left
- Arrow icon (chevron) on right
- Ripple effect on tap
- Divider between items

---

#### 4️⃣ Footer Section
- Centered footer text:
  - “Copyright 2025  Massager All Rights Reserved”
  - Font size: 12sp, color #AAAAAA
  - Top margin: 48dp

---

### ⚙️ Functional Behavior

#### Version Check (Mock Logic)
- Define constant:
currentVersion = "1.0.0"
latestVersion = "1.0.0" // simulate current version

- When “Check for update” is clicked:
- If same → show toast “Already up to date”.
- If higher mock version → show alert dialog:
  - “New version available: V1.7.0”
  - Buttons: “Later” / “Update Now”
  - On “Update Now” → open Google play

#### User Agreement & Privacy Policy
- Load URLs (mock):
userAgreementUrl = "https://www.intestweb.com/service.html"
privacyPolicyUrl = "https://www.intestweb.com/privacy.html"

- Open via embedded WebView screen with title bar.

---

### 💡 UI Design
- Background: #FAFAFA
- Card background: white (#FFFFFF)
- Accent color: Massager Red (#E54335)
- Spacing:
- Logo top margin: 80dp
- List top margin: 40dp
- Typography:
- Title: 18sp semi-bold
- Subtext: 14sp gray (#777)
- Animation:
- Logo fade-in (300ms)
- Button ripple (Material default)
- Elevation: light shadows for cards (2dp)
- Use consistent rounded corners (12dp)

---

### 🌐 Internationalization
All text strings must come from resource files.

**English**
<string name="about_title">About</string>
<string name="check_update">Check for update</string>
<string name="user_agreement">User agreement</string>
<string name="privacy_policy">Privacy policy</string>
<string name="version_label">V1.6.0</string>
<string name="already_latest">Already up to date</string>
<string name="new_version_available">New version available!</string>
<string name="update_now">Update Now</string>
<string name="later">Later</string>
<string name="copyright">Copyright ©2025 深圳市泓加亿网络科技有限公司 All Rights Reserved</string>

**Simplified Chinese**
<string name="about_title">关于</string>
<string name="check_update">检查更新</string>
<string name="user_agreement">用户协议</string>
<string name="privacy_policy">隐私政策</string>
<string name="version_label">V1.6.0</string>
<string name="already_latest">已是最新版本</string>
<string name="new_version_available">发现新版本！</string>
<string name="update_now">立即更新</string>
<string name="later">稍后再说</string>
<string name="copyright">版权归 ©2025 深圳市泓加亿网络科技有限公司所有 </string>