package eu.hxreborn.phdp.ui.component.preference

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.hxreborn.phdp.ui.theme.Tokens

internal val PreferenceIconWidth = Tokens.PreferenceIconContainerMinWidth - Tokens.PreferencePadding

@Composable
internal fun PreferenceIcon(
    icon: @Composable () -> Unit,
    enabled: Boolean,
) {
    Box(
        modifier = Modifier.width(PreferenceIconWidth),
        contentAlignment = Alignment.CenterStart,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else Tokens.DISABLED_ALPHA),
            content = icon,
        )
    }
}
