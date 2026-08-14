package eu.hxreborn.phdp.ui.component

import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import eu.hxreborn.phdp.ui.theme.Tokens

val LocalSnackbarInset = compositionLocalOf<() -> Dp> { { 0.dp } }

@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val inset = LocalSnackbarInset.current
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.offset { IntOffset(x = 0, y = -inset().roundToPx()) },
    ) { data ->
        Snackbar(snackbarData = data, shape = Tokens.SnackbarShape)
    }
}
