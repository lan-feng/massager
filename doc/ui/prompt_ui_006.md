Optimize the Time Control component UI for a therapy device control screen.

Component description:
- Use a large rounded card container with a soft light background (iOS-style).
- The top section is split into two parts:
  - Left: a quick preset area with a timer icon and text "30 min", vertically aligned.
  - Right: a large remaining time display in HH:MM:SS format (e.g. 00:59:27).
- The time display should be the main visual focus:
  - Large font size
  - Semi-bold or bold
  - Brand primary color
  - Monospaced digits for stability.

- The bottom section contains three action buttons arranged horizontally:
  1. A prominent red "Stop" button with a stop-square icon on the left.
     - This is the primary action.
     - Large rounded corners.
  2. A secondary "- 5 min" button:
     - Minus icon
     - Light orange/yellow background
     - Used to decrease remaining time.
  3. A secondary "+ 5 min" button:
     - Plus icon
     - Light green background
     - Used to increase remaining time.

Design constraints:
- Do not change business logic.
- Only optimize layout, spacing, colors, and visual hierarchy.
- Maintain clear priority: Time display > Stop > +/- time buttons.
- The component should feel clean, calm, and medical-grade.

Platform target:
- Mobile (Android / iOS)
- Suitable for Jetpack Compose or SwiftUI implementation.
