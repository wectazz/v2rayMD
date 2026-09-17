package com.v2ray.md.ui.compose

// Adapted from ResukiSu MonetCompatColorSource + Theme.createColorScheme (GPL):
// wallpaper seed extraction for pre-S dynamic color (MonetCompat) and M3 scheme
// generation for all versions (material-kolor). Their Koin/DI, custom-background
// and blur machinery is intentionally omitted: a single static seed plus the
// system accent drive the scheme here.
import android.app.Application
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.kieronquinn.monetcompat.core.MonetCompat
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

object MonetColors {
    private val _wallpaperSeed = MutableStateFlow<Int?>(null)
    val wallpaperSeed: StateFlow<Int?> = _wallpaperSeed.asStateFlow()

    suspend fun initialize(app: Application) {
        MonetCompat.useSystemColorsOnAndroid12 = false
        runCatching { MonetCompat.enablePaletteCompat() }

        val monet = MonetCompat.setup(app)
        val seed: Int? = try {
            withTimeoutOrNull(2000L) {
                monet.getSelectedWallpaperColor()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        monet.wallpaperPrimaryColor = seed
        monet.updateMonetColors()
        _wallpaperSeed.value = seed
    }

    internal fun resolveSeed(
        useDynamic: Boolean,
        isAtLeastS: Boolean,
        systemAccent: Int?,
        monetSeed: Int?,
        fallbackSeed: Int
    ): Int? {
        if (!useDynamic) return null
        if (isAtLeastS && systemAccent != null) return systemAccent
        return monetSeed ?: fallbackSeed
    }

    @Composable
    fun rememberDynamicScheme(darkTheme: Boolean): ColorScheme {
        val monetSeed by wallpaperSeed.collectAsState()
        val context = LocalContext.current
        return remember(darkTheme, monetSeed) {
            val systemAccent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching { context.getColor(android.R.color.system_accent1_500) }.getOrNull()
            } else {
                null
            }
            val seed = resolveSeed(
                useDynamic = true,
                isAtLeastS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                systemAccent = systemAccent,
                monetSeed = monetSeed,
                fallbackSeed = FALLBACK_SEED
            ) ?: FALLBACK_SEED
            dynamicColorScheme(
                seedColor = Color(seed),
                isDark = darkTheme,
                style = PaletteStyle.TonalSpot,
                specVersion = ColorSpec.SpecVersion.SPEC_2021,
            )
        }
    }
}

private val FALLBACK_SEED = 0xFF6750A4.toInt()
