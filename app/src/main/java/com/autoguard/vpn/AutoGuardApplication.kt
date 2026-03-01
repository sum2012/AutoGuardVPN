package com.autoguard.vpn

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * AutoGuard VPN 应用程序入口类
 * 继承 Application 类并使用 Hilt 进行依赖注入
 */
@HiltAndroidApp
class AutoGuardApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 初始化工作可以在这里进行
    }
}
