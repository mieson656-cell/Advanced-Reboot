package com.mieson656.advancedreboot

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku

class ShizukuBridge(private val context: Context) : PrivilegedExecutor {
    companion object {
        const val REQUEST_CODE = 1001
    }

    private var service: IRebootUserService? = null
    private var connection: ServiceConnection? = null
    private var boundArgs: Shizuku.UserServiceArgs? = null

    fun isBinderReady(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) { false }

    fun hasPermission(): Boolean = try {
        isBinderReady() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) { false }

    fun requestPermission(): Boolean {
        if (!isBinderReady()) return false
        if (hasPermission()) return true
        return try { Shizuku.requestPermission(REQUEST_CODE); true } catch (_: Throwable) { false }
    }

    fun isServiceConnected(): Boolean = service != null

    fun connect(onReady: (Boolean) -> Unit) {
        if (!hasPermission()) { onReady(false); return }
        if (service != null) { onReady(true); return }
        if (connection != null) { onReady(false); return }

        val args = Shizuku.UserServiceArgs(
            ComponentName(context, RebootUserService::class.java)
        ).version(1).processNameSuffix("reboot").daemon(false)

        val newConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                if (binder == null) {
                    service = null; connection = null; onReady(false); return
                }
                val remote = IRebootUserService.Stub.asInterface(binder)
                service = remote
                onReady(remote != null)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null; connection = null
            }

            override fun onBindingDied(name: ComponentName?) {
                service = null; connection = null
                tryUnbind(args, newConnection)
            }

            override fun onNullBinding(name: ComponentName?) {
                service = null; connection = null; onReady(false)
            }
        }
        connection = newConnection
        boundArgs = args
        try {
            Shizuku.bindUserService(args, newConnection)
        } catch (_: Throwable) {
            connection = null; boundArgs = null; service = null; onReady(false)
        }
    }

    private fun tryUnbind(args: Shizuku.UserServiceArgs, connection: ServiceConnection) {
        try { Shizuku.unbindUserService(args, connection, true) } catch (_: Throwable) { }
    }

    fun disconnect() {
        val current = connection
        val args = boundArgs
        if (current != null && args != null) tryUnbind(args, current)
        connection = null; boundArgs = null; service = null
    }

    override fun isAvailable(): Boolean = hasPermission() && isBinderReady()

    override fun execute(command: String): Result<String> {
        val cleanCommand = command.trim()
        if (cleanCommand.isEmpty()) return Result.failure(IllegalArgumentException("Пустая команда"))
        if (!isAvailable()) return Result.failure(IllegalStateException("Shizuku недоступен или разрешение отозвано"))
        val current = service ?: return Result.failure(IllegalStateException("Shizuku User Service не подключён"))
        return try {
            val response = current.execute(cleanCommand)
            if (response.startsWith("exit=0")) Result.success(response)
            else Result.failure(IllegalStateException(response.ifBlank { "Команда завершилась с ошибкой" }))
        } catch (e: Throwable) {
            service = null
            Result.failure(IllegalStateException("Не удалось выполнить команду через Shizuku: " + (e.message ?: e.javaClass.simpleName), e))
        }
    }
}
