package com.mieson656.advancedreboot

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator

class MainActivity : ComponentActivity() {

    private data class Mode(
        val title: String,
        val description: String,
        val state: String,
        val action: (() -> Unit)? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val progress = findViewById<LinearProgressIndicator>(R.id.progress)
        val subtitle = findViewById<TextView>(R.id.subtitle)
        val deviceName = findViewById<TextView>(R.id.deviceName)
        val deviceDetails = findViewById<TextView>(R.id.deviceDetails)
        val container = findViewById<LinearLayout>(R.id.modesContainer)

        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        val androidVersion = Build.VERSION.RELEASE
        val api = Build.VERSION.SDK_INT
        val shell = detectShell()

        deviceName.text = "$manufacturer $model"
        deviceDetails.text = "Android $androidVersion (API $api)\nСистемная оболочка: $shell"

        // The first version is deliberately conservative: ordinary apps cannot
        // grant themselves privileged reboot permissions. Unsupported actions are
        // shown instead of pretending that they work.
        val modes = listOf(
            Mode(
                "Обычная перезагрузка",
                "Перезагрузка Android в обычный режим.",
                "Требуются дополнительные права"
            ),
            Mode(
                "Выключение",
                "Запрос штатного выключения устройства.",
                if (canRequestShutdown()) "Доступно через системный запрос" else "Не удалось определить"
            ) {
                requestShutdown()
            },
            Mode(
                "Recovery",
                "Загрузка устройства в recovery-раздел.",
                "Требуются дополнительные права"
            ),
            Mode(
                "Download Mode",
                "Режим загрузки, используемый некоторыми производителями, включая Samsung.",
                "Требуются дополнительные права / зависит от устройства"
            ),
            Mode(
                "Bootloader / Fastboot",
                "Загрузка bootloader/fastboot, если такой режим реализован производителем.",
                "Не удалось безопасно подтвердить"
            )
        )

        modes.forEach { addMode(container, it) }

        progress.visibility = View.GONE
        subtitle.text = "Проверка завершена"
    }

    private fun detectShell(): String {
        val props = listOf(
            "ro.build.version.oneui",
            "ro.miui.ui.version.name",
            "ro.build.version.hyperos",
            "ro.oxygen.version",
            "ro.build.ui.version"
        )

        for (key in props) {
            val value = getSystemProperty(key)
            if (!value.isNullOrBlank()) {
                return when {
                    key.contains("oneui", true) -> "One UI $value"
                    key.contains("miui", true) -> "MIUI $value"
                    key.contains("hyperos", true) -> "HyperOS $value"
                    key.contains("oxygen", true) -> "OxygenOS $value"
                    else -> value
                }
            }
        }

        return when {
            Build.MANUFACTURER.equals("samsung", true) -> "One UI (версия не подтверждена)"
            Build.MANUFACTURER.equals("xiaomi", true) -> "MIUI/HyperOS (версия не подтверждена)"
            else -> "Не удалось определить"
        }
    }

    private fun getSystemProperty(name: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", name))
            process.inputStream.bufferedReader().use { it.readLine()?.trim() }
        } catch (_: Exception) {
            null
        }
    }

    private fun canRequestShutdown(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }

    private fun requestShutdown() {
        try {
            val intent = Intent(Intent.ACTION_REQUEST_SHUTDOWN).apply {
                putExtra(Intent.EXTRA_KEY_CONFIRM, true)
            }
            startActivity(intent)
        } catch (_: Exception) {
            // Some OEMs do not expose this activity. No privileged fallback is used.
        }
    }

    private fun addMode(container: LinearLayout, mode: Mode) {
        val card = MaterialCardView(this).apply {
            radius = 20f
            strokeWidth = 1
            setContentPadding(18, 16, 18, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = TextView(this).apply {
            text = mode.title
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val desc = TextView(this).apply {
            text = mode.description
            textSize = 14f
            setPadding(0, 6, 0, 8)
        }

        val status = TextView(this).apply {
            text = mode.state
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        }

        column.addView(title)
        column.addView(desc)
        column.addView(status)

        if (mode.action != null && !mode.state.startsWith("Требуются")) {
            val button = MaterialButton(this).apply {
                text = "Выполнить"
                setOnClickListener { mode.action.invoke() }
            }
            column.addView(button)
        }

        card.addView(column)
        container.addView(card)
    }
}
