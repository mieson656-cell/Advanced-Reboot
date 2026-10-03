package com.mieson656.advancedreboot

import android.os.Build

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val shell: String
) {
    companion object {
        fun read(propertyReader: (String) -> String? = ::readProperty): DeviceInfo {
            return DeviceInfo(
                Build.MANUFACTURER,
                Build.MODEL,
                Build.VERSION.RELEASE,
                Build.VERSION.SDK_INT,
                ShellDetector.detect(Build.MANUFACTURER, propertyReader)
            )
        }

        private fun readProperty(name: String): String? = try {
            Runtime.getRuntime().exec(arrayOf("getprop", name))
                .inputStream.bufferedReader().use { it.readLine()?.trim() }
        } catch (_: Exception) { null }
    }
}

object ShellDetector {
    fun detect(manufacturer: String, propertyReader: (String) -> String?): String {
        val props = listOf(
            "ro.build.version.oneui" to "One UI",
            "ro.miui.ui.version.name" to "MIUI",
            "ro.build.version.hyperos" to "HyperOS",
            "ro.oxygen.version" to "OxygenOS",
            "ro.build.ui.version" to "System UI"
        )
        props.firstNotNullOfOrNull { (key, label) ->
            propertyReader(key)?.takeIf { it.isNotBlank() }?.let { "$label $it" }
        }?.let { return it }
        return when {
            manufacturer.equals("samsung", true) -> "One UI (версия не подтверждена)"
            manufacturer.equals("xiaomi", true) -> "MIUI/HyperOS (версия не подтверждена)"
            else -> "Не удалось определить"
        }
    }
}
