package com.fuckhyperotg.hooks

import android.os.Handler
import com.fuckhyperotg.utils.PrefsManager
import com.fuckhyperotg.utils.XposedUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class SettingsHook : BaseHook() {
    override val targetPackage: String = "com.android.settings"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedUtils.log("Loaded target package: $targetPackage")

        hookOtgControllers(lpparam)
        hookTimeoutHandlers(lpparam)
    }

    private fun hookOtgControllers(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classLoader = lpparam.classLoader

        // Candidate controller classes in Xiaomi MIUI / HyperOS Settings
        val candidateClasses = listOf(
            "com.android.settings.development.OtgPreferenceController",
            "com.android.settings.other.OtherPersonalSettings",
            "com.android.settings.connecteddevice.usb.UsbDefaultFragment",
            "com.android.settings.connecteddevice.usb.UsbConnectionBroadcastReceiver",
            "com.android.settings.fuelgauge.PowerUsageSummary",
            "com.android.settingslib.development.AbstractEnableOtgPreferenceController"
        )

        for (className in candidateClasses) {
            val clazz = XposedHelpers.findClassIfExists(className, classLoader) ?: continue
            XposedUtils.log("Found Settings candidate class: $className")

            // 1. Force Available Status
            XposedUtils.findAndHookMethodSafely(
                className,
                classLoader,
                "getAvailabilityStatus",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (PrefsManager.isForceShowSwitchEnabled()) {
                            // 0 = AVAILABLE in Android SettingsLib BasePreferenceController
                            param.result = 0
                        }
                    }
                }
            )

            XposedUtils.findAndHookMethodSafely(
                className,
                classLoader,
                "isAvailable",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (PrefsManager.isForceShowSwitchEnabled()) {
                            param.result = true
                        }
                    }
                }
            )

            // 2. Prevent setChecked(false) or setOtgEnabled(false) when triggered by auto-timeout
            val disableHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!PrefsManager.isPreventTimeoutEnabled()) return

                    val isEnabling = param.args.getOrNull(0) as? Boolean
                    // If trying to disable OTG (value == false)
                    if (isEnabling == false) {
                        val stackTrace = Thread.currentThread().stackTrace
                        val isTimeoutTriggered = stackTrace.any { element ->
                            val s = (element.className + "." + element.methodName).lowercase()
                            s.contains("timeout") ||
                                    s.contains("timer") ||
                                    s.contains("alarm") ||
                                    s.contains("runnable") ||
                                    s.contains("handlemessage") ||
                                    s.contains("dispatchmessage")
                        }

                        if (isTimeoutTriggered) {
                            XposedUtils.log("Blocked OTG auto-disable from timeout in $className")
                            param.result = null // Cancel execution
                        }
                    }
                }
            }

            XposedUtils.findAndHookMethodSafely(className, classLoader, "setChecked", Boolean::class.java, disableHook)
            XposedUtils.findAndHookMethodSafely(className, classLoader, "setOtgEnabled", Boolean::class.java, disableHook)
            XposedUtils.findAndHookMethodSafely(className, classLoader, "updateState", Any::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val pref = param.args.getOrNull(0)
                    if (pref != null && PrefsManager.isForceShowSwitchEnabled()) {
                        // Ensure preference is always enabled and not greyed out
                        try {
                            XposedHelpers.callMethod(pref, "setEnabled", true)
                        } catch (_: Throwable) {}
                    }
                }
            })
        }
    }

    private fun hookTimeoutHandlers(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classLoader = lpparam.classLoader

        // Hook Handler.postDelayed to intercept 10-minute (600,000ms) OTG timeout runnables
        try {
            XposedHelpers.findAndHookMethod(
                Handler::class.java,
                "postDelayed",
                Runnable::class.java,
                Long::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (!PrefsManager.isPreventTimeoutEnabled()) return

                        val delay = param.args[1] as? Long ?: return
                        // 10 minutes = 600,000 ms; check for timeouts between 5 mins and 30 mins
                        if (delay in 300000L..1800000L) {
                            val runnable = param.args[0]
                            val runnableName = runnable?.javaClass?.name?.lowercase() ?: ""
                            val callerClass = Thread.currentThread().stackTrace.getOrNull(3)?.className?.lowercase() ?: ""

                            if (runnableName.contains("otg") || callerClass.contains("otg")) {
                                XposedUtils.log("Prevented scheduling OTG auto-disable timeout (${delay}ms) in $callerClass")
                                // Set delay to Long.MAX_VALUE or cancel
                                param.args[1] = Long.MAX_VALUE
                            }
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedUtils.logError("Failed to hook Handler.postDelayed", t)
        }
    }
}
