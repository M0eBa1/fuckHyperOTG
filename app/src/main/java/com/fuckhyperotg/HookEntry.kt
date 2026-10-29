package com.fuckhyperotg

import com.fuckhyperotg.hooks.ProviderHook
import com.fuckhyperotg.hooks.SettingsHook
import com.fuckhyperotg.hooks.SystemServerHook
import com.fuckhyperotg.utils.XposedUtils
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookEntry : IXposedHookLoadPackage, IXposedHookZygoteInit {

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        XposedUtils.log("FuckHyperOTG Zygote initialized")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName

        // 1. Module Self-Activation Check
        if (packageName == "com.fuckhyperotg" || packageName == "com.fuckhyperotg.debug") {
            try {
                XposedHelpers.findAndHookMethod(
                    "com.fuckhyperotg.ui.MainActivity",
                    lpparam.classLoader,
                    "isModuleActivated",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            param.result = true
                        }
                    }
                )
            } catch (t: Throwable) {
                XposedUtils.logError("Failed to hook isModuleActivated", t)
            }
            return
        }

        // 2. Dispatch to specific hookers
        when (packageName) {
            "com.android.settings" -> {
                SettingsHook().handleLoadPackage(lpparam)
            }
            "android" -> {
                SystemServerHook().handleLoadPackage(lpparam)
                ProviderHook().handleLoadPackage(lpparam)
            }
            "com.android.providers.settings" -> {
                ProviderHook().handleLoadPackage(lpparam)
            }
        }
    }
}
