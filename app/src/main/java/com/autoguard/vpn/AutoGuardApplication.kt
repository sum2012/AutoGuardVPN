package com.autoguard.vpn

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * AutoGuard VPN Application Entry Point Class
 * Inherits from the Application class and uses Hilt for dependency injection
 */
@HiltAndroidApp
class AutoGuardApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialization work can be done here
    }
}
