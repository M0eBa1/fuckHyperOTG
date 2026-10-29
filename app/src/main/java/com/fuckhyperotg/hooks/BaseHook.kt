package com.fuckhyperotg.hooks

import de.robv.android.xposed.callbacks.XC_LoadPackage

abstract class BaseHook {
    abstract val targetPackage: String
    abstract fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam)
}
