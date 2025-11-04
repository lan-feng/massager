"Device Control" Optimization

### Screen Title:
- Use Device name

### Device Display Area
- "+": reconnect to device , if device is disconnected.
- charge level: use device heartbeat info from EMS
- Mute button: send command to EMS

### Body Zone Tabs
A horizontal segmented tab control with 5 zones:
0：shldr，1：waist，2：legs，3：arms，4：joint，5：body
Each represents a treatment area; 
####  Mode Selection Grid
MODE-->0~7。0：massage，1：knead，2：scraping，3：pressure，4：acupoint，5：cupping。6：activate，7：shape
####  Intensity Control
Center number showing current intensity (0–19)
####  Timer & Action Section
- START → send command to EMS,the command is comprise of treatment area,mode,level and timer.
- STOP → stops command to EMS,the command is set level to zero.

### Functional Behavior
use device heartbeat info from EMS to show battery charge level,treatment area,mode,level and timer.
