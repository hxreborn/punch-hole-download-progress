package eu.hxreborn.phdp.ui.navigation

import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.OffsetPx
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.prefs.RotationSlot
import eu.hxreborn.phdp.prefs.bind
import eu.hxreborn.phdp.ui.MenuAction
import eu.hxreborn.phdp.ui.SettingsUiState
import eu.hxreborn.phdp.ui.SettingsViewModel
import eu.hxreborn.phdp.ui.component.MainTabScaffold
import eu.hxreborn.phdp.ui.screen.AppearanceScreen
import eu.hxreborn.phdp.ui.screen.BehaviorScreen
import eu.hxreborn.phdp.ui.screen.CalibrationScreen
import eu.hxreborn.phdp.ui.screen.CalibrationTarget
import eu.hxreborn.phdp.ui.screen.LayoutConfig
import eu.hxreborn.phdp.ui.screen.LicensesScreen
import eu.hxreborn.phdp.ui.screen.MaterialYouScreen
import eu.hxreborn.phdp.ui.screen.PackageSelectionScreen
import eu.hxreborn.phdp.ui.screen.SystemScreen
import eu.hxreborn.phdp.ui.screen.TextCalibrationScreen
import eu.hxreborn.phdp.ui.screen.TypographyConfig
import eu.hxreborn.phdp.ui.screen.rememberDisplayRotation
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
    data object AppIconCalibration : Screen

    @Serializable
    data object BadgeCalibration : Screen

    @Serializable
    data object MaterialYou : Screen

    @Serializable
    data object Licenses : Screen
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
private fun rememberScaledAnimDuration(): Int {
    val context = LocalContext.current
    return remember {
        val scale =
            runCatching {
                Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            }.getOrDefault(1f)
        (Tokens.ANIMATION_DURATION_MS * scale).toInt().coerceAtLeast(0)
    }
}

@Composable
fun MainNavDisplay(
    backStack: NavBackStack<NavKey>,
    viewModel: SettingsViewModel,
    onMenuAction: (MenuAction) -> Unit,
    bottomNavPadding: Dp,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                slideOutHorizontally(targetOffsetX = { -it })
        },
        popTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                slideOutHorizontally(targetOffsetX = { it })
        },
        entryProvider =
            entryProvider {
                entry<Screen.Design> {
                    MainTabScaffold(
                        onMenuAction = onMenuAction,
                        bottomNavPadding = bottomNavPadding,
                    ) { contentPadding ->
                        AppearanceScreen(
                            viewModel = viewModel,
                            onNavigateToCalibration = { target ->
                                backStack.add(
                                    when (target) {
                                        CalibrationTarget.RING -> Screen.Calibration
                                        CalibrationTarget.PERCENT -> Screen.PercentCalibration
                                        CalibrationTarget.FILENAME -> Screen.FilenameCalibration
                                        CalibrationTarget.APP_ICON -> Screen.AppIconCalibration
                                    },
                                )
                            },
                            onNavigateToMaterialYou = {
                                backStack.add(Screen.MaterialYou)
                            },
                            contentPadding = contentPadding,
                        )
                    }
                }
                entry<Screen.Calibration> {
                    CalibrationScreen(
                        viewModel = viewModel,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        bottomNavPadding = bottomNavPadding,
                    )
                }
                entry<Screen.PercentCalibration> {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val prefs = (uiState as? SettingsUiState.Success)?.prefs ?: return@entry
                    val rotation = rememberDisplayRotation()
                    val slot = RotationSlot.fromSurfaceRotation(rotation)
                    val current = prefs.percentTextOffsets[slot]
                    TextCalibrationScreen(
                        titleRes = R.string.pref_calibrate_percent_title,
                        offsetX = current.x,
                        offsetY = current.y,
                        onOffsetXChange = { newX ->
                            val updated = prefs.percentTextOffsets.with(slot, OffsetPx(newX, current.y))
                            viewModel.savePref(Prefs.percentTextOffsets, updated)
                        },
                        onOffsetYChange = { newY ->
                            val updated = prefs.percentTextOffsets.with(slot, OffsetPx(current.x, newY))
                            viewModel.savePref(Prefs.percentTextOffsets, updated)
                        },
                        onOffsetXReset = {
                            val updated = prefs.percentTextOffsets.with(slot, OffsetPx(0f, current.y))
                            viewModel.savePref(Prefs.percentTextOffsets, updated)
                        },
                        onOffsetYReset = {
                            val updated = prefs.percentTextOffsets.with(slot, OffsetPx(current.x, 0f))
                            viewModel.savePref(Prefs.percentTextOffsets, updated)
                        },
                        viewModel = viewModel,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        bottomNavPadding = bottomNavPadding,
                        typography =
                            TypographyConfig(
                                fontSize = Prefs.percentTextSize bind prefs.percentTextSize,
                                bold = Prefs.percentTextBold bind prefs.percentTextBold,
                                italic = Prefs.percentTextItalic bind prefs.percentTextItalic,
                            ),
                    )
                }
                entry<Screen.FilenameCalibration> {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val prefs = (uiState as? SettingsUiState.Success)?.prefs ?: return@entry
                    val rotation = rememberDisplayRotation()
                    val slot = RotationSlot.fromSurfaceRotation(rotation)
                    val current = prefs.filenameTextOffsets[slot]
                    TextCalibrationScreen(
                        titleRes = R.string.pref_calibrate_filename_title,
                        offsetX = current.x,
                        offsetY = current.y,
                        onOffsetXChange = { newX ->
                            val updated = prefs.filenameTextOffsets.with(slot, OffsetPx(newX, current.y))
                            viewModel.savePref(Prefs.filenameTextOffsets, updated)
                        },
                        onOffsetYChange = { newY ->
                            val updated = prefs.filenameTextOffsets.with(slot, OffsetPx(current.x, newY))
                            viewModel.savePref(Prefs.filenameTextOffsets, updated)
                        },
                        onOffsetXReset = {
                            val updated = prefs.filenameTextOffsets.with(slot, OffsetPx(0f, current.y))
                            viewModel.savePref(Prefs.filenameTextOffsets, updated)
                        },
                        onOffsetYReset = {
                            val updated = prefs.filenameTextOffsets.with(slot, OffsetPx(current.x, 0f))
                            viewModel.savePref(Prefs.filenameTextOffsets, updated)
                        },
                        viewModel = viewModel,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        bottomNavPadding = bottomNavPadding,
                        typography =
                            TypographyConfig(
                                fontSize = Prefs.filenameTextSize bind prefs.filenameTextSize,
                                bold = Prefs.filenameTextBold bind prefs.filenameTextBold,
                                italic = Prefs.filenameTextItalic bind prefs.filenameTextItalic,
                            ),
                        layout =
                            LayoutConfig(
                                truncateEnabled = Prefs.filenameTruncateEnabled bind prefs.filenameTruncateEnabled,
                                maxLength = Prefs.filenameMaxChars bind prefs.filenameMaxChars,
                                maxLengthTitleRes = R.string.pref_filename_max_chars_title,
                                ellipsize = Prefs.filenameEllipsize bind prefs.filenameEllipsize,
                                ellipsizeValues = listOf("start", "middle", "end"),
                                previewText = Prefs.previewFilenameText bind prefs.previewFilenameText,
                                verticalText = Prefs.filenameVerticalText bind prefs.filenameVerticalText,
                            ),
                    )
                }
                entry<Screen.AppIconCalibration> {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val prefs = (uiState as? SettingsUiState.Success)?.prefs ?: return@entry
                    val rotation = rememberDisplayRotation()
                    val slot = RotationSlot.fromSurfaceRotation(rotation)
                    val current = prefs.appIconOffsets[slot]
                    TextCalibrationScreen(
                        titleRes = R.string.pref_calibrate_app_icon_title,
                        offsetX = current.x,
                        offsetY = current.y,
                        onOffsetXChange = { newX ->
                            val updated = prefs.appIconOffsets.with(slot, OffsetPx(newX, current.y))
                            viewModel.savePref(Prefs.appIconOffsets, updated)
                        },
                        onOffsetYChange = { newY ->
                            val updated = prefs.appIconOffsets.with(slot, OffsetPx(current.x, newY))
                            viewModel.savePref(Prefs.appIconOffsets, updated)
                        },
                        onOffsetXReset = {
                            val updated = prefs.appIconOffsets.with(slot, OffsetPx(0f, current.y))
                            viewModel.savePref(Prefs.appIconOffsets, updated)
                        },
                        onOffsetYReset = {
                            val updated = prefs.appIconOffsets.with(slot, OffsetPx(current.x, 0f))
                            viewModel.savePref(Prefs.appIconOffsets, updated)
                        },
                        viewModel = viewModel,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        bottomNavPadding = bottomNavPadding,
                    )
                }
                entry<Screen.MaterialYou> {
                    MaterialYouScreen(
                        viewModel = viewModel,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        bottomNavPadding = bottomNavPadding,
                    )
                }
                entry<Screen.BadgeCalibration> {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val prefs = (uiState as? SettingsUiState.Success)?.prefs ?: return@entry
                    val rotation = rememberDisplayRotation()
                    val slot = RotationSlot.fromSurfaceRotation(rotation)
                    val current = prefs.badgeOffsets[slot]
                    TextCalibrationScreen(
                        titleRes = R.string.pref_calibrate_badge_title,
                        offsetX = current.x,
                        offsetY = current.y,
                        onOffsetXChange = { newX ->
                            val updated = prefs.badgeOffsets.with(slot, OffsetPx(newX, current.y))
                            viewModel.savePref(Prefs.badgeOffsets, updated)
                        },
                        onOffsetYChange = { newY ->
                            val updated = prefs.badgeOffsets.with(slot, OffsetPx(current.x, newY))
                            viewModel.savePref(Prefs.badgeOffsets, updated)
                        },
                        onOffsetXReset = {
                            val updated = prefs.badgeOffsets.with(slot, OffsetPx(0f, current.y))
                            viewModel.savePref(Prefs.badgeOffsets, updated)
                        },
                        onOffsetYReset = {
                            val updated = prefs.badgeOffsets.with(slot, OffsetPx(current.x, 0f))
                            viewModel.savePref(Prefs.badgeOffsets, updated)
                        },
                        viewModel = viewModel,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        bottomNavPadding = bottomNavPadding,
                        typography =
                            TypographyConfig(
                                fontSize = Prefs.badgeTextSize bind prefs.badgeTextSize,
                            ),
                    )
                }
                entry<Screen.Motion> {
                    MainTabScaffold(
                        onMenuAction = onMenuAction,
                        bottomNavPadding = bottomNavPadding,
                    ) { contentPadding ->
                        BehaviorScreen(
                            viewModel = viewModel,
                            onNavigateToBadgeCalibration = {
                                backStack.add(Screen.BadgeCalibration)
                            },
                            contentPadding = contentPadding,
                        )
                    }
                }
                entry<Screen.Packages> {
                    MainTabScaffold(
                        onMenuAction = onMenuAction,
                        bottomNavPadding = bottomNavPadding,
                    ) { contentPadding ->
                        PackageSelectionScreen(
                            viewModel = viewModel,
                            contentPadding = contentPadding,
                        )
                    }
                }
                entry<Screen.System> {
                    MainTabScaffold(
                        onMenuAction = onMenuAction,
                        bottomNavPadding = bottomNavPadding,
                    ) { contentPadding ->
                        SystemScreen(
                            viewModel = viewModel,
                            onNavigateToLicenses = { backStack.add(Screen.Licenses) },
                            contentPadding = contentPadding,
                        )
                    }
                }
                entry<Screen.Licenses> {
                    LicensesScreen(
                        onBack = { backStack.removeLastOrNull() },
                        bottomNavPadding = bottomNavPadding,
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
    val haptics = LocalHapticFeedback.current
    val animDuration = rememberScaledAnimDuration()

    val effectiveKey =
        when (currentKey) {
            Screen.Calibration,
            Screen.PercentCalibration,
            Screen.FilenameCalibration,
            Screen.AppIconCalibration,
            Screen.MaterialYou,
            -> Screen.Design

            Screen.BadgeCalibration -> Screen.Motion

            Screen.Licenses -> Screen.System

            else -> currentKey
        }

    val selectedIndex =
        bottomNavItems
            .indexOfFirst { it.key == effectiveKey }
            .coerceAtLeast(0)

    val transparentIndicatorColors =
        NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)

    CompositionLocalProvider(
        LocalNavigationBarSelectedIndex provides selectedIndex,
        LocalNavigationBarItemCount provides bottomNavItems.size,
    ) {
        NavigationBar(modifier = modifier) {
            bottomNavItems.forEach { item ->
                val selected = effectiveKey == item.key
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
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
                    colors = transparentIndicatorColors,
                )
            }
        }
    }
}
