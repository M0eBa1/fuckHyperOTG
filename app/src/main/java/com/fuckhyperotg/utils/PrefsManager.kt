package com.fuckhyperotg.utils

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences

object PrefsManager {
    const val PREFS_NAME = "fuck_hyper_otg_prefs"

    const val KEY_PREVENT_TIMEOUT = "key_prevent_timeout"
    const val KEY_FORCE_SHOW_SWITCH = "key_force_show_switch"
    const val KEY_KEEP_HOST_POWER = "key_keep_host_power"

    private var xSharedPreferences: XSharedPreferences? = null

    fun getAppPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getXposedPrefs(): XSharedPreferences {
        if (xSharedPreferences == null) {
            xSharedPreferences = XSharedPreferences("com.fuckhyperotg", PREFS_NAME).apply {
                makeWorldReadable()
                reload()
            }
        } else {
            xSharedPreferences?.reload()
        }
        return xSharedPreferences!!
    }

    fun isPreventTimeoutEnabled(): Boolean {
        return try {
            getXposedPrefs().getBoolean(KEY_PREVENT_TIMEOUT, true)
        } catch (_: Throwable) {
            true
        }
    }

    fun isForceShowSwitchEnabled(): Boolean {
        return try {
            getXposedPrefs().getBoolean(KEY_FORCE_SHOW_SWITCH, true)
        } catch (_: Throwable) {
            true
        }
    }

    fun isKeepHostPowerEnabled(): Boolean {
        return try {
            getXposedPrefs().getBoolean(KEY_KEEP_HOST_POWER, true)
        } catch (_: Throwable) {
            true
        }
    }
}
