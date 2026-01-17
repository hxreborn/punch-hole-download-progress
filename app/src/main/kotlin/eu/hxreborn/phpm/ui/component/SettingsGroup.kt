package eu.hxreborn.phpm.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import eu.hxreborn.phpm.ui.theme.Tokens

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable SettingsGroupScope.() -> Unit,
) {
    val scope = SettingsGroupScopeImpl()
    scope.content()

    Surface(
        modifier =
            modifier
                .padding(
                    horizontal = Tokens.ScreenHorizontalPadding,
                    vertical = Tokens.GroupMarginVertical,
                ).fillMaxWidth()
                .alpha(if (enabled) 1f else Tokens.DISABLED_ALPHA),
        shape = RoundedCornerShape(Tokens.GroupCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            scope.items.forEachIndexed { index, item ->
                item()
                if (index < scope.items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = Tokens.ListItemHorizontalPadding),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
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
