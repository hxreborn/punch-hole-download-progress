package eu.hxreborn.phdp.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
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
    onClearDownloads: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            snapAnimationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        )
    var menuExpanded by remember { mutableStateOf(false) }
    val collapsedFraction = scrollBehavior.state.collapsedFraction
    val isCollapsed = collapsedFraction >= 1f
    val titleScale = lerp(1.25f, 1f, collapsedFraction)

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        maxLines = 2,
                        softWrap = true,
                        lineHeight = 32.sp,
                        modifier =
                            Modifier
                                .padding(end = 16.dp)
                                .graphicsLayer {
                                    scaleX = titleScale
                                    scaleY = titleScale
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                },
                        style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
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
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.restart_systemui)) },
                                    onClick = {
                                        menuExpanded = false
                                        onMenuAction(MenuAction.RestartSystemUI)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.reset_defaults)) },
                                    onClick = {
                                        menuExpanded = false
                                        onMenuAction(MenuAction.Reset)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.RestartAlt, contentDescription = null)
                                    },
                                )
                            }
                        }
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
            onClearDownloads = onClearDownloads,
            contentPadding = paddingValues,
        )
    }
}
