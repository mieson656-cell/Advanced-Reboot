package com.mieson656.advancedreboot

import android.content.Intent
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
    private lateinit var device: DeviceInfo
    private lateinit var capabilities: List<Capability>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val progress = findViewById<LinearProgressIndicator>(R.id.progress)
        val subtitle = findViewById<TextView>(R.id.subtitle)
        val deviceName = findViewById<TextView>(R.id.deviceName)
        val deviceDetails = findViewById<TextView>(R.id.deviceDetails)
        val container = findViewById<LinearLayout>(R.id.modesContainer)

        device = DeviceInfo.read()
        capabilities = CapabilityDetector().detect(device, ShizukuBridge(this).isAvailable())

        deviceName.text = "${device.manufacturer.replaceFirstChar { it.uppercase() }} ${device.model}"
        deviceDetails.text = "Android ${device.androidVersion} (API ${device.apiLevel})\nСистемная оболочка: ${device.shell}"

        capabilities.forEach { addCapability(container, it) }
        progress.visibility = View.GONE
        subtitle.text = "Проверка завершена"
    }

    private fun addCapability(container: LinearLayout, capability: Capability) {
        val card = MaterialCardView(this).apply {
            radius = 20f
            strokeWidth = 1
            setContentPadding(18, 16, 18, 16)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 }
        }
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(TextView(this).apply {
            text = capability.operation.title
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        column.addView(TextView(this).apply {
            text = capability.operation.description
            textSize = 14f
            setPadding(0, 6, 0, 8)
        })
        column.addView(TextView(this).apply {
            text = "Статус: ${statusText(capability.state)}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        })
        column.addView(TextView(this).apply {
            text = capability.reason
            textSize = 13f
            setPadding(0, 5, 0, 8)
        })
        capability.adbCommand?.let {
            column.addView(MaterialButton(this).apply {
                text = "Показать ADB-команду"
                setOnClickListener { showAdbInstructions(capability) }
            })
        }
        if (capability.operation == RebootOperation.SHUTDOWN &&
            capability.state == CapabilityState.AVAILABLE) {
            column.addView(MaterialButton(this).apply {
                text = "Выключить"
                setOnClickListener { requestShutdown() }
            })
        }
        card.addView(column)
        container.addView(card)
    }

    private fun statusText(state: CapabilityState): String = when (state) {
        CapabilityState.AVAILABLE -> "Доступно"
        CapabilityState.NEEDS_SHIZUKU -> "Нужен Shizuku"
        CapabilityState.NEEDS_ADB -> "Нужен ADB на ПК"
        CapabilityState.UNSUPPORTED -> "Не поддерживается"
        CapabilityState.UNKNOWN -> "Не удалось определить"
    }

    private fun requestShutdown() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Выключить устройство?")
            .setMessage("Телефон будет передан штатному системному механизму выключения.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Продолжить") { _, _ ->
                try {
                    startActivity(Intent(Intent.ACTION_REQUEST_SHUTDOWN).apply {
                        putExtra(Intent.EXTRA_KEY_CONFIRM, true)
                    })
                } catch (_: Exception) {
                    showAdbInstructions(
                        Capability(
                            RebootOperation.SHUTDOWN,
                            CapabilityState.NEEDS_ADB,
                            "Системный запрос недоступен.",
                            "ADB"
                        )
                    )
                }
            }.show()
    }

    private fun showAdbInstructions(capability: Capability) {
        MaterialAlertDialogBuilder(this)
            .setTitle("ADB — ${capability.operation.title}")
            .setMessage(AdbInstructions.text(capability))
            .setPositiveButton("Понятно", null)
            .show()
    }
}
