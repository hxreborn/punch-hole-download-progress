package eu.hxreborn.phdp.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.hxreborn.phdp.BuildConfig
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.ui.SettingsUiState
import eu.hxreborn.phdp.ui.SettingsViewModel
import eu.hxreborn.phdp.ui.component.SectionCard
import eu.hxreborn.phdp.ui.component.preference.SelectPreference
import eu.hxreborn.phdp.ui.component.preference.TogglePreferenceWithIcon
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig
import eu.hxreborn.phdp.ui.theme.Tokens
import eu.hxreborn.phdp.util.labelFromValues
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceCategory

@Composable
fun SystemScreen(
    viewModel: SettingsViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onNavigateToLicenses: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefsState = (uiState as? SettingsUiState.Success)?.prefs ?: return
    val context = LocalContext.current

    val themeEntries = stringArrayResource(R.array.theme_entries).toList()
    val themeValues = stringArrayResource(R.array.theme_values).toList()
    val powerSaverEntries = stringArrayResource(R.array.power_saver_entries).toList()
    val powerSaverValues = stringArrayResource(R.array.power_saver_values).toList()
    val currentThemeValue =
        when (prefsState.darkThemeConfig) {
            DarkThemeConfig.FOLLOW_SYSTEM -> "follow_system"
            DarkThemeConfig.LIGHT -> "light"
            DarkThemeConfig.DARK -> "dark"
        }

    ProvidePreferenceLocals {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = contentPadding.calculateTopPadding() + Tokens.SpacingLg,
                    bottom = contentPadding.calculateBottomPadding() + Tokens.SpacingLg,
                ),
        ) {
            preferenceCategory(
                key = "system_app_theme_header",
                title = { Text(stringResource(R.string.group_app_theme)) },
            )

            item(key = "system_app_theme_section") {
                SectionCard(
                    items =
                        listOf(
                            {
                                SelectPreference(
                                    value = currentThemeValue,
                                    onValueChange = { viewModel.savePref(Prefs.darkThemeConfig, it) },
                                    values = themeValues,
                                    title = { Text(stringResource(R.string.pref_theme_title)) },
                                    summary = { Text(stringResource(R.string.pref_theme_summary)) },
                                    valueToText = {
                                        labelFromValues(it, themeEntries, themeValues) ?: it
                                    },
                                )
                            },
                            {
                                TogglePreferenceWithIcon(
                                    value = prefsState.useDynamicColor,
                                    onValueChange = { viewModel.savePref(Prefs.useDynamicColor, it) },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_dynamic_color_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_dynamic_color_summary),
                                        )
                                    },
                                )
                            },
                        ),
                )
            }

            preferenceCategory(
                key = "system_power_header",
                title = { Text(stringResource(R.string.group_power)) },
            )

            item(key = "system_power_section") {
                SectionCard(
                    items =
                        listOf(
                            {
                                SelectPreference(
                                    value = prefsState.powerSaverMode,
                                    onValueChange = { viewModel.savePref(Prefs.powerSaverMode, it) },
                                    values = powerSaverValues,
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_battery_saver_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_battery_saver_summary),
                                        )
                                    },
                                    valueToText = {
                                        labelFromValues(it, powerSaverEntries, powerSaverValues)
                                            ?: it
                                    },
                                )
                            },
                        ),
                )
            }

            preferenceCategory(
                key = "system_diagnostics_header",
                title = { Text(stringResource(R.string.group_diagnostics)) },
            )

            item(key = "system_diagnostics_section") {
                SectionCard(
                    items =
                        listOf(
                            {
                                Preference(
                                    onClick = { viewModel.simulateSuccess() },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_debug_completion_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_debug_completion_summary),
                                        )
                                    },
                                )
                            },
                            {
                                Preference(
                                    onClick = { viewModel.simulateFailure() },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_test_failure_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_test_failure_summary),
                                        )
                                    },
                                )
                            },
                            {
                                Preference(
                                    onClick = {
                                        viewModel.clearDownloads()
                                        Toast
                                            .makeText(
                                                context,
                                                R.string.clear_downloads_done,
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_clear_downloads_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_clear_downloads_summary),
                                        )
                                    },
                                )
                            },
                        ),
                )
            }

            preferenceCategory(
                key = "system_about_header",
                title = { Text(stringResource(R.string.group_about)) },
            )

            item(key = "system_about_section") {
                val versionString = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                SectionCard(
                    items =
                        listOf(
                            {
                                Preference(
                                    onClick = {
                                        val clipboard =
                                            context.getSystemService(ClipboardManager::class.java)
                                        clipboard.setPrimaryClip(
                                            ClipData.newPlainText("version", versionString),
                                        )
                                        Toast
                                            .makeText(
                                                context,
                                                R.string.pref_version_copied,
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    },
                                    icon = {
                                        Icon(
                                            Icons.Outlined.Info,
                                            contentDescription = null,
                                        )
                                    },
                                    title = { Text(stringResource(R.string.pref_version_title)) },
                                    summary = { Text(versionString) },
                                )
                            },
                            {
                                Preference(
                                    onClick = {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(
                                                    "https://github.com/hxreborn/punch-hole-download-progress",
                                                ),
                                            ),
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            painterResource(R.drawable.ic_github_24),
                                            contentDescription = null,
                                        )
                                    },
                                    title = { Text(stringResource(R.string.pref_git_repo)) },
                                    summary = {
                                        Text(stringResource(R.string.pref_git_repo_summary))
                                    },
                                )
                            },
                            {
                                Preference(
                                    onClick = onNavigateToLicenses,
                                    icon = {
                                        Icon(
                                            Icons.Outlined.Gavel,
                                            contentDescription = null,
                                        )
                                    },
                                    title = { Text(stringResource(R.string.pref_licenses)) },
                                    summary = {
                                        Text(stringResource(R.string.pref_licenses_summary))
                                    },
                                )
                            },
                            {
                                Preference(
                                    onClick = {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(
                                                    "https://github.com/hxreborn/punch-hole-download-progress/issues/new/choose",
                                                ),
                                            ),
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            Icons.Outlined.BugReport,
                                            contentDescription = null,
                                        )
                                    },
                                    title = {
                                        Text(stringResource(R.string.pref_report_issue))
                                    },
                                    summary = {
                                        Text(stringResource(R.string.pref_report_issue_summary))
                                    },
                                )
                            },
                        ),
                )
            }
        }
    }
}

@Suppress("ViewModelConstructorInComposable")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SystemScreenPreview() {
    AppTheme(darkThemeConfig = DarkThemeConfig.DARK) {
        SystemScreen(
            viewModel = PreviewViewModel(),
            contentPadding = PaddingValues(),
        )
    }
}
