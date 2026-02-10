package eu.hxreborn.phdp.ui.component.preference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.Tokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextStyleChipsPreference(
    bold: Boolean,
    onBoldChange: (Boolean) -> Unit,
    italic: Boolean,
    onItalicChange: (Boolean) -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(Tokens.PreferencePadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
        ) {
            ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                title()
            }
        }
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            FilterChip(
                selected = bold,
                onClick = { onBoldChange(!bold) },
                label = { Text(stringResource(R.string.pref_bold_title)) },
                leadingIcon =
                    if (bold) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
            )
            FilterChip(
                selected = italic,
                onClick = { onItalicChange(!italic) },
                label = { Text(stringResource(R.string.pref_italic_title)) },
                leadingIcon =
                    if (italic) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
            )
        }
    }
}

@Preview
@Composable
private fun TextStyleChipsPreferencePreview() {
    AppTheme {
        TextStyleChipsPreference(
            bold = true,
            onBoldChange = {},
            italic = false,
            onItalicChange = {},
            title = { Text(stringResource(R.string.pref_text_style_title)) },
        )
    }
}
