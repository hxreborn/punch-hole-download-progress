package eu.hxreborn.phdp.ui.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Modifier.verticalScrollbar(state: ScrollableState): Modifier =
    state.scrollIndicatorState?.let { nonInteractiveScrollbar(it, Orientation.Vertical) } ?: this

@Composable
fun BoxScope.VerticalScrollbar(
    state: ScrollableState,
    contentPadding: PaddingValues,
) {
    Box(Modifier.matchParentSize().padding(contentPadding).verticalScrollbar(state))
}
