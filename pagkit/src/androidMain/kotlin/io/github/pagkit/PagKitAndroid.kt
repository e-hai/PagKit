package io.github.pagkit

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("StaticFieldLeak")
object PagKitAndroid {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getAppContext(): Context {
        return appContext ?: try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val currentApp = activityThread.getMethod("currentApplication").invoke(null) as? android.app.Application
            currentApp?.applicationContext?.also { appContext = it }
                ?: error("PagKitAndroid: Context not initialized. Please call PagKitAndroid.init(context) in Application.onCreate()")
        } catch (e: Throwable) {
            error("PagKitAndroid: Context not initialized: ${e.message}")
        }
    }
}
