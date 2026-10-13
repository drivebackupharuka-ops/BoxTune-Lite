# Optional, reversible Android TV box ADB optimizations

**Only for your own device, using a computer you control.** This is not executed by BoxTune Lite. Avoid using wireless ADB on shared networks. Turn USB debugging off afterward.

## Connect and identify

1. On the box, enable Developer Options and USB debugging if available.
2. Install the official Android platform-tools on your computer.
3. Plug in a supported USB data cable or use a secure, authorized ADB connection and accept the prompt on the TV box.

```sh
adb devices
adb shell getprop ro.hardware
adb shell getprop ro.board.platform
adb shell getprop ro.build.version.release
adb shell dumpsys meminfo
```

## Reduce Android UI animations

This can improve **perceived menu responsiveness** but generally does not increase Roblox FPS. Use 0.5 for shorter animations:

```sh
adb shell settings put global window_animation_scale 0.5
adb shell settings put global transition_animation_scale 0.5
adb shell settings put global animator_duration_scale 0.5
```

To restore defaults:

```sh
adb shell settings put global window_animation_scale 1.0
adb shell settings put global transition_animation_scale 1.0
adb shell settings put global animator_duration_scale 1.0
```

## Optional logical screen resolution test

**Use with caution:** this changes the logical framebuffer for some Android firmware; on an unsupported box the UI may look wrong. Do not attempt if you lack a way to restore through ADB.

```sh
adb shell wm size
adb shell wm size 1280x720
```

Restore the original behavior:

```sh
adb shell wm size reset
```

It may improve rendering load on some devices and do nothing on others. Lowering HDMI output alone is not necessarily the same as lowering the game's rendering resolution.

## Avoid

- Disabling unknown system packages or Google Play Services.
- Random `su`, kernel overclocking, thermal-throttle modifications, bootloader unlocking, or unfamiliar firmware images.
- Task-killer apps claiming to add RAM or doubling available RAM through swap on slow flash memory.

Most reliable improvement on a 2 GB / 8 GB box: free internal storage, reboot before gaming, set Roblox to minimum graphics, and run Discord voice on a separate phone. ADB does **not** enable mouse controls inside apps that do not support them.
