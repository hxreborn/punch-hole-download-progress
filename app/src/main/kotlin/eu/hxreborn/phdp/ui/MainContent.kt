package eu.hxreborn.phdp.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberNavBackStack
import eu.hxreborn.phdp.ui.navigation.BottomNav
import eu.hxreborn.phdp.ui.navigation.MainNavDisplay
import eu.hxreborn.phdp.ui.navigation.Screen

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

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            BottomNav(
                backStack = backStack,
                currentKey = currentKey,
            )
        },
    ) { outerPadding ->
        MainNavDisplay(
            backStack = backStack,
            viewModel = viewModel,
            onMenuAction = onMenuAction,
            bottomNavPadding = outerPadding.calculateBottomPadding(),
        )
    }
}
