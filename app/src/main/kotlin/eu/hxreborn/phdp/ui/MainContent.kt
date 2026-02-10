package eu.hxreborn.phdp.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.rememberNavBackStack
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.ui.component.OverflowMenu
import eu.hxreborn.phdp.ui.component.OverflowMenuItem
import eu.hxreborn.phdp.ui.navigation.BottomNav
import eu.hxreborn.phdp.ui.navigation.MainNavDisplay
import eu.hxreborn.phdp.ui.navigation.Screen
import eu.hxreborn.phdp.ui.theme.Tokens

sealed class MenuAction {
    data object RestartSystemUI : MenuAction()

    data object Reset : MenuAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PunchHoleProgressContent(
    viewModel: SettingsViewModel,
    onMenuAction: (MenuAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(Screen.Design)
    val currentKey = backStack.lastOrNull() as? Screen
    val showMainAppBar =
        currentKey != Screen.Calibration &&
            currentKey != Screen.PercentCalibration &&
            currentKey != Screen.FilenameCalibration &&
            currentKey != Screen.BadgeCalibration
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            snapAnimationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
        )
    var menuExpanded by remember { mutableStateOf(false) }
    val isCollapsed by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction >= 1f }
    }

    Scaffold(
        modifier =
            if (showMainAppBar) {
                modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            } else {
                modifier
            },
        topBar = {
            if (showMainAppBar) {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    },
                    expandedHeight = Tokens.LargeAppBarExpandedHeight,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        if (isCollapsed) {
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = stringResource(R.string.more_options),
                                    )
                                }
                                OverflowMenu(
                                    expanded = menuExpanded,
                                    onDismiss = { menuExpanded = false },
                                ) {
                                    OverflowMenuItem(
                                        text = stringResource(R.string.restart_systemui),
                                        icon = Icons.Default.Refresh,
                                        onClick = {
                                            menuExpanded = false
                                            onMenuAction(MenuAction.RestartSystemUI)
                                        },
                                    )
                                    OverflowMenuItem(
                                        text = stringResource(R.string.reset_defaults),
                                        icon = Icons.Default.RestartAlt,
                                        onClick = {
                                            menuExpanded = false
                                            onMenuAction(MenuAction.Reset)
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            BottomNav(
                backStack = backStack,
                currentKey = currentKey,
            )
        },
    ) { paddingValues ->
        MainNavDisplay(
            backStack = backStack,
            viewModel = viewModel,
            contentPadding = paddingValues,
        )
    }
}
