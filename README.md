# Advanced Reboot

Advanced Reboot is an Android utility for detecting and exposing reboot/power operations without pretending that unsupported operations are available.

## What it supports

- Normal reboot.
- Recovery.
- Bootloader / Fastboot.
- Shutdown.
- Device and Android version information.
- Basic OEM/system-shell detection.
- Shizuku User Service execution for privileged operations.
- ADB fallback instructions for a PC.
- Explicit capability states instead of fake buttons.

## Capability states

The UI distinguishes:

- **Доступно** — the selected provider is currently authorized.
- **Нужен Shizuku** — an operation needs Shizuku authorization.
- **Нужен Shizuku или ADB на ПК** — the app cannot execute it directly with current privileges, but a generic ADB command is known.
- **Не поддерживается** — the operation is confirmed unsupported.
- **Не удалось определить** — the app does not have enough evidence to claim support.

The project intentionally prefers an honest unknown state over guessing.

## Shizuku

Shizuku must be installed and authorized by the user.

When authorized, Advanced Reboot starts its own Shizuku User Service and sends only the selected, hard-coded reboot command to that service. The app does not implement privilege escalation itself and never assumes that Shizuku is available.

The User Service connection is treated as a separate runtime state: permission can exist while the service is still unavailable.

## ADB fallback

ADB is a PC-side fallback. The app does not pretend that it can control the computer's terminal.

Known generic commands:

`adb reboot`

`adb reboot recovery`

`adb reboot bootloader`

`adb shell reboot -p`

Before using ADB, enable USB debugging, connect the device, accept the RSA prompt, and use a matching Android platform-tools installation.

## Download Mode

There is intentionally **no universal Download Mode command** in this project.

Download Mode is OEM/model-specific. Until a method is confirmed for a particular device family, the UI shows the operation as **Не удалось определить** and does not expose a guessed command.

## Detection

The app reads the Android manufacturer, model, Android release/API level and selected system properties used to identify common shells such as One UI, MIUI, HyperOS and OxygenOS.

A shell version is only displayed when the corresponding property is present. Manufacturer-only fallback text is explicitly marked as unconfirmed.

## Testing and CI

GitHub Actions runs JVM unit tests, builds the debug APK, and uploads the APK as a workflow artifact.

The test suite covers:

- generic ADB fallback commands;
- Shizuku provider commands;
- Download Mode's no-guessing rule;
- One UI / MIUI property detection;
- unknown OEM shell handling.

## Build locally

Requirements:

- JDK 17
- Android SDK / platform tools
- Gradle 8.10

Run:

`gradle test`

Then:

`gradle assembleDebug`

The debug APK is produced at:

`app/build/outputs/apk/debug/app-debug.apk`
