package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MedicalTealNight,
    onPrimary = Color(0xFF003731),
    primaryContainer = MedicalTealNightContainer,
    onPrimaryContainer = Color(0xFF73F8E5),
    secondary = MedicalSlateNight,
    onSecondary = Color(0xFF1B3534),
    secondaryContainer = Color(0xFF324B4B),
    onSecondaryContainer = Color(0xFFCCE8E6),
    tertiary = Color(0xFFFFB68E),
    onTertiary = Color(0xFF502400),
    tertiaryContainer = Color(0xFF713700),
    onTertiaryContainer = Color(0xFFFFDCC5),
    background = MedicalBackgroundDark,
    onBackground = MedicalOnSurfaceDark,
    surface = MedicalSurfaceDark,
    onSurface = MedicalOnSurfaceDark,
    surfaceVariant = MedicalSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFBEC9C7),
    outline = Color(0xFF889391),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = MedicalTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F6F1),
    onPrimaryContainer = MedicalTealDark,
    secondary = MedicalSlate,
    onSecondary = Color.White,
    secondaryContainer = MedicalSlateContainer,
    onSecondaryContainer = Color(0xFF051F1F),
    tertiary = ClinicalAmber,
    onTertiary = Color.White,
    tertiaryContainer = ClinicalAmberContainer,
    onTertiaryContainer = Color(0xFF301400),
    background = MedicalBackgroundLight,
    onBackground = MedicalOnSurfaceLight,
    surface = MedicalSurfaceLight,
    onSurface = MedicalOnSurfaceLight,
    surfaceVariant = MedicalSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF3F4948),
    outline = MedicalOutlineLight,
    error = ClinicalError,
    onError = Color.White,
    errorContainer = ClinicalErrorContainer,
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve clinical branding consistency
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun AadharClinicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MyApplicationTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
