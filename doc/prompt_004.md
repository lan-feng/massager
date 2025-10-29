Create a modern "Profile" screen for the ComfyTemp app using Kotlin Jetpack Compose (or Flutter).

### Design Goals
- Maintain a soft, minimal wellness/health-tech aesthetic.
- White background (#FAFAFA) with subtle gray shadows.
- Rounded cards and calm spacing.
- Accent color: ComfyTemp Red (#E54335).
- Smooth transitions, adaptive layout for mobile devices.
- Use Material 3 components.

### Layout Sections

#### Top Header Section
- Background: soft gradient (white → light gray).
- Left-aligned greeting text: “Hi, I'm your smart customer service”.
  - Font size: 16sp, color #555.
- Small chatbot icon (rounded, gradient background with subtle glow).
- Center: Avatar + Username block.
  - Circular avatar (default cartoon style).
  - Username below, e.g. “Geoffrey”.
  - Add subtle entry animation (fade-in or slide-up).

#### Settings List Section
- Use a rounded white card container with shadow and padding.
- Each list item includes:
  - Icon (Material Icons / Fluent Icons).
  - Label text.
  - Optional right-side value or unit.
  - Chevron (>) icon for navigation.
  - Tap animation (ripple).
- List items (in order):
  1. Personal Information → navigate to `PersonalInfoScreen`
    - Icon: person outline.
  2. Account and Security → navigate to `AccountSecurityScreen`
    - Icon: shield outline.
  3. Temperature Unit → toggle between “°C” and “°F”.
    - Icon: thermostat or thermometer.
    - Value on the right (“°F” by default).
  4. Clear Cache → show current cache size (e.g. “8.28MB”)
    - Icon: trash can.
    - Tap → clear cache → show toast “Cache cleared successfully”.
  5. Browsing History → navigate to browsing history.
    - Icon: eye outline.
  6. My Favorites → navigate to favorites page.
    - Icon: heart outline.
  7. About → navigate to about page.
    - Icon: info outline.

#### Bottom Navigation Bar
- Tabs (icons + labels):
  - Home
  - Manual
  - Device
  - Profile (active, red highlight)
- Height ~56dp, soft white background, active icon colored red (#E54335).

---

### ⚙️ Functional Logic
- Store user info (name, avatar, temperature unit, cache size) in ViewModel or state.
- Cache size fetched from local storage on init.
- Temperature unit toggle saves to SharedPreferences (or DataStore).
- “Clear Cache” triggers coroutine cleanup + toast message.
- Each navigation item links to respective route via NavController (Compose) or Navigator (Flutter).
- Support theme switching and dark mode automatically.

---

### 🌐 Internationalization
Use resource files for all text (English & Simplified Chinese).

Example:
<string name="profile_title">Profile</string>
<string name="hi_smart_assistant">Hi, I'm your smart customer service</string>
<string name="personal_info">Personal Information</string>
<string name="account_security">Account and Security</string>
<string name="temperature_unit">Temperature Unit</string>
<string name="clear_cache">Clear Cache</string>
<string name="browsing_history">Browsing History</string>
<string name="my_favorites">My Favorites</string>
<string name="about">About</string>
<string name="cache_cleared">Cache cleared successfully</string>
<string name="unit_celsius">°C</string>
<string name="unit_fahrenheit">°F</string>


Chinese (values-zh/strings.xml):

<string name="profile_title">我的</string>
<string name="hi_smart_assistant">您好，我是您的智能客服</string>
<string name="personal_info">个人信息</string>
<string name="account_security">账号与安全</string>
<string name="temperature_unit">温度单位</string>
<string name="clear_cache">清除缓存</string>
<string name="browsing_history">浏览记录</string>
<string name="my_favorites">我的收藏</string>
<string name="about">关于</string>
<string name="cache_cleared">缓存清理完成</string>
<string name="unit_celsius">°C</string>
<string name="unit_fahrenheit">°F</string>

### Navigation:
- Home  screen navigate to setting screen.
- When user taps an item, navigate to its corresponding screen.
- Use NavController (Jetpack Compose) or Navigator (Flutter).
