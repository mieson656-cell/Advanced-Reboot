package com.mieson656.advancedreboot

class CapabilityDetector {
    fun detect(
        device: DeviceInfo,
        shizukuAvailable: Boolean,
        deviceOwnerAvailable: Boolean = false
    ): List<Capability> {
        return RebootOperation.entries.map { operation ->
            when (operation) {
                RebootOperation.REBOOT -> {
                    if (deviceOwnerAvailable) {
                        Capability(
                            operation,
                            CapabilityState.AVAILABLE,
                            "Приложение является владельцем устройства и может использовать системный DevicePolicyManager.",
                            "Device Owner",
                            adbCommand = adbCommand("reboot")
                        )
                    } else {
                        privileged("reboot", operation, shizukuAvailable)
                    }
                }
                RebootOperation.RECOVERY -> privileged("recovery", operation, shizukuAvailable)
                RebootOperation.BOOTLOADER -> privileged("bootloader", operation, shizukuAvailable)
                RebootOperation.SHUTDOWN -> privileged("shutdown", operation, shizukuAvailable)
                RebootOperation.DOWNLOAD -> Capability(
                    operation,
                    CapabilityState.UNKNOWN,
                    "Нет универсального системного API или ADB-команды. Нужен подтверждённый способ для конкретного OEM/модели.",
                    adbCommand = null,
                    providerCommand = null
                )
            }
        }
    }

    private fun privileged(
        kind: String,
        operation: RebootOperation,
        shizukuAvailable: Boolean
    ): Capability {
        val providerCommand = providerCommand(kind)
        return if (shizukuAvailable) {
            Capability(
                operation,
                CapabilityState.AVAILABLE,
                "Shizuku авторизован. Действие будет выполнено через привилегированный User Service.",
                "Shizuku",
                adbCommand = adbCommand(kind),
                providerCommand = providerCommand
            )
        } else {
            Capability(
                operation,
                CapabilityState.NEEDS_ADB,
                "Прямой системный способ недоступен. Можно использовать Shizuku или выполнить ADB-команду с ПК.",
                "ADB",
                adbCommand = adbCommand(kind),
                providerCommand = providerCommand
            )
        }
    }

    private fun adbCommand(kind: String): String = when (kind) {
        "reboot" -> "adb reboot"
        "recovery" -> "adb reboot recovery"
        "bootloader" -> "adb reboot bootloader"
        "shutdown" -> "adb shell reboot -p"
        else -> ""
    }

    private fun providerCommand(kind: String): String = when (kind) {
        "reboot" -> "reboot"
        "recovery" -> "reboot recovery"
        "bootloader" -> "reboot bootloader"
        "shutdown" -> "reboot -p"
        else -> ""
    }
}
