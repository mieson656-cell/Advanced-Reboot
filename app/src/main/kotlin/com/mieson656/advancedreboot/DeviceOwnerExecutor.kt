package com.mieson656.advancedreboot

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

class DeviceOwnerExecutor(context: Context) {
    private val appContext = context.applicationContext
    private val policyManager = appContext.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(appContext, AdvancedRebootAdminReceiver::class.java)

    fun isAvailable(): Boolean =
        policyManager?.isDeviceOwnerApp(appContext.packageName) == true

    fun reboot(): Result<String> {
        if (!isAvailable()) {
            return Result.failure(IllegalStateException("Приложение не является владельцем устройства"))
        }
        return try {
            policyManager?.reboot(admin)
            Result.success("Системный запрос перезагрузки отправлен")
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}
