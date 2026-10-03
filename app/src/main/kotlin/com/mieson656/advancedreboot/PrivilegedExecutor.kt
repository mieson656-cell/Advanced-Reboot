package com.mieson656.advancedreboot

interface PrivilegedExecutor {
    fun isAvailable(): Boolean
    fun execute(command: String): Result<String>
}
