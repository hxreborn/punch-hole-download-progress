package eu.hxreborn.phdp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColors(
    val diffAdded: Color,
)

val diffAddedLight = Color(0xFF006D43)
val diffAddedDark = Color(0xFF52DF9A)

val lightExtendedColors = ExtendedColors(diffAdded = diffAddedLight)
val darkExtendedColors = ExtendedColors(diffAdded = diffAddedDark)

val LocalExtendedColors = staticCompositionLocalOf { lightExtendedColors }

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current
