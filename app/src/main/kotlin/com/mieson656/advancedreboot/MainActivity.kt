package com.mieson656.advancedreboot

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator

class MainActivity : ComponentActivity() {
    private data class Mode(val title: String, val description: String, val state: String, val action: (() -> Unit)? = null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val progress = findViewById<LinearProgressIndicator>(R.id.progress)
        val subtitle = findViewById<TextView>(R.id.subtitle)
        val deviceName = findViewById<TextView>(R.id.deviceName)
        val deviceDetails = findViewById<TextView>(R.id.deviceDetails)
        val container = findViewById<LinearLayout>(R.id.modesContainer)

        deviceName.text = "\${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} \${Build.MODEL}"
        deviceDetails.text = "Android \${Build.VERSION.RELEASE} (API \${Build.VERSION.SDK_INT})\\nСистемная оболочка: \${detectShell()}"

        listOf(
            Mode("Обычная перезагрузка", "Перезагрузка через привилегированный системный механизм.", "ADB / Shizuku / root") { showAdbInstructions("reboot") },
            Mode("Recovery", "Переход в recovery. Обычно: adb reboot recovery.", "ADB / Shizuku / root") { showAdbInstructions("recovery") },
            Mode("Download Mode", "OEM-режим загрузки. Команда зависит от производителя и модели.", "ADB: зависит от устройства • Shizuku: не гарантируется") { showAdbInstructions("download") },
            Mode("Bootloader / Fastboot", "Загрузка bootloader/fastboot, если поддерживается устройством.", "ADB / Shizuku / root") { showAdbInstructions("bootloader") },
            Mode("Выключение", "Запрос штатного выключения устройства.", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) "Системный запрос" else "Не удалось определить") { requestShutdown() }
        ).forEach { addMode(container, it) }

        progress.visibility = View.GONE
        subtitle.text = "Проверка завершена"
    }

    private fun detectShell(): String {
        val props = listOf(
            "ro.build.version.oneui" to "One UI",
            "ro.miui.ui.version.name" to "MIUI",
            "ro.build.version.hyperos" to "HyperOS",
            "ro.oxygen.version" to "OxygenOS",
            "ro.build.ui.version" to "System UI"
        )
        for ((key, label) in props) {
            val value = getSystemProperty(key)
            if (!value.isNullOrBlank()) return "\$label \$value"
        }
        return when {
            Build.MANUFACTURER.equals("samsung", true) -> "One UI (версия не подтверждена)"
            Build.MANUFACTURER.equals("xiaomi", true) -> "MIUI/HyperOS (версия не подтверждена)"
            else -> "Не удалось определить"
        }
    }

    private fun getSystemProperty(name: String): String? = try {
        Runtime.getRuntime().exec(arrayOf("getprop", name)).inputStream.bufferedReader().use { it.readLine()?.trim() }
    } catch (_: Exception) { null }

    private fun requestShutdown() {
        try {
            startActivity(Intent(Intent.ACTION_REQUEST_SHUTDOWN).apply {
                putExtra(Intent.EXTRA_KEY_CONFIRM, true)
            })
        } catch (_: Exception) {
            showAdbInstructions("shutdown")
        }
    }

    private fun showAdbInstructions(mode: String) {
        val message = when (mode) {
            "reboot" -> "1. Включите «Параметры разработчика» и «Отладка по USB».\\n\\n2. Подключите телефон к компьютеру.\\n\\n3. Разрешите отладку на телефоне.\\n\\n4. Откройте CMD/Terminal в папке Android platform-tools.\\n\\n5. Выполните:\\n\\nadb reboot\\n\\nПока ADB-провайдер не подключён, приложение показывает инструкцию, а не притворяется, что выполняет команду."
            "recovery" -> "1. Включите отладку по USB.\\n2. Подключите телефон к компьютеру.\\n3. Подтвердите RSA-запрос.\\n4. Выполните:\\n\\nadb reboot recovery"
            "bootloader" -> "1. Включите отладку по USB.\\n2. Подключите телефон к компьютеру.\\n3. Подтвердите RSA-запрос.\\n4. Выполните:\\n\\nadb reboot bootloader\\n\\nЕсли устройство не поддерживает этот переход, команда может не сработать."
            "download" -> "1. Включите отладку по USB и подключите телефон.\\n2. Подтвердите RSA-запрос.\\n3. Для Download Mode используйте только способ, подтверждённый для вашей модели.\\n\\nУ производителей нет одной универсальной ADB-команды для этого режима, поэтому приложение позже будет выбирать способ по базе возможностей устройства."
            else -> "1. Включите отладку по USB.\\n2. Подключите телефон к компьютеру.\\n3. Разрешите USB debugging.\\n4. Выполните подходящую ADB-команду для вашей модели."
        }
        MaterialAlertDialogBuilder(this).setTitle("Нужен ADB").setMessage(message).setPositiveButton("Понятно", null).show()
    }

    private fun addMode(container: LinearLayout, mode: Mode) {
        val card = MaterialCardView(this).apply {
            radius = 20f
            strokeWidth = 1
            setContentPadding(18, 16, 18, 16)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 }
        }
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(TextView(this).apply {
            text = mode.title; textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        column.addView(TextView(this).apply {
            text = mode.description; textSize = 14f; setPadding(0, 6, 0, 8)
        })
        column.addView(TextView(this).apply {
            text = "Способ: \${mode.state}"; textSize = 13f
            setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        })
        if (mode.action != null) column.addView(MaterialButton(this).apply {
            text = "Как выполнить"; setOnClickListener { mode.action.invoke() }
        })
        card.addView(column)
        container.addView(card)
    }
}
