package com.mieson656.advancedreboot

import android.os.Build

class CapabilityDetector {
    fun detect(device: DeviceInfo, shizukuAvailable: Boolean): List<Capability> {
        return RebootOperation.entries.map { operation ->
            when (operation) {
                RebootOperation.REBOOT -> privileged("reboot", operation, shizukuAvailable)
                RebootOperation.RECOVERY -> privileged("recovery", operation, shizukuAvailable)
                RebootOperation.BOOTLOADER -> privileged("bootloader", operation, shizukuAvailable)
                RebootOperation.SHUTDOWN -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        Capability(operation, CapabilityState.AVAILABLE, "Доступен штатный системный запрос.", "Android")
                    else
                        Capability(operation, CapabilityState.UNSUPPORTED, "Требуется Android 9 или новее.")
                }
                RebootOperation.DOWNLOAD -> Capability(
                    operation,
                    CapabilityState.UNKNOWN,
                    "Универсальная команда не определена; нужен подтверждённый способ для конкретной модели."
                )
            }
        }
    }

    private fun privileged(kind: String, operation: RebootOperation, shizukuAvailable: Boolean): Capability {
        return if (shizukuAvailable) {
            Capability(operation, CapabilityState.NEEDS_SHIZUKU, "Операция требует привилегированного провайдера.", "Shizuku", adbCommand = command(kind))
        } else {
            Capability(operation, CapabilityState.NEEDS_ADB, "Встроенный ADB-провайдер не подключён; используйте ПК.", "ADB", command(kind))
        }
    }

    private fun command(kind: String): String = when (kind) {
        "reboot" -> "adb reboot"
        "recovery" -> "adb reboot recovery"
        "bootloader" -> "adb reboot bootloader"
        else -> ""
    }
}
