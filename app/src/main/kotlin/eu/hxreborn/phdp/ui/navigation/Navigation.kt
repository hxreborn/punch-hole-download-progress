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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.prefs.bind
import eu.hxreborn.phdp.ui.SettingsUiState
import eu.hxreborn.phdp.ui.SettingsViewModel
import eu.hxreborn.phdp.ui.screen.AppearanceScreen
import eu.hxreborn.phdp.ui.screen.BehaviorScreen
import eu.hxreborn.phdp.ui.screen.CalibrationScreen
import eu.hxreborn.phdp.ui.screen.CalibrationTarget
import eu.hxreborn.phdp.ui.screen.LayoutConfig
import eu.hxreborn.phdp.ui.screen.PackageSelectionScreen
import eu.hxreborn.phdp.ui.screen.SystemScreen
import eu.hxreborn.phdp.ui.screen.TextCalibrationScreen
import eu.hxreborn.phdp.ui.screen.TypographyConfig
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

private val slideTransitionMetadata =
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
        }

@Composable
fun MainNavDisplay(
    backStack: NavBackStack<NavKey>,
    viewModel: SettingsViewModel,
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
                        viewModel = viewModel,
                        onNavigateToCalibration = { target ->
                            backStack.add(
                                when (target) {
                                    CalibrationTarget.RING -> Screen.Calibration
                                    CalibrationTarget.PERCENT -> Screen.PercentCalibration
                                    CalibrationTarget.FILENAME -> Screen.FilenameCalibration
                                },
                            )
                        },
                        contentPadding = contentPadding,
                    )
                }
                entry<Screen.Calibration>(metadata = slideTransitionMetadata) {
                    CalibrationScreen(
                        viewModel = viewModel,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        contentPadding = contentPadding,
                    )
                }
                entry<Screen.PercentCalibration>(metadata = slideTransitionMetadata) {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val prefs = (uiState as? SettingsUiState.Success)?.prefs ?: return@entry
                    TextCalibrationScreen(
                        titleRes = R.string.pref_calibrate_percent_title,
                        offsetX = Prefs.percentTextOffsetX bind prefs.percentTextOffsetX,
                        offsetY = Prefs.percentTextOffsetY bind prefs.percentTextOffsetY,
                        viewModel = viewModel,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        contentPadding = contentPadding,
                        typography =
                            TypographyConfig(
                                fontSize = Prefs.percentTextSize bind prefs.percentTextSize,
                                bold = Prefs.percentTextBold bind prefs.percentTextBold,
                                italic = Prefs.percentTextItalic bind prefs.percentTextItalic,
                            ),
                    )
                }
                entry<Screen.FilenameCalibration>(metadata = slideTransitionMetadata) {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val prefs = (uiState as? SettingsUiState.Success)?.prefs ?: return@entry
                    TextCalibrationScreen(
                        titleRes = R.string.pref_calibrate_filename_title,
                        offsetX = Prefs.filenameTextOffsetX bind prefs.filenameTextOffsetX,
                        offsetY = Prefs.filenameTextOffsetY bind prefs.filenameTextOffsetY,
                        viewModel = viewModel,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        contentPadding = contentPadding,
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
                            ),
                    )
                }
                entry<Screen.BadgeCalibration>(metadata = slideTransitionMetadata) {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val prefs = (uiState as? SettingsUiState.Success)?.prefs ?: return@entry
                    TextCalibrationScreen(
                        titleRes = R.string.pref_calibrate_badge_title,
                        offsetX = Prefs.badgeOffsetX bind prefs.badgeOffsetX,
                        offsetY = Prefs.badgeOffsetY bind prefs.badgeOffsetY,
                        viewModel = viewModel,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        contentPadding = contentPadding,
                        typography =
                            TypographyConfig(
                                fontSize = Prefs.badgeTextSize bind prefs.badgeTextSize,
                            ),
                    )
                }
                entry<Screen.Motion> {
                    BehaviorScreen(
                        viewModel = viewModel,
                        onNavigateToBadgeCalibration = {
                            backStack.add(Screen.BadgeCalibration)
                        },
                        contentPadding = contentPadding,
                    )
                }
                entry<Screen.Packages> {
                    PackageSelectionScreen(
                        viewModel = viewModel,
                        contentPadding = contentPadding,
                    )
                }
                entry<Screen.System> {
                    SystemScreen(
                        viewModel = viewModel,
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
