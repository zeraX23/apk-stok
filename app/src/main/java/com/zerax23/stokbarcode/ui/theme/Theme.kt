package com.zerax23.stokbarcode.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Green800,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Green200,
    onPrimaryContainer = Green800,
    secondary = Blue800,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = Blue200,
    onSecondaryContainer = Blue800,
    background = BackgroundLight,
    onBackground = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    surface = SurfaceLight,
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    error = androidx.compose.ui.graphics.Color(0xFFB00020),
    onError = androidx.compose.ui.graphics.Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Green200,
    onPrimary = Green800,
    primaryContainer = Green600,
    onPrimaryContainer = Green200,
    secondary = Blue200,
    onSecondary = Blue800,
    secondaryContainer = Blue600,
    onSecondaryContainer = Blue200,
    background = BackgroundDark,
    onBackground = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    surface = SurfaceDark,
    onSurface = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    error = androidx.compose.ui.graphics.Color(0xFFCF6679),
    onError = androidx.compose.ui.graphics.Color(0xFF370B1E)
)

@Composable
fun StokBarcodeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic color untuk Android 12+
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
