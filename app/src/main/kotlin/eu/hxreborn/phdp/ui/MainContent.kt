package eu.hxreborn.phdp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.rememberNavBackStack
import eu.hxreborn.phdp.ui.component.LocalSnackbarInset
import eu.hxreborn.phdp.ui.navigation.BottomNav
import eu.hxreborn.phdp.ui.navigation.MainNavDisplay
import eu.hxreborn.phdp.ui.navigation.Screen
import eu.hxreborn.phdp.ui.navigation.bottomNavItems

sealed class MenuAction {
    data object RestartSystemUI : MenuAction()

    data object Reset : MenuAction()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val prefs = (uiState as? SettingsUiState.Success)?.prefs
    val floatingNavBar = prefs?.floatingNavBar ?: false
    val hideNavBarOnScroll = prefs?.hideNavBarOnScroll ?: false

    val slide by animateFloatAsState(
        targetValue = if (isTopLevel) 0f else 1f,
        label = "bottomBarSlide",
    )

    val scrollBehavior =
        if (floatingNavBar && hideNavBarOnScroll) {
            FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = FloatingToolbarExitDirection.Bottom)
        } else {
            null
        }

    Scaffold(
        modifier = if (scrollBehavior != null) modifier.nestedScroll(scrollBehavior) else modifier,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Box(modifier = Modifier.graphicsLayer { translationY = size.height * slide }) {
                BottomNav(
                    backStack = backStack,
                    currentKey = currentKey,
                    floating = floatingNavBar,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { outerPadding ->
        val bottomNavPadding = outerPadding.calculateBottomPadding() * (1f - slide)
        val systemInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        CompositionLocalProvider(
            LocalSnackbarInset provides {
                val state = scrollBehavior?.state
                val hidden =
                    if (state != null && state.offsetLimit < 0f) {
                        (state.offset / state.offsetLimit).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                maxOf(systemInset, bottomNavPadding * (1f - hidden))
            },
        ) {
            MainNavDisplay(
                backStack = backStack,
                viewModel = viewModel,
                onMenuAction = onMenuAction,
                bottomNavPadding = bottomNavPadding,
            )
        }
    }
}
