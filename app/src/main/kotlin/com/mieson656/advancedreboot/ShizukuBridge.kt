package com.mieson656.advancedreboot

import android.content.Context

interface PrivilegedExecutor {
    fun isAvailable(): Boolean
    fun execute(command: String): Result<String>
}

/**
 * Boundary for Shizuku integration. The project intentionally keeps the
 * capability engine independent from a concrete Shizuku API.
 */
class ShizukuBridge(private val context: Context) : PrivilegedExecutor {
    override fun isAvailable(): Boolean = false
    override fun execute(command: String): Result<String> =
        Result.failure(UnsupportedOperationException("Shizuku provider is not connected yet"))
}
