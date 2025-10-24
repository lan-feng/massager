Build a "Profile / Settings" screen for an app named ComfyTemp using Kotlin (Jetpack Compose) or Flutter.

### Layout requirements:
- The top section shows:
  - A welcome text: “Hi, I'm your smart customer service”
  - An avatar (circle image) for the user (default cartoon style)
  - Username below (e.g., “Geoffrey”)
- Below that, display a list of setting options, each with an icon, label, and optional value or arrow.

### Setting items (in order):
1. Personal Information → navigate to personal info screen
  - Icon: user outline
2. Account and Security → navigate to account settings
  - Icon: shield
3. Temperature Unit → toggle between °C and °F
  - Icon: temperature symbol
  - Current selection displayed at right side (default °F)
4. Clear Cache → show current cache size (e.g., “8.64MB”)
  - Icon: trash bin
  - Tap to clear cache and show a toast “Cache cleared successfully”
5. Browsing History → navigate to browsing history page
  - Icon: eye
6. My Favorites → navigate to favorites page
  - Icon: heart
7. About → navigate to about page
  - Icon: info circle

### Bottom Navigation Bar:
- 4 tabs with icons and labels:
  1. Home
  2. Manual / Guide
  3. Device
  4. Profile (this screen, highlighted in red)

### Design requirements:
- Clean, minimalist layout with soft shadows and rounded corners.
- Use Material 3 components.
- Icons should match labels visually (use Material Icons if Kotlin, or Flutter's Icons.*).
- Use light background color (#F8F8F8) and white cards for each list item.
- Add ripple effect on tap.
- Support dark mode automatically.

### Behavior:
- Support localization (i18n) for all labels.
- Pull data from mock user object:
  {
  name: "Geoffrey",
  avatarUrl: "",
  cacheSize: "8.64MB",
  tempUnit: "°F"
  }
- Save temperature unit selection and cache size to local preferences.
- Animate avatar slightly on tap (e.g., bounce or rotate).
- Include toast messages for Clear Cache and Temperature Unit changes.

### Navigation:
- Home  screen navigate to setting screen.
- When user taps an item, navigate to its corresponding screen.
- Use NavController (Jetpack Compose) or Navigator (Flutter).
