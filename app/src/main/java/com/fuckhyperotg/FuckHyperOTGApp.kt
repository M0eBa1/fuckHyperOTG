package com.fuckhyperotg

import android.app.Application

class FuckHyperOTGApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: FuckHyperOTGApp
            private set
    }
}
