package eu.hxreborn.phdp.ui.screen

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.hxreborn.phdp.BuildConfig
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.backup.FileProblem
import eu.hxreborn.phdp.prefs.ImportResult
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.ui.BackupEvent
import eu.hxreborn.phdp.ui.SettingsUiState
import eu.hxreborn.phdp.ui.SettingsViewModel
import eu.hxreborn.phdp.ui.component.AppSnackbarHost
import eu.hxreborn.phdp.ui.component.ImportResultDialog
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
import java.time.LocalDate

private fun BackupEvent.snackbarMessage(context: Context): String? =
    when (this) {
        is BackupEvent.Exported -> {
            result.entries.size.let {
                context.resources.getQuantityString(R.plurals.backup_exported, it, it)
            }
        }

        BackupEvent.ExportFailed -> {
            context.getString(R.string.backup_export_failed)
        }

        BackupEvent.FileUnreadable -> {
            context.getString(R.string.backup_file_unreadable)
        }

        BackupEvent.FileTooLarge -> {
            context.getString(R.string.backup_file_too_large)
        }

        is BackupEvent.Imported -> {
            when (val outcome = result) {
                is ImportResult.Restored -> {
                    null
                }

                is ImportResult.Unreadable -> {
                    context.getString(
                        when (outcome.problem) {
                            FileProblem.NOT_JSON -> R.string.backup_not_json
                            FileProblem.NOT_A_BACKUP -> R.string.backup_not_a_backup
                            FileProblem.FROM_NEWER_APP -> R.string.backup_from_newer_app
                        },
                    )
                }
            }
        }
    }

@Composable
fun SystemScreen(
    viewModel: SettingsViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onNavigateToLicenses: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val success = uiState as? SettingsUiState.Success ?: return
    val prefsState = success.prefs
    val context = LocalContext.current
    val backupEvent by viewModel.backupEvent.collectAsStateWithLifecycle()

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { viewModel.exportSettings(it) }
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.importSettings(it) }
        }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.backupEvent.collect { event ->
            val message = event?.snackbarMessage(context) ?: return@collect
            viewModel.clearBackupEvent()
            snackbarHostState.showSnackbar(message)
        }
    }

    ((backupEvent as? BackupEvent.Imported)?.result as? ImportResult.Restored)
        ?.let { ImportResultDialog(report = it.report, onDismiss = { viewModel.clearBackupEvent() }) }

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

    Box(modifier = modifier.fillMaxSize()) {
        ProvidePreferenceLocals {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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
                            listOfNotNull(
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
                                {
                                    TogglePreferenceWithIcon(
                                        value = prefsState.floatingNavBar,
                                        onValueChange = { viewModel.savePref(Prefs.floatingNavBar, it) },
                                        title = {
                                            Text(
                                                stringResource(R.string.pref_floating_nav_bar_title),
                                            )
                                        },
                                        summary = {
                                            Text(
                                                stringResource(R.string.pref_floating_nav_bar_summary),
                                            )
                                        },
                                    )
                                },
                                if (prefsState.floatingNavBar) {
                                    {
                                        TogglePreferenceWithIcon(
                                            value = prefsState.hideNavBarOnScroll,
                                            onValueChange = { viewModel.savePref(Prefs.hideNavBarOnScroll, it) },
                                            title = {
                                                Text(
                                                    stringResource(R.string.pref_hide_nav_bar_on_scroll_title),
                                                )
                                            },
                                            summary = {
                                                Text(
                                                    stringResource(R.string.pref_hide_nav_bar_on_scroll_summary),
                                                )
                                            },
                                        )
                                    }
                                } else {
                                    null
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
                                            labelFromValues(it, powerSaverEntries, powerSaverValues) ?: it
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
                                {
                                    TogglePreferenceWithIcon(
                                        value = success.isLauncherIconHidden,
                                        onValueChange = { viewModel.setLauncherIconHidden(it) },
                                        title = {
                                            Text(
                                                stringResource(R.string.pref_hide_launcher_icon_title),
                                            )
                                        },
                                        summary = {
                                            Text(
                                                stringResource(R.string.pref_hide_launcher_icon_summary),
                                            )
                                        },
                                    )
                                },
                                {
                                    TogglePreferenceWithIcon(
                                        value = prefsState.verboseLogging,
                                        onValueChange = { viewModel.savePref(Prefs.verboseLogging, it) },
                                        title = {
                                            Text(stringResource(R.string.pref_verbose_logging_title))
                                        },
                                        summary = {
                                            Text(stringResource(R.string.pref_verbose_logging_summary))
                                        },
                                    )
                                },
                            ),
                    )
                }

                preferenceCategory(
                    key = "system_backup_header",
                    title = { Text(stringResource(R.string.group_backup)) },
                )

                item(key = "system_backup_section") {
                    SectionCard(
                        items =
                            listOf(
                                {
                                    Preference(
                                        onClick = {
                                            importLauncher.launch(arrayOf("application/json", "*/*"))
                                        },
                                        icon = {
                                            Icon(
                                                Icons.Outlined.Download,
                                                contentDescription = null,
                                            )
                                        },
                                        title = { Text(stringResource(R.string.pref_import_settings_title)) },
                                        summary = {
                                            Text(stringResource(R.string.pref_import_settings_summary))
                                        },
                                    )
                                },
                                {
                                    Preference(
                                        onClick = {
                                            exportLauncher.launch(
                                                "phdp-settings-${BuildConfig.VERSION_NAME}-${LocalDate.now()}.json",
                                            )
                                        },
                                        icon = {
                                            Icon(
                                                Icons.Outlined.Upload,
                                                contentDescription = null,
                                            )
                                        },
                                        title = { Text(stringResource(R.string.pref_export_settings_title)) },
                                        summary = {
                                            Text(stringResource(R.string.pref_export_settings_summary))
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
                                                    "https://github.com/hxreborn/punch-hole-download-progress".toUri(),
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
                                                    "https://github.com/hxreborn/punch-hole-download-progress/issues/new/choose".toUri(),
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
        AppSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
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
