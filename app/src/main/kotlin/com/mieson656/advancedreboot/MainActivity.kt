package com.mieson656.advancedreboot

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
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private lateinit var device: DeviceInfo
    private lateinit var capabilities: List<Capability>
    private lateinit var shizuku: ShizukuBridge
    private lateinit var modesContainer: LinearLayout

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) renderCapabilities()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val progress = findViewById<LinearProgressIndicator>(R.id.progress)
        val subtitle = findViewById<TextView>(R.id.subtitle)
        val deviceName = findViewById<TextView>(R.id.deviceName)
        val deviceDetails = findViewById<TextView>(R.id.deviceDetails)
        modesContainer = findViewById(R.id.modesContainer)

        shizuku = ShizukuBridge(this)
        Shizuku.addRequestPermissionResultListener(permissionListener)

        device = DeviceInfo.read()
        deviceName.text = "${device.manufacturer.replaceFirstChar { it.uppercase() }} ${device.model}"
        deviceDetails.text = "Android ${device.androidVersion} (API ${device.apiLevel})\nСистемная оболочка: ${device.shell}"

        renderCapabilities()
        progress.visibility = View.GONE
        subtitle.text = "Проверка завершена"
    }

    private fun renderCapabilities() {
        modesContainer.removeAllViews()
        capabilities = CapabilityDetector().detect(device, shizuku.hasPermission())
        capabilities.forEach { addCapability(modesContainer, it) }
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

        if (capability.state == CapabilityState.AVAILABLE && capability.provider == "Shizuku") {
            column.addView(MaterialButton(this).apply {
                text = "Выполнить через Shizuku"
                setOnClickListener { confirmPrivilegedAction(capability) }
            })
        }

        if (capability.state == CapabilityState.NEEDS_ADB) {
            column.addView(MaterialButton(this).apply {
                text = "Подключить Shizuku"
                setOnClickListener {
                    if (shizuku.isBinderReady()) {
                        if (!shizuku.requestPermission()) showShizukuError()
                    } else openShizuku()
                }
            })
        }

        capability.adbCommand?.let {
            column.addView(MaterialButton(this).apply {
                text = "Показать ADB-команду"
                setOnClickListener { showAdbInstructions(capability) }
            })
        }


        card.addView(column)
        container.addView(card)
    }

    private fun statusText(state: CapabilityState): String = when (state) {
        CapabilityState.AVAILABLE -> "Доступно"
        CapabilityState.NEEDS_SHIZUKU -> "Нужен Shizuku"
        CapabilityState.NEEDS_ADB -> "Нужен Shizuku или ADB на ПК"
        CapabilityState.UNSUPPORTED -> "Не поддерживается"
        CapabilityState.UNKNOWN -> "Не удалось определить"
    }

    private fun confirmPrivilegedAction(capability: Capability) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Выполнить «${capability.operation.title}»?")
            .setMessage("Команда будет выполнена через Shizuku с привилегиями его текущего провайдера.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Продолжить") { _, _ ->
                shizuku.connect { ready ->
                    if (!ready) {
                        runOnUiThread { showShizukuError() }
                        return@connect
                    }
                    Thread {
                        val result = shizuku.execute(capability.providerCommand ?: "")
                        runOnUiThread {
                            MaterialAlertDialogBuilder(this)
                                .setTitle(if (result.isSuccess) "Команда отправлена" else "Ошибка выполнения")
                                .setMessage(result.getOrElse { it.message ?: "Неизвестная ошибка" })
                                .setPositiveButton("Понятно", null)
                                .show()
                        }
                    }.start()
                }
            }.show()
    }

    private fun showShizukuServiceError() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Shizuku User Service не запустился")
            .setMessage("Разрешение Shizuku есть, но привилегированный сервис не удалось подключить. Перезапустите Shizuku и попробуйте снова.")
            .setPositiveButton("Понятно", null)
            .show()
    }

    private fun showShizukuError() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Shizuku недоступен")
            .setMessage("Запустите Shizuku и выдайте Advanced Reboot разрешение. Если Shizuku недоступен, используйте ADB-команду.")
            .setPositiveButton("Понятно", null)
            .show()
    }

    private fun openShizuku() {
        val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        if (intent != null) startActivity(intent) else showShizukuError()
    }

    private fun showAdbInstructions(capability: Capability) {
        MaterialAlertDialogBuilder(this)
            .setTitle("ADB — ${capability.operation.title}")
            .setMessage(AdbInstructions.text(capability))
            .setPositiveButton("Понятно", null)
            .show()
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        shizuku.disconnect()
        super.onDestroy()
    }
}
