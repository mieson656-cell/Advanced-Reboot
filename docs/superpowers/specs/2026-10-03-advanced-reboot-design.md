# Advanced Reboot Architecture Design

## Goal

Turn Advanced Reboot into a conservative Android reboot/power utility that diagnoses the current device and exposes only operations whose support and execution path can be justified.

## Scope

The app must detect manufacturer, model, Android/API level, OEM shell/version when observable, and provider availability. Each operation reports an explicit state rather than pretending unsupported operations work.

Supported operation families in this phase:
- Normal reboot
- Shutdown
- Recovery
- Bootloader/Fastboot
- Download Mode when an OEM/model-specific method is known

Providers:
1. Normal Android/system APIs where applicable.
2. Shizuku for privileged on-device shell/API execution when the user has authorized Shizuku and the specific operation is actually permitted.
3. ADB fallback instructions for PC command-line execution.
4. Root provider boundary reserved for later implementation; no fake root path.

ADB fallback must show the exact command for operations with a documented generic command, such as `adb reboot`, `adb reboot recovery`, and `adb reboot bootloader`. Download Mode must never receive a fabricated universal ADB command; it requires device-specific knowledge.

## Architecture

The UI consumes a capability model rather than deciding support itself.

- `DeviceInfo`: immutable device/OEM/API facts.
- `CapabilityDetector`: converts device facts and provider state into operation capabilities.
- `RebootOperation`: stable operation identifiers and display metadata.
- `RebootProvider`: interface for executable providers.
- `SystemProvider`: normal Android APIs available to the app.
- `ShizukuProvider`: Shizuku-backed privileged execution.
- `AdbInstructionsProvider`: non-executing fallback that returns PC instructions/commands.
- `RootProvider`: reserved boundary; no implementation until a safe root path is specified.

The UI displays:
- support state;
- reason;
- provider/path;
- an action button only when the path is executable;
- an ADB instruction button when ADB is a valid fallback.

## Safety and correctness

- Never claim support solely because an operation exists in the UI.
- Never assume an OEM-specific mode has a universal command.
- Never attempt privilege escalation without an authorized provider.
- Failure messages must distinguish unsupported hardware/software, missing privilege, and unknown capability.
- Reboot actions must be explicit user actions, not automatic.
- Destructive power operations must require an explicit confirmation.

## Testing

Pure capability/provider-selection logic must have JVM unit tests. Android integration points should be isolated behind small interfaces so they can be tested without requiring a physical device. Before APK delivery, the project must pass the available Gradle test/build checks and the resulting APK must be produced from the verified repository state.
