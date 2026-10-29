package com.fuckhyperotg.utils

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object ShellUtils {

    fun executeRootCommand(command: String): Boolean {
        var process: Process? = null
        var os: DataOutputStream? = null
        return try {
            process = ProcessBuilder("su").start()
            os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            val exitVal = process.waitFor()
            exitVal == 0
        } catch (_: Exception) {
            false
        } finally {
            try {
                os?.close()
                process?.destroy()
            } catch (_: Exception) {}
        }
    }

    /**
     * Force enable OTG via sysfs and system properties for Xiaomi / HyperOS devices
     */
    fun forceEnableOtgSysfs(): Boolean {
        val cmds = listOf(
            // Xiaomi / Qualcomm / MTK common sysfs nodes
            "echo 1 > /sys/class/power_supply/usb/otg_switch 2>/dev/null",
            "echo 1 > /sys/class/power_supply/battery/otg_switch 2>/dev/null",
            "echo host > /sys/class/typec/port0/power_role 2>/dev/null",
            "echo source > /sys/class/typec/port0/power_role 2>/dev/null",
            "echo 1 > /sys/devices/platform/soc/*usb*/otg_switch 2>/dev/null",
            // System properties & settings table
            "setprop persist.sys.miui.otg 1",
            "setprop persist.sys.otg_support true",
            "settings put global otg_switch 1",
            "settings put system otg_switch 1"
        )
        return executeRootCommand(cmds.joinToString(" && "))
    }
}
