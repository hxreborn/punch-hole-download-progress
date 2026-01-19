package eu.hxreborn.phdp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.ui.navigation.BottomNav
import eu.hxreborn.phdp.ui.navigation.MainNavHost
import eu.hxreborn.phdp.ui.state.PrefsState
import eu.hxreborn.phdp.ui.theme.Tokens

sealed class MenuAction {
    data object RestartSystemUI : MenuAction()

    data object Reset : MenuAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PunchHoleProgressContent(
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Punch-hole",
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            softWrap = true,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.titleSmall,
                            color = LocalContentColor.current.copy(alpha = Tokens.MEDIUM_EMPHASIS_ALPHA),
                            maxLines = 1,
                            softWrap = true,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
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
