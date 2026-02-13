package com.autoguard.vpn

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AutoGuardApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 取得目前 App 專屬的語言設定
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        
        // 如果是第一次執行（設定為空），強制預設為英文
        // 這樣可以防止 App 啟動時自動套用手機系統的語言（如繁體中文）
        if (currentLocales.isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
    }
}
