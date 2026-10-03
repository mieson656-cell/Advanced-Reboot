package com.mieson656.advancedreboot

object AdbInstructions {
    fun text(capability: Capability): String {
        val command = capability.adbCommand
        return if (command != null) {
            "1. Включите «Параметры разработчика» и «Отладка по USB».\n\n" +
                "2. Подключите телефон к компьютеру и подтвердите RSA-запрос.\n\n" +
                "3. Откройте CMD/Terminal в папке Android platform-tools.\n\n" +
                "4. Выполните:\n\n$command"
        } else {
            "Для этого режима нет универсальной ADB-команды. Используйте только способ, подтверждённый для вашей модели."
        }
    }
}
