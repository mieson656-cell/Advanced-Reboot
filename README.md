# Advanced Reboot

Android utility for diagnosing device reboot/power capabilities without pretending that unsupported operations are available.

## Current architecture

- Device/OEM/Android/shell detection.
- Capability model with explicit states.
- Normal Android shutdown request.
- Shizuku integration through a dedicated User Service for generic reboot, recovery and bootloader commands.
- ADB fallback instructions for PC command line.
- Download Mode stays unknown until a model-specific method is confirmed.
- JVM tests for capability selection.
- GitHub Actions builds the debug APK and uploads it as an artifact.

## ADB fallback

The app shows commands that are intended for the PC's Android platform-tools terminal:

`adb reboot`

`adb reboot recovery`

`adb reboot bootloader`

ADB is a PC-side transport here; the app does not pretend to control the computer's terminal.

## Shizuku

Shizuku must be installed and authorized by the user. On supported setups, Advanced Reboot starts its own Shizuku User Service and asks that service to execute the selected reboot command.

Shizuku permissions are never assumed. If authorization is unavailable, the UI falls back to ADB instructions.

## Download Mode

There is intentionally no universal Download Mode command in this project. OEM/model-specific behavior will only be added after it is confirmed for the target device family.

## Build

GitHub Actions runs JVM tests and produces a debug APK artifact from the repository.
