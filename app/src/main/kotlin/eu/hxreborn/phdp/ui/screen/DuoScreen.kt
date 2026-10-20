package eu.hxreborn.phdp.ui.screen

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
import eu.hxreborn.phdp.ui.component.sectionPreferenceTheme
import eu.hxreborn.phdp.ui.theme.Tokens
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceCategory

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
    val isSupported = remember(context) { context.isDuoStatusBarSupported() }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    if (showAbout) {
        DuoAboutDialog(
            onOpenGitHub = {
                showAbout = false
                context.startActivity(Intent(Intent.ACTION_VIEW, DUO_STATUS_BAR_URL.toUri()))
            },
            onDismiss = { showAbout = false },
        )
    }

    SettingsScaffold(
        title = stringResource(R.string.group_duo_status_bar),
        onNavigateBack = onNavigateBack,
        actions = {
            IconButton(onClick = { showAbout = true }) {
                Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.duo_about_title))
            }
        },
        bottomPadding = bottomNavPadding,
        modifier = modifier,
    ) { innerPadding ->
        ProvidePreferenceLocals(theme = sectionPreferenceTheme()) {
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
                item(key = "duo_mode_section") {
                    SectionCard(
                        items =
                            listOf(
                                {
                                    TogglePreferenceWithIcon(
                                        value = prefsState.duoStatusBarMode,
                                        onValueChange = { viewModel.savePref(Prefs.duoStatusBarMode, it) },
                                        icon = { Icon(Icons.Outlined.DonutLarge, contentDescription = null) },
                                        enabled = isSupported || prefsState.duoStatusBarMode,
                                        title = { Text(stringResource(R.string.pref_duo_status_bar_title)) },
                                        summary = {
                                            Text(
                                                stringResource(
                                                    if (isSupported) {
                                                        R.string.pref_duo_status_bar_summary
                                                    } else {
                                                        R.string.pref_duo_status_bar_unavailable
                                                    },
                                                ),
                                            )
                                        },
                                    )
                                },
                            ),
                    )
                }

                if (prefsState.duoStatusBarMode) {
                    preferenceCategory(
                        key = "duo_ring_header",
                        title = { Text(stringResource(R.string.group_duo_ring)) },
                    )

                    item(key = "duo_ring_section") {
                        SectionCard(
                            items =
                                listOf(
                                    {
                                        ColorPreference(
                                            value = prefsState.duoProgressColor,
                                            onValueChange = { viewModel.savePref(Prefs.duoProgressColor, it) },
                                            icon = { Icon(Icons.Outlined.Downloading, contentDescription = null) },
                                            enabled =
                                                !prefsState.gradientEnabled &&
                                                    !prefsState.materialYouEnabled,
                                            title = { Text(stringResource(R.string.pref_progress_color_title)) },
                                            summary = {
                                                Text(
                                                    stringResource(
                                                        when {
                                                            prefsState.materialYouEnabled -> R.string.pref_duo_progress_color_material_you
                                                            prefsState.gradientEnabled -> R.string.pref_duo_progress_color_gradient
                                                            else -> R.string.pref_duo_progress_color_summary
                                                        },
                                                    ),
                                                )
                                            },
                                        )
                                    },
                                    {
                                        TogglePreferenceWithIcon(
                                            value = prefsState.percentTextEnabled,
                                            onValueChange = { viewModel.savePref(Prefs.percentTextEnabled, it) },
                                            icon = { Icon(Icons.Outlined.Percent, contentDescription = null) },
                                            title = { Text(stringResource(R.string.pref_show_percentage_title)) },
                                            summary = { Text(stringResource(R.string.pref_duo_percent_summary)) },
                                        )
                                    },
                                ),
                        )
                    }

                    preferenceCategory(
                        key = "duo_preview_header",
                        title = { Text(stringResource(R.string.group_duo_preview)) },
                    )

                    item(key = "duo_test_buttons") {
                        TestButtonsRow(
                            onSimulateSuccess = viewModel::simulateSuccess,
                            onSimulateFailure = viewModel::simulateFailure,
                        )
                    }
                }

                item(key = "duo_footer") {
                    DuoFooter()
                }
            }
            VerticalScrollbar(listState, innerPadding)
        }
    }
}

@Composable
private fun DuoAboutDialog(
    onOpenGitHub: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
        title = { Text(stringResource(R.string.duo_about_title)) },
        text = {
            Text(
                text = stringResource(R.string.duo_about_body),
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onOpenGitHub,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    painterResource(R.drawable.ic_github_24),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.duo_about_open_github))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.duo_about_close))
            }
        },
    )
}

@Composable
private fun DuoFooter() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Tokens.SpacingLg),
        modifier =
            Modifier.padding(
                horizontal = Tokens.SectionHorizontalMargin + Tokens.SpacingLg,
                vertical = Tokens.SpacingLg,
            ),
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Tokens.SpacingLg)) {
            DuoFooterGroup(R.string.duo_footer_hidden_title, R.array.duo_footer_hidden_items)
            DuoFooterGroup(R.string.duo_footer_used_title, R.array.duo_footer_used_items)
        }
    }
}

@Composable
private fun DuoFooterGroup(
    @StringRes title: Int,
    @ArrayRes items: Int,
) {
    Column {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        stringArrayResource(items).forEach { item ->
            Text(
                text = "• $item",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val DUO_STATUS_BAR_PACKAGE = "io.github.kvmy666.duostatusbar"
private const val DUO_STATUS_BAR_URL = "https://github.com/kvmy666/duoStatusBar"
private const val DUO_DOWNLOAD_PROGRESS_META = "io.github.kvmy666.duostatusbar.DOWNLOAD_PROGRESS"

internal fun Context.isDuoStatusBarSupported(): Boolean =
    try {
        val info =
            if (Build.VERSION.SDK_INT >= 33) {
                packageManager.getApplicationInfo(
                    DUO_STATUS_BAR_PACKAGE,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(DUO_STATUS_BAR_PACKAGE, PackageManager.GET_META_DATA)
            }
        info.metaData?.getBoolean(DUO_DOWNLOAD_PROGRESS_META, false) == true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
