package eu.hxreborn.phpm.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.hxreborn.phpm.ui.theme.Tokens

@Composable
fun TweakButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .padding(
                    horizontal = Tokens.SettingsRowHorizontalPadding,
                    vertical = Tokens.SettingsRowVerticalPadding,
                ).fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
