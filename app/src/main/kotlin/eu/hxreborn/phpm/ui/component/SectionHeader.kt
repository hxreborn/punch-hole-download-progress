package eu.hxreborn.phpm.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.hxreborn.phpm.ui.theme.Tokens

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            modifier.padding(
                start = Tokens.SectionHeaderStartPadding,
                top = Tokens.SpacingLg,
                bottom = Tokens.SpacingSm,
            ),
    )
}
