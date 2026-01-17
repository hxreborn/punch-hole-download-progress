package eu.hxreborn.phpm.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import eu.hxreborn.phpm.R
import eu.hxreborn.phpm.ui.navigation.BottomNav
import eu.hxreborn.phpm.ui.navigation.MainNavHost
import eu.hxreborn.phpm.ui.state.PrefsState

sealed class MenuAction {
    data object RestartSystemUI : MenuAction()

    data object Reset : MenuAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PunchHoleMonitorAppContent(
    prefsState: PrefsState,
    onSavePrefs: (key: String, value: Any) -> Unit,
    onMenuAction: (MenuAction) -> Unit,
    onTestSuccess: () -> Unit,
    onTestFailure: () -> Unit,
    onPreviewAnimation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { onMenuAction(MenuAction.RestartSystemUI) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.restart_systemui),
                        )
                    }
                    IconButton(onClick = { onMenuAction(MenuAction.Reset) }) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = stringResource(R.string.reset_defaults),
                        )
                    }
                },
            )
        },
        bottomBar = {
            BottomNav(
                navController = navController,
                currentRoute = currentRoute,
            )
        },
    ) { paddingValues ->
        MainNavHost(
            navController = navController,
            prefsState = prefsState,
            onSavePrefs = onSavePrefs,
            onTestSuccess = onTestSuccess,
            onTestFailure = onTestFailure,
            onPreviewAnimation = onPreviewAnimation,
            contentPadding = paddingValues,
        )
    }
}
