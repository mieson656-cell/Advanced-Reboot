package com.mieson656.advancedreboot

import org.junit.Assert.assertEquals
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
    fun shutdownFallsBackToAdb() {
        val c = CapabilityDetector().detect(device, false).first { it.operation == RebootOperation.SHUTDOWN }
        assertEquals(CapabilityState.NEEDS_ADB, c.state)
        assertEquals("adb shell reboot -p", c.adbCommand)
    }

    @Test
    fun shutdownUsesShizukuWhenAuthorized() {
        val c = CapabilityDetector().detect(device, true).first { it.operation == RebootOperation.SHUTDOWN }
        assertEquals(CapabilityState.AVAILABLE, c.state)
        assertEquals("reboot -p", c.providerCommand)
    }

    @Test
    fun downloadModeNeverInventsACommand() {
        val c = CapabilityDetector().detect(device, false).first { it.operation == RebootOperation.DOWNLOAD }
        assertEquals(CapabilityState.UNKNOWN, c.state)
        assertEquals(null, c.adbCommand)
    }
}
