package com.jora.socialup

import androidx.multidex.MultiDexApplication

class CustomApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            return
//        }
//        LeakCanary.install(this)
    }
}