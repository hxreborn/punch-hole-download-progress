package eu.hxreborn.phdp.ui.navigation

import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.ui.screen.AppearanceScreen
import eu.hxreborn.phdp.ui.screen.BehaviorScreen
import eu.hxreborn.phdp.ui.screen.CalibrationScreen
import eu.hxreborn.phdp.ui.screen.PackageSelectionScreen
import eu.hxreborn.phdp.ui.screen.SystemScreen
import eu.hxreborn.phdp.ui.screen.TextCalibrationScreen
import eu.hxreborn.phdp.ui.state.PrefsState
import eu.hxreborn.phdp.ui.theme.Tokens
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
    @Serializable
    data object Design : Screen

    @Serializable
    data object Motion : Screen

    @Serializable
    data object Packages : Screen

    @Serializable
    data object System : Screen

    @Serializable
    data object Calibration : Screen

    @Serializable
    data object PercentCalibration : Screen

    @Serializable
    data object FilenameCalibration : Screen

    @Serializable
    data object BadgeCalibration : Screen
}

data class BottomNavItem(
    val key: Screen,
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems =
    listOf(
        BottomNavItem(
            key = Screen.Design,
            titleRes = R.string.tab_design,
            selectedIcon = Icons.Filled.Palette,
            unselectedIcon = Icons.Outlined.Palette,
        ),
        BottomNavItem(
            key = Screen.Motion,
            titleRes = R.string.tab_motion,
            selectedIcon = Icons.Filled.AutoAwesomeMotion,
            unselectedIcon = Icons.Outlined.AutoAwesomeMotion,
        ),
        BottomNavItem(
            key = Screen.Packages,
            titleRes = R.string.tab_packages,
            selectedIcon = Icons.Filled.Dashboard,
            unselectedIcon = Icons.Outlined.Dashboard,
        ),
        BottomNavItem(
            key = Screen.System,
            titleRes = R.string.tab_system,
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
        ),
    )

@Composable
fun MainNavDisplay(
    backStack: NavBackStack<NavKey>,
    prefsState: PrefsState,
    onSavePrefs: (key: String, value: Any) -> Unit,
    onTestSuccess: () -> Unit,
    onTestFailure: () -> Unit,
    onClearDownloads: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        entryProvider =
            entryProvider {
                entry<Screen.Design> {
                    AppearanceScreen(
                        prefsState = prefsState,
                        onSavePrefs = onSavePrefs,
                        onNavigateToCalibration = { backStack.add(Screen.Calibration) },
                        onNavigateToPercentCalibration = {
                            backStack.add(Screen.PercentCalibration)
                        },
                        onNavigateToFilenameCalibration = {
                            backStack.add(Screen.FilenameCalibration)
                        },
                        contentPadding = contentPadding,
                    )
                }
                entry<Screen.Calibration>(
                    metadata =
                        NavDisplay.transitionSpec {
                            slideInHorizontally(initialOffsetX = { it }) togetherWith
                                slideOutHorizontally(targetOffsetX = { -it })
                        } +
                            NavDisplay.popTransitionSpec {
                                slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                    slideOutHorizontally(targetOffsetX = { it })
                            } +
                            NavDisplay.predictivePopTransitionSpec {
                                slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                    slideOutHorizontally(targetOffsetX = { it })
                            },
                ) {
                    CalibrationScreen(
                        prefsState = prefsState,
                        onSavePrefs = onSavePrefs,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        contentPadding = contentPadding,
                    )
                }
                entry<Screen.PercentCalibration>(
                    metadata =
                        NavDisplay.transitionSpec {
                            slideInHorizontally(initialOffsetX = { it }) togetherWith
                                slideOutHorizontally(targetOffsetX = { -it })
                        } +
                            NavDisplay.popTransitionSpec {
                                slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                    slideOutHorizontally(targetOffsetX = { it })
                            } +
                            NavDisplay.predictivePopTransitionSpec {
                                slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                    slideOutHorizontally(targetOffsetX = { it })
                            },
                ) {
                    TextCalibrationScreen(
                        titleRes = R.string.pref_calibrate_percent_title,
                        offsetX = prefsState.percentTextOffsetX,
                        offsetY = prefsState.percentTextOffsetY,
                        offsetXPref = Prefs.percentTextOffsetX,
                        offsetYPref = Prefs.percentTextOffsetY,
                        onSavePrefs = onSavePrefs,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        contentPadding = contentPadding,
                    )
                }
                entry<Screen.FilenameCalibration>(
                    metadata =
                        NavDisplay.transitionSpec {
                            slideInHorizontally(initialOffsetX = { it }) togetherWith
                                slideOutHorizontally(targetOffsetX = { -it })
                        } +
                            NavDisplay.popTransitionSpec {
                                slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                    slideOutHorizontally(targetOffsetX = { it })
                            } +
                            NavDisplay.predictivePopTransitionSpec {
                                slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                    slideOutHorizontally(targetOffsetX = { it })
                            },
                ) {
                    TextCalibrationScreen(
                        titleRes = R.string.pref_calibrate_filename_title,
                        offsetX = prefsState.filenameTextOffsetX,
                        offsetY = prefsState.filenameTextOffsetY,
                        offsetXPref = Prefs.filenameTextOffsetX,
                        offsetYPref = Prefs.filenameTextOffsetY,
                        onSavePrefs = onSavePrefs,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        contentPadding = contentPadding,
                    )
                }
                entry<Screen.BadgeCalibration>(
                    metadata =
                        NavDisplay.transitionSpec {
                            slideInHorizontally(initialOffsetX = { it }) togetherWith
                                slideOutHorizontally(targetOffsetX = { -it })
                        } +
                            NavDisplay.popTransitionSpec {
                                slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                    slideOutHorizontally(targetOffsetX = { it })
                            } +
                            NavDisplay.predictivePopTransitionSpec {
                                slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                    slideOutHorizontally(targetOffsetX = { it })
                            },
                ) {
                    TextCalibrationScreen(
                        titleRes = R.string.pref_calibrate_badge_title,
                        offsetX = prefsState.badgeOffsetX,
                        offsetY = prefsState.badgeOffsetY,
                        offsetXPref = Prefs.badgeOffsetX,
                        offsetYPref = Prefs.badgeOffsetY,
                        onSavePrefs = onSavePrefs,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        contentPadding = contentPadding,
                    )
                }
                entry<Screen.Motion> {
                    BehaviorScreen(
                        prefsState = prefsState,
                        onSavePrefs = onSavePrefs,
                        onNavigateToBadgeCalibration = {
                            backStack.add(Screen.BadgeCalibration)
                        },
                        contentPadding = contentPadding,
                    )
                }
                entry<Screen.Packages> {
                    PackageSelectionScreen(
                        prefsState = prefsState,
                        onSavePrefs = onSavePrefs,
                        contentPadding = contentPadding,
                    )
                }
                entry<Screen.System> {
                    SystemScreen(
                        prefsState = prefsState,
                        onSavePrefs = onSavePrefs,
                        onTestSuccess = onTestSuccess,
                        onTestFailure = onTestFailure,
                        onClearDownloads = onClearDownloads,
                        contentPadding = contentPadding,
                    )
                }
            },
    )
}

@Composable
fun BottomNav(
    backStack: NavBackStack<NavKey>,
    currentKey: NavKey?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val animDuration =
        remember {
            val scale =
                runCatching {
                    Settings.Global.getFloat(
                        context.contentResolver,
                        Settings.Global.ANIMATOR_DURATION_SCALE,
                        1f,
                    )
                }.getOrDefault(1f)
            (Tokens.ANIMATION_DURATION_MS * scale).toInt().coerceAtLeast(0)
        }

    NavigationBar(modifier = modifier) {
        val effectiveKey =
            when (currentKey) {
                Screen.Calibration,
                Screen.PercentCalibration,
                Screen.FilenameCalibration,
                -> Screen.Design

                Screen.BadgeCalibration -> Screen.Motion

                else -> currentKey
            }
        bottomNavItems.forEach { item ->
            val selected = effectiveKey == item.key
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        backStack.removeAll { it != Screen.Design }
                        if (item.key != Screen.Design) {
                            backStack.add(item.key)
                        }
                    }
                },
                icon = {
                    Crossfade(
                        targetState = selected,
                        animationSpec = tween(durationMillis = animDuration),
                        label = "iconCrossfade",
                    ) { isSelected ->
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = stringResource(item.titleRes),
                        )
                    }
                },
                label = { Text(stringResource(item.titleRes)) },
                alwaysShowLabel = false,
            )
        }
    }
}
