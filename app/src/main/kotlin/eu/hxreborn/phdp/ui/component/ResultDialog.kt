package eu.hxreborn.phdp.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.ui.theme.Tokens
import java.text.NumberFormat

internal fun format(value: Number): String = NumberFormat.getInstance().format(value)

enum class ResultTone {
    Positive,
    Neutral,
    Alert,
}

@Immutable
data class ResultItem(
    val name: String,
    val detail: String? = null,
)

@Immutable
data class ResultGroup(
    val label: String,
    val icon: ImageVector,
    val tone: ResultTone,
    val items: List<ResultItem>,
)

@Composable
fun ResultDialog(
    icon: ImageVector,
    title: String,
    headline: String,
    groups: List<ResultGroup>,
    metadata: String? = null,
    alert: String? = null,
    onDismiss: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Tokens.DialogShape,
        icon = { Icon(imageVector = icon, contentDescription = null) },
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item(key = "summary") {
                    Summary(headline = headline, metadata = metadata, alert = alert, divided = groups.isNotEmpty())
                }
                groups.forEach { group ->
                    item(key = "group:${group.label}") {
                        GroupRow(
                            group = group,
                            expanded = expanded == group.label,
                            onClick = { expanded = group.label.takeIf { it != expanded } },
                        )
                    }
                    if (expanded == group.label) {
                        groupDetails(group)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
        },
    )
}

@Composable
private fun Summary(
    headline: String,
    metadata: String?,
    alert: String?,
    divided: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.SpacingSm / 2)) {
        Text(
            text = headline,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        metadata?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        alert?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (divided) {
            HorizontalDivider(modifier = Modifier.padding(top = Tokens.SpacingSm))
        }
    }
}

@Composable
private fun GroupRow(
    group: ResultGroup,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val tone = group.tone.color()
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) CHEVRON_EXPANDED_ROTATION else 0f,
        label = "chevron",
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable(
                    onClickLabel =
                        stringResource(
                            if (expanded) R.string.result_hide_details else R.string.result_show_details,
                        ),
                    onClick = onClick,
                ).padding(vertical = Tokens.SpacingSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.ResultRowSpacing),
    ) {
        Icon(
            imageVector = group.icon,
            contentDescription = null,
            tint = tone,
            modifier = Modifier.size(Tokens.ResultIconSize),
        )
        Text(
            text = group.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = format(group.items.size),
            style = MaterialTheme.typography.labelLarge,
            color = tone,
        )
        Icon(
            imageVector = Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Tokens.ResultIconSize).rotate(chevronRotation),
        )
    }
}

private fun LazyListScope.groupDetails(group: ResultGroup) {
    items(items = group.items, key = { "${group.label}:${it.name}" }) { DetailRow(it) }
}

@Composable
private fun DetailRow(item: ResultItem) {
    Column(
        modifier =
            Modifier.padding(
                start = Tokens.ResultDetailIndent,
                bottom = Tokens.SpacingSm,
            ),
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        item.detail?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResultTone.color(): Color =
    when (this) {
        ResultTone.Positive -> MaterialTheme.colorScheme.primary
        ResultTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        ResultTone.Alert -> MaterialTheme.colorScheme.error
    }

private const val CHEVRON_EXPANDED_ROTATION = 180f
