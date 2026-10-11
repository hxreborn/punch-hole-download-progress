package eu.hxreborn.phdp.ui.component.preference

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * False disables every preference composed under it. Duo Status Bar mode uses this for the
 * controls that only place a ring around the camera cutout.
 */
val LocalPreferencesEnabled = staticCompositionLocalOf { true }

@Composable
fun preferenceEnabled(enabled: Boolean): Boolean = enabled && LocalPreferencesEnabled.current

@Composable
fun IrrelevantPreferences(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalPreferencesEnabled provides enabled, content = content)
}
