package com.fuckhyperotg.utils

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Method

object XposedUtils {
    private const val TAG = "FuckHyperOTG"

    fun log(msg: String) {
        XposedBridge.log("[$TAG] $msg")
    }

    fun logError(msg: String, throwable: Throwable? = null) {
        XposedBridge.log("[$TAG] ERROR: $msg")
        throwable?.let { XposedBridge.log(it) }
    }

    /**
     * Safely find and hook a method, ignoring ClassNotFoundException or NoSuchMethodError
     */
    fun findAndHookMethodSafely(
        className: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg parameterTypesAndCallback: Any
    ): XC_MethodHook.Unhook? {
        return try {
            val clazz = XposedHelpers.findClassIfExists(className, classLoader) ?: return null
            XposedHelpers.findAndHookMethod(clazz, methodName, *parameterTypesAndCallback)
        } catch (t: Throwable) {
            log("Method $methodName in $className not hooked: ${t.message}")
            null
        }
    }

    /**
     * Safely hook all methods matching name in class
     */
    fun hookAllMethodsSafely(
        className: String,
        classLoader: ClassLoader,
        methodName: String,
        callback: XC_MethodHook
    ) {
        try {
            val clazz = XposedHelpers.findClassIfExists(className, classLoader) ?: return
            for (method in clazz.declaredMethods) {
                if (method.name == methodName) {
                    XposedBridge.hookMethod(method, callback)
                }
            }
        } catch (t: Throwable) {
            log("Failed to hook all $methodName in $className: ${t.message}")
        }
    }
}
