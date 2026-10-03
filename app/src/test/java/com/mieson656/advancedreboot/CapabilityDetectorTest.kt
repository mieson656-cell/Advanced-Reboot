package com.mieson656.advancedreboot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityDetectorTest {
    private val device = DeviceInfo("Test", "Model", "15", 35, "Test Shell")

    @Test
    fun rebootWithoutShizukuFallsBackToAdb() {
        val c = CapabilityDetector().detect(device, false).first { it.operation == RebootOperation.REBOOT }
        assertEquals(CapabilityState.NEEDS_ADB, c.state)
        assertEquals("adb reboot", c.adbCommand)
    }

    @Test
    fun recoveryUsesGenericAdbCommand() {
        val c = CapabilityDetector().detect(device, false).first { it.operation == RebootOperation.RECOVERY }
        assertEquals("adb reboot recovery", c.adbCommand)
    }

    @Test
    fun bootloaderUsesGenericAdbCommand() {
        val c = CapabilityDetector().detect(device, false).first { it.operation == RebootOperation.BOOTLOADER }
        assertEquals(CapabilityState.NEEDS_ADB, c.state)
        assertEquals("adb reboot bootloader", c.adbCommand)
    }

    @Test
    fun shutdownFallsBackToAdb() {
        val c = CapabilityDetector().detect(device, false).first { it.operation == RebootOperation.SHUTDOWN }
        assertEquals(CapabilityState.NEEDS_ADB, c.state)
        assertEquals("adb shell reboot -p", c.adbCommand)
    }

    @Test
    fun privilegedOperationsUseShizukuWhenAuthorized() {
        val capabilities = CapabilityDetector().detect(device, true)

        assertEquals("reboot", capabilities.first { it.operation == RebootOperation.REBOOT }.providerCommand)
        assertEquals("reboot recovery", capabilities.first { it.operation == RebootOperation.RECOVERY }.providerCommand)
        assertEquals("reboot bootloader", capabilities.first { it.operation == RebootOperation.BOOTLOADER }.providerCommand)

        val shutdown = capabilities.first { it.operation == RebootOperation.SHUTDOWN }
        assertEquals(CapabilityState.AVAILABLE, shutdown.state)
        assertEquals("reboot -p", shutdown.providerCommand)
    }

    @Test
    fun downloadModeNeverInventsACommand() {
        val c = CapabilityDetector().detect(device, false).first { it.operation == RebootOperation.DOWNLOAD }
        assertEquals(CapabilityState.UNKNOWN, c.state)
        assertNull(c.adbCommand)
        assertNull(c.providerCommand)
    }
}

class RebootCommandPolicyTest {
    @Test
    fun allowsOnlyKnownRebootCommands() {
        assertTrue(RebootCommandPolicy.isAllowed("reboot"))
        assertTrue(RebootCommandPolicy.isAllowed("reboot recovery"))
        assertTrue(RebootCommandPolicy.isAllowed("reboot bootloader"))
        assertTrue(RebootCommandPolicy.isAllowed("reboot -p"))
    }

    @Test
    fun rejectsShellInjectionAndUnknownCommands() {
        assertFalse(RebootCommandPolicy.isAllowed("id"))
        assertFalse(RebootCommandPolicy.isAllowed("reboot; id"))
        assertFalse(RebootCommandPolicy.isAllowed("reboot recovery; id"))
        assertFalse(RebootCommandPolicy.isAllowed("reboot && id"))
    }
}

class ShellDetectorTest {
    @Test
    fun detectsOneUiFromConfirmedProperty() {
        val result = ShellDetector.detect("samsung") { key ->
            if (key == "ro.build.version.oneui") "8.0" else null
        }
        assertEquals("One UI 8.0", result)
    }

    @Test
    fun detectsMiuiFromConfirmedProperty() {
        val result = ShellDetector.detect("xiaomi") { key ->
            if (key == "ro.miui.ui.version.name") "14" else null
        }
        assertEquals("MIUI 14", result)
    }

    @Test
    fun usesUnconfirmedFallbackOnlyForKnownManufacturer() {
        assertEquals("One UI (версия не подтверждена)", ShellDetector.detect("Samsung") { null })
        assertEquals("MIUI/HyperOS (версия не подтверждена)", ShellDetector.detect("XIAOMI") { null })
    }

    @Test
    fun unknownManufacturerDoesNotGetInventedShell() {
        assertEquals("Не удалось определить", ShellDetector.detect("TestBrand") { null })
    }
}
