package io.github.pagkit.sample.android

import android.app.Application
import io.github.pagkit.PagKitAndroid

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PagKitAndroid.init(this)
    }
}
