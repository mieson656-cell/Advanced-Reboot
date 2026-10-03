package com.mieson656.advancedreboot

object RebootCommandPolicy {
    private val allowedCommands = setOf(
        "reboot",
        "reboot recovery",
        "reboot bootloader",
        "reboot -p"
    )

    fun isAllowed(command: String): Boolean = command.trim() in allowedCommands
}
