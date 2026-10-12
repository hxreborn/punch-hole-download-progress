package eu.hxreborn.phdp.ui.screen

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.ui.SettingsUiState
import eu.hxreborn.phdp.ui.SettingsViewModel
import eu.hxreborn.phdp.ui.component.SectionCard
import eu.hxreborn.phdp.ui.component.SettingsScaffold
import eu.hxreborn.phdp.ui.component.TestButtonsRow
import eu.hxreborn.phdp.ui.component.VerticalScrollbar
import eu.hxreborn.phdp.ui.component.preference.ColorPreference
import eu.hxreborn.phdp.ui.component.preference.TogglePreferenceWithIcon
import eu.hxreborn.phdp.ui.theme.Tokens
import me.zhanghai.compose.preference.ProvidePreferenceLocals

@Composable
fun DuoScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomNavPadding: Dp = 0.dp,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefsState = (uiState as? SettingsUiState.Success)?.prefs ?: return
    val context = LocalContext.current
    val isInstalled = remember(context) { context.isDuoStatusBarInstalled() }

    SettingsScaffold(
        title = stringResource(R.string.group_duo_status_bar),
        onNavigateBack = onNavigateBack,
        bottomPadding = bottomNavPadding,
        modifier = modifier,
    ) { innerPadding ->
        ProvidePreferenceLocals {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        top = innerPadding.calculateTopPadding() + Tokens.SpacingLg,
                        bottom = innerPadding.calculateBottomPadding() + Tokens.SpacingLg,
                    ),
            ) {
                item(key = "duo_section") {
                    SectionCard(
                        items =
                            buildList {
                                add {
                                    TogglePreferenceWithIcon(
                                        value = prefsState.duoStatusBarMode,
                                        onValueChange = { viewModel.savePref(Prefs.duoStatusBarMode, it) },
                                        icon = { Icon(Icons.Outlined.DonutLarge, contentDescription = null) },
                                        enabled = isInstalled || prefsState.duoStatusBarMode,
                                        title = { Text(stringResource(R.string.pref_duo_status_bar_title)) },
                                        summary = { Text(stringResource(R.string.pref_duo_status_bar_summary)) },
                                    )
                                }
                                if (prefsState.duoStatusBarMode) {
                                    add {
                                        ColorPreference(
                                            value = prefsState.duoProgressColor,
                                            onValueChange = { viewModel.savePref(Prefs.duoProgressColor, it) },
                                            icon = { Icon(Icons.Outlined.Downloading, contentDescription = null) },
                                            enabled =
                                                !prefsState.gradientEnabled &&
                                                    !prefsState.materialYouEnabled,
                                            title = { Text(stringResource(R.string.pref_progress_color_title)) },
                                            summary = { Text(stringResource(R.string.pref_duo_progress_color_summary)) },
                                        )
                                    }
                                    add {
                                        TogglePreferenceWithIcon(
                                            value = prefsState.percentTextEnabled,
                                            onValueChange = { viewModel.savePref(Prefs.percentTextEnabled, it) },
                                            icon = { Icon(Icons.Outlined.Percent, contentDescription = null) },
                                            title = { Text(stringResource(R.string.pref_show_percentage_title)) },
                                            summary = { Text(stringResource(R.string.pref_duo_percent_summary)) },
                                        )
                                    }
                                }
                            },
                    )
                }

                if (prefsState.duoStatusBarMode) {
                    item(key = "duo_note") {
                        Text(
                            text = stringResource(R.string.duo_shared_settings_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                                Modifier.padding(
                                    horizontal = Tokens.SectionHorizontalMargin,
                                    vertical = Tokens.SpacingSm,
                                ),
                        )
                    }

                    item(key = "duo_test_buttons") {
                        TestButtonsRow(
                            onSimulateSuccess = { viewModel.simulateSuccess() },
                            onSimulateFailure = { viewModel.simulateFailure() },
                        )
                    }
                }
            }
            VerticalScrollbar(listState, innerPadding)
        }
    }
}

private const val DUO_STATUS_BAR_PACKAGE = "io.github.kvmy666.duostatusbar"

internal fun Context.isDuoStatusBarInstalled(): Boolean =
    try {
        if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getApplicationInfo(
                DUO_STATUS_BAR_PACKAGE,
                PackageManager.ApplicationInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(DUO_STATUS_BAR_PACKAGE, 0)
        }
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
