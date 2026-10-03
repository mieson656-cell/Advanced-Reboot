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
            Capability(
                operation,
                CapabilityState.AVAILABLE,
                "Shizuku авторизован. Действие будет выполнено через привилегированный User Service.",
                "Shizuku",
                adbCommand = adbCommand(kind),
                providerCommand = providerCommand(kind)
            )
        } else {
            Capability(
                operation,
                CapabilityState.NEEDS_ADB,
                "Для выполнения нужен Shizuku или ADB на ПК.",
                "ADB",
                adbCommand = adbCommand(kind),
                providerCommand = providerCommand(kind)
            )
        }
    }

    private fun adbCommand(kind: String): String = when (kind) {
        "reboot" -> "adb reboot"
        "recovery" -> "adb reboot recovery"
        "bootloader" -> "adb reboot bootloader"
        else -> ""
    }

    private fun providerCommand(kind: String): String = when (kind) {
        "reboot" -> "reboot"
        "recovery" -> "reboot recovery"
        "bootloader" -> "reboot bootloader"
        else -> ""
    }
}
