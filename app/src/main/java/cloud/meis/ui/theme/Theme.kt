package cloud.meis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    secondary = BrandGradientEnd,
    background = ColorBlack,
    surface = ColorBlack,
    onPrimary = ColorWhite,
    onSecondary = ColorWhite,
    onBackground = ColorWhite,
    onSurface = ColorWhite,
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    secondary = BrandGradientEnd,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = ColorWhite,
    onSecondary = ColorWhite,
    onBackground = TextMain,
    onSurface = TextMain,
    error = ErrorRed
)

@Composable
fun Tugas_LatihanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}