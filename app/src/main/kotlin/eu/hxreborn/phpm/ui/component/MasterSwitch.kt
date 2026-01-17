package eu.hxreborn.phpm.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import eu.hxreborn.phpm.ui.theme.Tokens

@Composable
fun MasterSwitch(
    title: String,
    summaryOn: String,
    summaryOff: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val backgroundColor by animateColorAsState(
        targetValue =
            if (checked) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
        animationSpec = tween(Tokens.ANIMATION_DURATION_MS),
        label = "masterSwitchBackground",
    )

    val contentColor =
        if (checked) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    ElevatedCard(
        modifier =
            modifier
                .padding(
                    horizontal = Tokens.ScreenHorizontalPadding,
                    vertical = Tokens.GroupMarginVertical,
                ).fillMaxWidth(),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = backgroundColor,
            ),
        shape = RoundedCornerShape(Tokens.GroupCornerRadius),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onCheckedChange(!checked) }
                    .padding(
                        horizontal = Tokens.SpacingXl,
                        vertical = Tokens.ListItemHorizontalPadding,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.padding(end = Tokens.ListItemLeadingSpacing),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                )
                Text(
                    text = if (checked) summaryOn else summaryOff,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = Tokens.MEDIUM_EMPHASIS_ALPHA),
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}
