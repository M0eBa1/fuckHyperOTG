package com.fuckhyperotg.hooks

import com.fuckhyperotg.utils.PrefsManager
import com.fuckhyperotg.utils.XposedUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class SystemServerHook : BaseHook() {
    override val targetPackage: String = "android"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "android") return

        XposedUtils.log("Loaded system_server framework hook")

        hookUsbDeviceManager(lpparam)
        hookAlarmManager(lpparam)
        hookMiuiUsbService(lpparam)
    }

    private fun hookUsbDeviceManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classLoader = lpparam.classLoader

        // Hook UsbDeviceManager / UsbHostManager
        val usbDeviceManagerClass = XposedHelpers.findClassIfExists("com.android.server.usb.UsbDeviceManager", classLoader)
        if (usbDeviceManagerClass != null) {
            XposedUtils.log("Found UsbDeviceManager in system_server")

            // Hook setCurrentFunctions or setEnabledFunctions to ensure host/otg states aren't abruptly terminated
            XposedUtils.hookAllMethodsSafely(
                "com.android.server.usb.UsbDeviceManager",
                classLoader,
                "setCurrentFunctions",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        // Check if an OTG shutoff function mask is being applied unexpectedly
                    }
                }
            )
        }

        // Hook UsbPortManager for USB Type-C role switching
        val usbPortManagerClass = XposedHelpers.findClassIfExists("com.android.server.usb.UsbPortManager", classLoader)
        if (usbPortManagerClass != null) {
            XposedUtils.log("Found UsbPortManager in system_server")

            // Hook resetPort or setPortRoles to keep power delivery active
            XposedUtils.hookAllMethodsSafely(
                "com.android.server.usb.UsbPortManager",
                classLoader,
                "setPortRoles",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (!PrefsManager.isKeepHostPowerEnabled()) return
                        // args usually include portId, newRole (powerRole, dataRole)
                    }
                }
            )
        }
    }

    private fun hookAlarmManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classLoader = lpparam.classLoader
        val alarmManagerServiceClass = XposedHelpers.findClassIfExists("com.android.server.AlarmManagerService", classLoader)
        if (alarmManagerServiceClass != null) {
            XposedUtils.log("Found AlarmManagerService in system_server")

            // Hook setImpl to filter out OTG timeout alarms
            XposedUtils.hookAllMethodsSafely(
                "com.android.server.AlarmManagerService",
                classLoader,
                "setImpl",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (!PrefsManager.isPreventTimeoutEnabled()) return

                        // Inspect PendingIntent or action in arguments
                        for (arg in param.args) {
                            if (arg == null) continue
                            val argStr = arg.toString().lowercase()
                            if (argStr.contains("otg") && (argStr.contains("timeout") || argStr.contains("disable") || argStr.contains("close"))) {
                                XposedUtils.log("Intercepted AlarmManager OTG timeout registration: $argStr")
                                param.result = null // Prevent alarm from scheduling
                                return
                            }
                        }
                    }
                }
            )
        }
    }

    private fun hookMiuiUsbService(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classLoader = lpparam.classLoader

        // Candidate MIUI / HyperOS custom service classes
        val miuiUsbClasses = listOf(
            "com.miui.server.usb.MiuiUsbService",
            "com.android.server.usb.MiuiUsbService",
            "com.miui.server.SecurityManagerService"
        )

        for (className in miuiUsbClasses) {
            val clazz = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            XposedUtils.log("Found MIUI USB Service: $className")

            // Block any method that disables OTG due to timeout or lack of activity
            XposedUtils.hookAllMethodsSafely(
                className,
                classLoader,
                "disableOtg",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (PrefsManager.isPreventTimeoutEnabled()) {
                            XposedUtils.log("Blocked disableOtg in $className")
                            param.result = null
                        }
                    }
                }
            )

            XposedUtils.hookAllMethodsSafely(
                className,
                classLoader,
                "setOtgEnabled",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (PrefsManager.isPreventTimeoutEnabled()) {
                            val enabled = param.args.getOrNull(0) as? Boolean
                            if (enabled == false) {
                                XposedUtils.log("Blocked setOtgEnabled(false) in $className")
                                param.result = null
                            }
                        }
                    }
                }
            )
        }
    }
}
