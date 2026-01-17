package eu.hxreborn.phdp.ui.component

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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import eu.hxreborn.phdp.ui.theme.Tokens

private enum class RowPosition { SINGLE, FIRST, MIDDLE, LAST }

private fun RowPosition.shape(): Shape {
    val large = 24.dp
    val small = 4.dp
    return when (this) {
        RowPosition.SINGLE -> RoundedCornerShape(large)
        RowPosition.FIRST -> RoundedCornerShape(large, large, small, small)
        RowPosition.MIDDLE -> RoundedCornerShape(small)
        RowPosition.LAST -> RoundedCornerShape(small, small, large, large)
    }
}

private fun positionOf(
    index: Int,
    count: Int,
) = when {
    count == 1 -> RowPosition.SINGLE
    index == 0 -> RowPosition.FIRST
    index == count - 1 -> RowPosition.LAST
    else -> RowPosition.MIDDLE
}

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable SettingsGroupScope.() -> Unit,
) {
    val scope = SettingsGroupScopeImpl().apply { content() }

    Column(
        modifier =
            modifier
                .padding(horizontal = Tokens.ScreenHorizontalPadding, vertical = Tokens.GroupSpacing)
                .alpha(if (enabled) 1f else Tokens.DISABLED_ALPHA),
        verticalArrangement = Arrangement.spacedBy(Tokens.RowGap),
    ) {
        scope.items.forEachIndexed { index, item ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = positionOf(index, scope.items.size).shape(),
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
