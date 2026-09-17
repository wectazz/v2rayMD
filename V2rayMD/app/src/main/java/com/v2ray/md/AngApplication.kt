package com.v2ray.md

import android.app.Application
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.work.Configuration
import androidx.work.WorkManager
import com.v2ray.md.AppConfig.ANG_PACKAGE
import com.v2ray.md.handler.AppLocaleManager
import com.v2ray.md.handler.MmkvManager
import com.v2ray.md.handler.SettingsManager
import com.v2ray.md.ui.compose.MonetColors
import com.v2ray.md.ui.compose.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AngApplication : Application() {
    companion object {
        lateinit var application: AngApplication
    }

    /**
     * Attaches the base context to the application.
     * @param base The base context.
     */
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base?.let(ContextCompat::getContextForLanguage))
        application = this
    }

    private val workManagerConfiguration: Configuration = Configuration.Builder()
        .setDefaultProcessName("${ANG_PACKAGE}:bg")
        .build()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Initializes the application.
     */
    override fun onCreate() {
        super.onCreate()

        MmkvManager.initialize(this)

        AppLocaleManager.initialize(this)

        // Initialize WorkManager with the custom configuration
        WorkManager.initialize(this, workManagerConfiguration)

        // Ensure critical preference defaults are present in MMKV early
        SettingsManager.initApp(this)

        // Initialize theme state from MMKV
        ThemeManager.refresh()

        // Wallpaper seed for pre-S dynamic color (Monet backport); no-op if it fails
        appScope.launch { MonetColors.initialize(this@AngApplication) }
    }
}
