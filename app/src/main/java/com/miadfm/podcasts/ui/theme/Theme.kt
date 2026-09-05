package com.miadfm.podcasts.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import com.miadfm.podcasts.data.settings.AppLanguage
import com.miadfm.podcasts.ui.i18n.EnglishStrings
import com.miadfm.podcasts.ui.i18n.LocalAppLanguage
import com.miadfm.podcasts.ui.i18n.LocalAppStrings
import com.miadfm.podcasts.ui.i18n.PersianStrings

private val PodcastsDarkColorScheme = darkColorScheme(
    primary = BlueAccent,
    onPrimary = TextPrimary,
    primaryContainer = BlueContainer,
    onPrimaryContainer = BlueAccentLight,
    secondary = BlueAccentLight,
    onSecondary = CharcoalBlack,
    secondaryContainer = BlueAccentPill,
    onSecondaryContainer = BlueAccentLight,
    background = CharcoalBlack,
    onBackground = TextPrimary,
    surface = CharcoalDark,
    onSurface = TextPrimary,
    surfaceVariant = CharcoalCard,
    onSurfaceVariant = TextSecondary,
    outline = CharcoalBorder,
    outlineVariant = CharcoalElevated,
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun PodcastsTheme(
    language: AppLanguage = AppLanguage.ENGLISH,
    content: @Composable () -> Unit
) {
    val colorScheme = PodcastsDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = CharcoalBlack.toArgb()
                window.navigationBarColor = CharcoalBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    val isRtl = (language == AppLanguage.PERSIAN)
    val appStrings = if (isRtl) PersianStrings else EnglishStrings
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalAppLanguage provides language,
        LocalAppStrings provides appStrings,
        LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
