package eu.hxreborn.phpm.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import eu.hxreborn.phpm.ui.theme.Tokens

// ColorBlendr style: rows with position-based corners to appear connected
private val cornerLarge = 24.dp
private val cornerSmall = 4.dp

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable SettingsGroupScope.() -> Unit,
) {
    val scope = SettingsGroupScopeImpl()
    scope.content()
    val count = scope.items.size

    Column(
        modifier =
            modifier
                .padding(
                    horizontal = Tokens.ScreenHorizontalPadding,
                    vertical = Tokens.GroupSpacing,
                ).alpha(if (enabled) 1f else Tokens.DISABLED_ALPHA),
        verticalArrangement = Arrangement.spacedBy(Tokens.RowGap),
    ) {
        scope.items.forEachIndexed { index, item ->
            val shape = when {
                count == 1 -> RoundedCornerShape(cornerLarge)
                index == 0 -> RoundedCornerShape(cornerLarge, cornerLarge, cornerSmall, cornerSmall)
                index == count - 1 -> RoundedCornerShape(cornerSmall, cornerSmall, cornerLarge, cornerLarge)
                else -> RoundedCornerShape(cornerSmall)
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                item()
            }
        }
    }
}

interface SettingsGroupScope {
    fun item(content: @Composable () -> Unit)
}

private class SettingsGroupScopeImpl : SettingsGroupScope {
    val items = mutableListOf<@Composable () -> Unit>()

    override fun item(content: @Composable () -> Unit) {
        items.add(content)
    }
}
