package eu.hxreborn.phdp.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import eu.hxreborn.phdp.BuildConfig
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.ui.component.SectionCard
import eu.hxreborn.phdp.ui.component.preference.SelectPreference
import eu.hxreborn.phdp.ui.state.PrefsState
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig
import eu.hxreborn.phdp.ui.theme.Tokens
import eu.hxreborn.phdp.util.labelFromValues
import me.zhanghai.compose.preference.Preference
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
    val powerSaverEntries = stringArrayResource(R.array.power_saver_entries).toList()
    val powerSaverValues = stringArrayResource(R.array.power_saver_values).toList()

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
                                    onValueChange = { onSavePrefs(Prefs.powerSaverMode.key, it) },
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
                                Preference(
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
                                Preference(
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
                                    Preference(
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SystemScreenPreview() {
    AppTheme(darkThemeConfig = DarkThemeConfig.DARK) {
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
