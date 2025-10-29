Home Screen Optimization


#### Device Section
- Section title: “Common Device” (smaller font, gray #777).
- Display one or more **Device Cards**:
    - Rounded white card with shadow (elevation 3dp)
    - Padding around 12–16dp
    - Device image (rounded icon) on left
    - Device name (e.g., “SmartPulse TENS Unit”) as title
    - Subtext: “SmartPulse TENS Device”
    - Status indicator (e.g., “Online” in green or gray dot)
    - Tap → navigate to Device Control Page
- If no devices → show empty state illustration (“No devices connected yet”) + “Add Device” button.

### Functional Behavior
- Device list should be dynamically loaded (mock data ok).
- "+" button opens device pairing or scan page.
- Maintain app-wide theme consistency (same fonts, color tokens, and icons).
- Use Compose/Flutter `LazyColumn` for scrollable list.
- Responsive on both phone and tablet.

### Localization & Accessibility
- All UI text uses string resources (for i18n).
- Support both English and Simplified Chinese:
    - “Welcome to ComfyTemp” → “欢迎使用 MASSAGER”
    - “Common Device” → “常用设备”
    - “Add Device” → “添加设备”
- Ensure readable contrast and accessible touch targets.

### Bonus Enhancements (Optional)
- Animated card entry (fade-up on appear).
- “Online” status pulse animation (green glow).
- Smooth color transition when switching tabs.
- Add pull-to-refresh for device list.
- Integrate light haptic feedback when tapping cards or buttons.

### Design Keywords for AI Visual Consistency
“modern wellness app UI”, “soft minimalist healthtech interface”,  
“white background, rounded device cards, warm light theme”,  
“elevated material design, friendly icons, red accent color (#E54335)”,  
“airy spacing, calm atmosphere, mobile-first design”.

### Data:
DeviceList ：http://192.168.2.110:9100/iot/api/device/v1/listByType ,deviceType default 10-19.
deviceType ：this app Reserve deviceType 10-19,res config deviceType.


