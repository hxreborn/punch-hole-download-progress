package eu.hxreborn.phdp.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.ui.theme.Tokens

@Composable
fun TestButtonsRow(
    onSimulateSuccess: () -> Unit,
    onSimulateFailure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = Tokens.CardShape,
        tonalElevation = 1.dp,
        modifier =
            modifier.padding(
                horizontal = Tokens.SectionHorizontalMargin,
                vertical = Tokens.SpacingLg,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TextButton(onClick = onSimulateSuccess) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(Tokens.SpacingSm))
                Text(stringResource(R.string.test_preview_completion))
            }
            TextButton(onClick = onSimulateFailure) {
                Icon(
                    imageVector = Icons.Outlined.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(Tokens.SpacingSm))
                Text(stringResource(R.string.test_preview_error))
            }
        }
    }
}
