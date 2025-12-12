You are a senior Android Jetpack Compose UI engineer.

Generate a reusable Jetpack Compose component named:

TimerDashboard

This component is the central circular countdown timer panel. It must match iOS visual behavior exactly. Only UI + state mapping, no timer business logic.


-------------------------
✅ Layout Structure
-------------------------

Horizontal Layout:

Left:
- Large circular progress ring (140dp)
Center:
- Inside the ring:
  - Row: "30 min  >" (clickable)
  - Button: "Start" or "stop"
Right:
- Vertical column:
  - +5 min button
  - -5 min button

-------------------------
✅ Circular Progress Ring
-------------------------

Max = 60 minutes

progress = displayMinutes / 60f

Background ring:
- primaryColor with 15% alpha
- stroke width: 12dp

Progress ring:
- Sweep gradient:
  primary → primary 60% alpha
- stroke width: 12dp
- Rounded stroke cap

-------------------------
✅ Running Glow Animation
-------------------------

Only when:
isRunning == true AND isInteractive == true

Shadow glow:
- radius: 8 → 12 → 8
- alpha: 0.7 → 1.0 → 0.7
- infinite loop

-------------------------
✅ Center Content
-------------------------

Top:
- Text: "30 min"
- Chevron icon ">"
- Click opens BottomSheet time selector

Bottom:
- Button text:
  if isRunning == true → "Stop"
  else → "Start"

Button color = primaryColor

-------------------------
✅ +5 / -5 Quick Buttons
-------------------------

+5 Button:
- Text: "+ 5 min"
- Foreground: Green
- Background: glassmorphism + green gradient
- Border: green 1.5dp

-5 Button:
- Text: "- 5 min"
- Foreground: Orange
- Background: glassmorphism + orange gradient
- Border: orange 1.5dp

Press animation:
Scale: 1.0 → 0.92 → 1.0

Running pulse:
Scale: 1.0 → 1.03 → 1.0 infinite

-------------------------
✅ BottomSheet Time Picker
-------------------------

Grid layout:

5   10  15  
20  25  30  
40  50  60  

Selected item:
- background: primaryColor
- text: white

Click item:
- onSetTimer(value)
- close BottomSheet immediately

-------------------------
✅ Output Requirements
-------------------------

- Use Material3
- Use ModalBottomSheet
- No fake countdown logic
- No preview
- Output only valid Compose Kotlin code
