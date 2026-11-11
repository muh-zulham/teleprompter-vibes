package com.telepromptervibes.app

import android.app.Application
import com.telepromptervibes.app.di.AppContainer

/**
 * Application subclass. Init AppContainer sekali saat proses app dibuat.
 * Container diakses dari ViewModel factory (Langkah 7+).
 */
class TeleprompterApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
