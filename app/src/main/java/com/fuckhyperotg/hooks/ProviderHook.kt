package com.fuckhyperotg.hooks

import android.content.ContentResolver
import android.provider.Settings
import com.fuckhyperotg.utils.PrefsManager
import com.fuckhyperotg.utils.XposedUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ProviderHook : BaseHook() {
    override val targetPackage: String = "com.android.providers.settings"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classLoader = lpparam.classLoader

        // Hook Settings.Global / System / Secure putInt & putString
        val targets = listOf(
            Settings.Global::class.java,
            Settings.System::class.java,
            Settings.Secure::class.java
        )

        for (targetClass in targets) {
            try {
                // Hook putInt(ContentResolver cr, String name, int value)
                XposedHelpers.findAndHookMethod(
                    targetClass,
                    "putInt",
                    ContentResolver::class.java,
                    String::class.java,
                    Int::class.javaPrimitiveType,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            if (!PrefsManager.isPreventTimeoutEnabled()) return

                            val name = param.args[1] as? String ?: return
                            val value = param.args[2] as? Int ?: return

                            if (isOtgKey(name) && value == 0) {
                                val stackTrace = Thread.currentThread().stackTrace
                                val isTimeout = stackTrace.any {
                                    val s = (it.className + "." + it.methodName).lowercase()
                                    s.contains("timeout") || s.contains("timer") || s.contains("alarm") || s.contains("idle")
                                }
                                if (isTimeout) {
                                    XposedUtils.log("Intercepted $name -> 0 in ${targetClass.simpleName}, keeping 1")
                                    param.args[2] = 1 // Force to remain enabled
                                }
                            }
                        }
                    }
                )

                // Hook putString(ContentResolver cr, String name, String value)
                XposedHelpers.findAndHookMethod(
                    targetClass,
                    "putString",
                    ContentResolver::class.java,
                    String::class.java,
                    String::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            if (!PrefsManager.isPreventTimeoutEnabled()) return

                            val name = param.args[1] as? String ?: return
                            val value = param.args[2] as? String ?: return

                            if (isOtgKey(name) && (value == "0" || value.equals("false", ignoreCase = true))) {
                                val stackTrace = Thread.currentThread().stackTrace
                                val isTimeout = stackTrace.any {
                                    val s = (it.className + "." + it.methodName).lowercase()
                                    s.contains("timeout") || s.contains("timer") || s.contains("alarm") || s.contains("idle")
                                }
                                if (isTimeout) {
                                    XposedUtils.log("Intercepted $name -> $value in ${targetClass.simpleName}, keeping active")
                                    param.args[2] = "1"
                                }
                            }
                        }
                    }
                )
            } catch (t: Throwable) {
                XposedUtils.log("Could not hook ${targetClass.simpleName}: ${t.message}")
            }
        }
    }

    private fun isOtgKey(name: String): Boolean {
        val lower = name.lowercase()
        return lower == "otg" ||
                lower == "otg_switch" ||
                lower == "otg_enabled" ||
                lower == "persist.sys.miui.otg" ||
                lower == "persist.sys.otg_support" ||
                lower == "otg_auto_disable" ||
                lower == "otg_time_out_close"
    }
}
