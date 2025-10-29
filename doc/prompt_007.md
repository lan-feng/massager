Build a "Personal Information" screen for the ComfyTemp app using Kotlin (Jetpack Compose) or Flutter.

### Layout requirements:
- Page title: “Personal Information”
- Top-left: Back button (←)
- Background color: light gray (#F8F8F8)
- Use white rounded list items with subtle dividers.
- Two list rows:
  1. Avatar
     - Left label: “Avatar”
     - Right side: current avatar image (round, 48dp) + right arrow icon (chevron)
     - Tap opens image picker (camera or gallery) to change avatar.
     - After selecting, show preview and save the new avatar.
  2. Name
     - Left label: “Name”
     - Right side: current username (e.g., “Geoffrey”) + right arrow icon.
     - Tap to open a dialog or new page to edit name.
     - Support input validation: must be 2–20 characters, letters only.
     - After saving, update display immediately.

### Behavior requirements:
- All data loaded from a mock UserProfile object:
  {
    "avatarUrl": "https://example.com/avatar.png",
    "name": "Geoffrey"
  }
- When avatar or name changes, save updates to local SharedPreferences (or ViewModel state).
- Display a success toast when updates are saved.
- Support light/dark theme automatically.
- Support internationalization (English + Simplified Chinese):
  - res/values/strings.xml
  - res/values-zh/strings.xml
  - All labels (“Avatar”, “Name”, “Personal Information”, “Save”, “Cancel”, “Change Avatar”, “Enter your name”) should come from string resources.

### Example string resources:
<string name="personal_info_title">Personal Information</string>
<string name="avatar_label">Avatar</string>
<string name="name_label">Name</string>
<string name="change_avatar">Change Avatar</string>
<string name="edit_name">Edit Name</string>
<string name="save">Save</string>
<string name="cancel">Cancel</string>
<string name="toast_profile_updated">Profile updated successfully</string>

### Optional enhancements:
- Animate avatar image when tapped (slight scale-up).
- Add shimmer loading effect while user data loads.
- Include image compression before uploading avatar.
- For Flutter, use “cached_network_image” and “image_picker” packages.
- For Kotlin, use “Coil” for avatar loading and “ActivityResultContracts.GetContent” for image picker.


Implement a modal dialog for editing user name:
- Title: “Edit Name”
- TextField prefilled with current name.
- Buttons: “Cancel” and “Save”.
- Disable save button if input is invalid.


Implement an avatar change dialog:
- Options: “Take Photo” and “Choose from Gallery”.
- Use system image picker or camera intent.
- Compress image before saving.
- Update avatar preview immediately.
