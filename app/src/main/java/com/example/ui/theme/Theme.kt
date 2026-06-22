package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = SophisticatedDarkPrimary,
    onPrimary = SophisticatedDarkOnPrimary,
    primaryContainer = SophisticatedDarkSurface,
    onPrimaryContainer = SophisticatedDarkText,
    secondary = SophisticatedDarkTertiary,
    onSecondary = SophisticatedDarkOnTertiary,
    secondaryContainer = SophisticatedDarkOutline,
    onSecondaryContainer = SophisticatedDarkText,
    tertiary = SophisticatedDarkTertiary,
    background = SophisticatedDarkBg,
    surface = SophisticatedDarkBg,
    surfaceVariant = SophisticatedDarkSurface,
    onBackground = SophisticatedDarkText,
    onSurface = SophisticatedDarkText,
    onSurfaceVariant = SophisticatedDarkSubText,
    outline = SophisticatedDarkOutline,
    outlineVariant = SophisticatedDarkOutline
  )

private val LightColorScheme = DarkColorScheme

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Force false to preserve Sophisticated Dark theme branding consistently across all devices
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }


  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
