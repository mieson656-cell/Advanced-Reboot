package com.mieson656.advancedreboot

class RebootUserService : IRebootUserService.Stub() {
    override fun execute(command: String): String {
        val cleanCommand = command.trim()
        if (!RebootCommandPolicy.isAllowed(cleanCommand)) {
            return "error=SecurityException: команда не разрешена"
        }

        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf(
                    "/system/bin/reboot",
                    *cleanCommand.removePrefix("reboot").trim().let {
                        if (it.isEmpty()) emptyArray() else it.split(" ").toTypedArray()
                    }
                )
            )
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val code = process.waitFor()
            "exit=$code\n" + output + if (error.isNotBlank()) "\n$error" else ""
        } catch (e: Exception) {
            "error=" + e.javaClass.simpleName + ": " + e.message
        }
    }

    override fun destroy() {
        System.exit(0)
    }
}
