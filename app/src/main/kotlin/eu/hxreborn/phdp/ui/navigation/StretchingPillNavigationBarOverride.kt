package eu.hxreborn.phdp.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarOverride
import androidx.compose.material3.NavigationBarOverrideScope
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import eu.hxreborn.phdp.ui.theme.Tokens
import kotlinx.coroutines.launch

internal val LocalNavigationBarSelectedIndex: ProvidableCompositionLocal<Int> =
    compositionLocalOf { 0 }

internal val LocalNavigationBarItemCount: ProvidableCompositionLocal<Int> =
    compositionLocalOf { 0 }

@OptIn(ExperimentalMaterial3ComponentOverrideApi::class)
object StretchingPillNavigationBarOverride : NavigationBarOverride {
    @Composable
    override fun NavigationBarOverrideScope.NavigationBar() {
        val selectedIndex = LocalNavigationBarSelectedIndex.current
        val itemCount = LocalNavigationBarItemCount.current.coerceAtLeast(1)
        val motionScheme = MaterialTheme.motionScheme
        val pillColor = MaterialTheme.colorScheme.secondaryContainer

        Surface(
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            modifier = modifier,
        ) {
            BoxWithConstraints(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(windowInsets)
                        .height(Tokens.NavBarHeight),
            ) {
                val totalWidth = maxWidth
                val totalSpacing = Tokens.NavBarItemSpacing * (itemCount - 1)
                val itemWidth = (totalWidth - totalSpacing) / itemCount

                fun pillStartFor(index: Int): Dp = (itemWidth + Tokens.NavBarItemSpacing) * index + (itemWidth - Tokens.NavBarPillWidth) / 2

                fun pillEndFor(index: Int): Dp = pillStartFor(index) + Tokens.NavBarPillWidth

                val initialIndex = remember { selectedIndex }
                val startEdge =
                    remember { Animatable(pillStartFor(initialIndex), Dp.VectorConverter) }
                val endEdge =
                    remember { Animatable(pillEndFor(initialIndex), Dp.VectorConverter) }

                LaunchedEffect(selectedIndex, itemWidth) {
                    val newStart = pillStartFor(selectedIndex)
                    val newEnd = pillEndFor(selectedIndex)
                    val movingRight = newStart > startEdge.targetValue
                    val fast = motionScheme.fastSpatialSpec<Dp>()
                    val slow = motionScheme.slowSpatialSpec<Dp>()
                    launch { startEdge.animateTo(newStart, if (movingRight) slow else fast) }
                    launch { endEdge.animateTo(newEnd, if (movingRight) fast else slow) }
                }

                Box(
                    modifier =
                        Modifier
                            .offset { IntOffset(startEdge.value.roundToPx(), Tokens.NavBarPillTopOffset.roundToPx()) }
                            .width((endEdge.value - startEdge.value).coerceAtLeast(0.dp))
                            .height(Tokens.NavBarPillHeight)
                            .background(color = pillColor, shape = RoundedCornerShape(50)),
                )

                Row(
                    modifier = Modifier.fillMaxSize().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.NavBarItemSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }
    }
}
