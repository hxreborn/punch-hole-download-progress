package eu.hxreborn.phdp.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.ui.MenuAction
import eu.hxreborn.phdp.ui.theme.Tokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabScaffold(
    onMenuAction: (MenuAction) -> Unit,
    bottomNavPadding: Dp,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
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
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
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
        },
    ) { innerPadding ->
        content(
            PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = bottomNavPadding,
            ),
        )
    }
}
