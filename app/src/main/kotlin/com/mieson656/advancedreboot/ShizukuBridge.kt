package com.mieson656.advancedreboot

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

interface PrivilegedExecutor {
    fun isAvailable(): Boolean
    fun execute(command: String): Result<String>
}

class ShizukuBridge(private val context: Context) : PrivilegedExecutor {
    companion object {
        const val REQUEST_CODE = 1001
    }

    fun isBinderReady(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    fun hasPermission(): Boolean = try {
        isBinderReady() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    fun requestPermission() {
        if (isBinderReady() && !hasPermission()) {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    override fun isAvailable(): Boolean = hasPermission()

    override fun execute(command: String): Result<String> =
        Result.failure(UnsupportedOperationException("Командный Shizuku-провайдер ещё не активирован для этой операции"))
}
