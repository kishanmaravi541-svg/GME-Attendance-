package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = AttendanceBlueSecondary,
    onPrimary = AttendanceSurface,
    secondary = AttendanceEmerald,
    onSecondary = AttendanceSurface,
    background = AttendanceSlateDark,
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF1F5F9),
    error = AttendanceError
  )

private val LightColorScheme =
  lightColorScheme(
    primary = AttendanceBluePrimary,
    onPrimary = AttendanceSurface,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = AttendanceBluePrimary,
    secondary = AttendanceEmerald,
    onSecondary = AttendanceSurface,
    secondaryContainer = AttendanceEmeraldLight,
    onSecondaryContainer = AttendanceEmeraldDark,
    background = AttendanceBackground,
    onBackground = AttendanceSlateDark,
    surface = AttendanceSurface,
    onSurface = AttendanceSlateDark,
    surfaceVariant = AttendanceSlateLight,
    onSurfaceVariant = AttendanceSlateMedium,
    error = AttendanceError,
    errorContainer = AttendanceErrorLight,
    onErrorContainer = AttendanceError
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
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
