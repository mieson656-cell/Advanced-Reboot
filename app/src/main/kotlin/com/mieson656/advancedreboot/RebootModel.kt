package com.mieson656.advancedreboot

enum class RebootOperation(val title: String, val description: String) {
    REBOOT("Обычная перезагрузка", "Перезагрузка устройства."),
    RECOVERY("Recovery", "Переход в recovery."),
    DOWNLOAD("Download Mode", "OEM-режим загрузки."),
    BOOTLOADER("Bootloader / Fastboot", "Переход в bootloader/fastboot."),
    SHUTDOWN("Выключение", "Штатный запрос выключения устройства.")
}

enum class CapabilityState {
    AVAILABLE,
    NEEDS_SHIZUKU,
    NEEDS_ADB,
    UNSUPPORTED,
    UNKNOWN
}

data class Capability(
    val operation: RebootOperation,
    val state: CapabilityState,
    val reason: String,
    val provider: String? = null,
    val adbCommand: String? = null
)
