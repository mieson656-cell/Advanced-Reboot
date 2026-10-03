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

    fun connect(onReady: (Boolean) -> Unit) {
        if (!hasPermission()) {
            onReady(false)
            return
        }
        if (service != null) {
            onReady(true)
            return
        }

        val args = Shizuku.UserServiceArgs(
            ComponentName(context, RebootUserService::class.java)
        ).version(1).processNameSuffix("reboot").daemon(false)

        val newConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = IRebootUserService.Stub.asInterface(binder)
                onReady(service != null)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
            }
        }
        connection = newConnection
        try {
            Shizuku.bindUserService(args, newConnection)
        } catch (_: Throwable) {
            connection = null
            service = null
            onReady(false)
        }
    }

    fun disconnect() {
        val current = connection ?: return
        try {
            val args = Shizuku.UserServiceArgs(
                ComponentName(context, RebootUserService::class.java)
            ).version(1).processNameSuffix("reboot").daemon(false)
            Shizuku.unbindUserService(args, current, true)
        } catch (_: Throwable) {
        }
        connection = null
        service = null
    }

    override fun isAvailable(): Boolean = hasPermission()

    override fun execute(command: String): Result<String> {
        val current = service ?: return Result.failure(
            IllegalStateException("Shizuku user service не подключён")
        )
        return try {
            Result.success(current.execute(command))
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}
