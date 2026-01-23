package eu.hxreborn.phdp.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import eu.hxreborn.phdp.BuildConfig
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.PrefsManager
import eu.hxreborn.phdp.ui.component.SectionCard
import eu.hxreborn.phdp.ui.component.preference.ActionPreference
import eu.hxreborn.phdp.ui.component.preference.SelectPreference
import eu.hxreborn.phdp.ui.component.preference.TogglePreferenceWithIcon
import eu.hxreborn.phdp.ui.state.PrefsState
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.Tokens
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceCategory

@Composable
fun SystemScreen(
    prefsState: PrefsState,
    onSavePrefs: (key: String, value: Any) -> Unit,
    onTestSuccess: () -> Unit,
    onTestFailure: () -> Unit,
    onClearDownloads: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val powerSaverEntries = context.resources.getStringArray(R.array.power_saver_entries).toList()
    val powerSaverValues = context.resources.getStringArray(R.array.power_saver_values).toList()

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
                                    onValueChange = {
                                        onSavePrefs(
                                            PrefsManager.KEY_POWER_SAVER_MODE,
                                            it,
                                        )
                                    },
                                    values = powerSaverValues,
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_battery_saver_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            powerSaverLabel(
                                                prefsState.powerSaverMode,
                                                powerSaverEntries,
                                                powerSaverValues,
                                            ) ?: prefsState.powerSaverMode,
                                        )
                                    },
                                    valueToText = {
                                        powerSaverLabel(it, powerSaverEntries, powerSaverValues)
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
                                ActionPreference(
                                    onClick = onTestSuccess,
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
                                ActionPreference(
                                    onClick = onTestFailure,
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
                                ActionPreference(
                                    onClick = onClearDownloads,
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
                SectionCard(
                    items =
                        listOf(
                            {
                                SelectionContainer {
                                    ActionPreference(
                                        onClick = {},
                                        title = { Text(stringResource(R.string.pref_version_title)) },
                                        summary = {
                                            Text(
                                                "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                            )
                                        },
                                        enabled = false,
                                    )
                                }
                            },
                        ),
                )
            }
        }
    }
}

private fun powerSaverLabel(
    value: String,
    entries: List<String>,
    values: List<String>,
): String? = entries.getOrNull(values.indexOf(value))

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SystemScreenPreview() {
    AppTheme(darkTheme = true) {
        SystemScreen(
            prefsState = PrefsState(),
            onSavePrefs = { _, _ -> },
            onTestSuccess = {},
            onTestFailure = {},
            onClearDownloads = {},
            contentPadding = PaddingValues(),
        )
    }
}
