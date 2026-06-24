package eu.hxreborn.phdp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.rememberNavBackStack
import eu.hxreborn.phdp.ui.navigation.BottomNav
import eu.hxreborn.phdp.ui.navigation.MainNavDisplay
import eu.hxreborn.phdp.ui.navigation.Screen
import eu.hxreborn.phdp.ui.navigation.bottomNavItems

sealed class MenuAction {
    data object RestartSystemUI : MenuAction()

    data object Reset : MenuAction()
}

@Composable
fun PunchHoleProgressContent(
    viewModel: SettingsViewModel,
    onMenuAction: (MenuAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(Screen.Design)
    val currentKey = backStack.lastOrNull() as? Screen
    val isTopLevel = bottomNavItems.any { it.key == currentKey }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val floatingNavBar = (uiState as? SettingsUiState.Success)?.prefs?.floatingNavBar ?: false

    val slide by animateFloatAsState(
        targetValue = if (isTopLevel) 0f else 1f,
        label = "bottomBarSlide",
    )

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Box(modifier = Modifier.graphicsLayer { translationY = size.height * slide }) {
                BottomNav(
                    backStack = backStack,
                    currentKey = currentKey,
                    floating = floatingNavBar,
                )
            }
        },
    ) { outerPadding ->
        MainNavDisplay(
            backStack = backStack,
            viewModel = viewModel,
            onMenuAction = onMenuAction,
            bottomNavPadding = outerPadding.calculateBottomPadding() * (1f - slide),
        )
    }
}
