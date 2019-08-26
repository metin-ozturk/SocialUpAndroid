package com.jora.socialup

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.squareup.leakcanary.LeakCanary

class CustomApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }
        LeakCanary.install(this)
    }
}