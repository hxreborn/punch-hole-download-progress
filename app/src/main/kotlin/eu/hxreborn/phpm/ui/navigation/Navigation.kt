package eu.hxreborn.phpm.ui.navigation

import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SlowMotionVideo
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
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import eu.hxreborn.phpm.R
import eu.hxreborn.phpm.ui.screen.DesignScreen
import eu.hxreborn.phpm.ui.screen.MotionScreen
import eu.hxreborn.phpm.ui.screen.SystemScreen
import eu.hxreborn.phpm.ui.state.PrefsState
import eu.hxreborn.phpm.ui.theme.Tokens

sealed class Screen(
    val route: String,
) {
    data object Design : Screen("design")

    data object Motion : Screen("motion")

    data object System : Screen("system")
}

data class BottomNavItem(
    val route: String,
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems =
    listOf(
        BottomNavItem(
            route = Screen.Design.route,
            titleRes = R.string.tab_design,
            selectedIcon = Icons.Filled.Palette,
            unselectedIcon = Icons.Outlined.Palette,
        ),
        BottomNavItem(
            route = Screen.Motion.route,
            titleRes = R.string.tab_motion,
            selectedIcon = Icons.Filled.SlowMotionVideo,
            unselectedIcon = Icons.Outlined.SlowMotionVideo,
        ),
        BottomNavItem(
            route = Screen.System.route,
            titleRes = R.string.tab_system,
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
        ),
    )

@Composable
fun MainNavHost(
    navController: NavHostController,
    prefsState: PrefsState,
    onSavePrefs: (key: String, value: Any) -> Unit,
    onTestSuccess: () -> Unit,
    onTestFailure: () -> Unit,
    onPreviewAnimation: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Design.route,
        modifier = modifier,
        enterTransition = { fadeIn(tween(Tokens.ANIMATION_DURATION_MS)) },
        exitTransition = { fadeOut(tween(Tokens.ANIMATION_DURATION_MS)) },
        popEnterTransition = { fadeIn(tween(Tokens.ANIMATION_DURATION_MS)) },
        popExitTransition = { fadeOut(tween(Tokens.ANIMATION_DURATION_MS)) },
    ) {
        composable(Screen.Design.route) {
            DesignScreen(
                prefsState = prefsState,
                onSavePrefs = onSavePrefs,
                contentPadding = contentPadding,
            )
        }
        composable(Screen.Motion.route) {
            MotionScreen(
                prefsState = prefsState,
                onSavePrefs = onSavePrefs,
                onPreviewAnimation = onPreviewAnimation,
                contentPadding = contentPadding,
            )
        }
        composable(Screen.System.route) {
            SystemScreen(
                prefsState = prefsState,
                onSavePrefs = onSavePrefs,
                onTestSuccess = onTestSuccess,
                onTestFailure = onTestFailure,
                contentPadding = contentPadding,
            )
        }
    }
}

@Composable
fun BottomNav(
    navController: NavController,
    currentRoute: String?,
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
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
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
