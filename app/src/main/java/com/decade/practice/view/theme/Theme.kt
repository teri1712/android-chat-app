package com.decade.practice.view.theme

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.core.view.WindowCompat

private const val THEME_PREFERENCE = "APPLICATION_THEME"
private const val LIGHT = "THEME"

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)


private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC73987),
    onPrimary = Color.White,
    secondary = Color(0xFF191919),
    onSecondary = Color.White,
    surfaceVariant = Color(0, 0, 0, 15),
    tertiary = Pink40,
    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Stable
interface Theme {
    var light: Boolean
}


val LocalTheme = compositionLocalOf<Theme> {
    error("Nothing bound")
}

@Stable
class ThemePreference(context: Context) : Theme {

    private val preference: SharedPreferences = context.getSharedPreferences(THEME_PREFERENCE, Context.MODE_PRIVATE)
    private var _light by mutableStateOf(true)

    init {
        _light = preference.getBoolean(LIGHT, true)
    }

    override var light: Boolean
        get() = _light
        set(value) {
            if (_light == value)
                return
            preference.edit() {
                putBoolean(LIGHT, value)
            }
            _light = value
        }
}

@Composable
fun ApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val theme: Theme = remember { ThemePreference(context) }
    val colorScheme = when {
        theme.light -> LightColorScheme
        else -> DarkColorScheme
    }
    if (context is Activity) {
        context.window.statusBarColor = Color.Transparent.toArgb()
        WindowCompat.getInsetsController(context.window, context.window.decorView)
            .isAppearanceLightStatusBars = theme.light
    }
    CompositionLocalProvider(LocalTheme provides theme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = {
                Surface(color = MaterialTheme.colorScheme.background) {
                    content()
                }
            }
        )
    }
}