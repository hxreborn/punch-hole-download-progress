package eu.hxreborn.phdp.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
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
                key = "system_service_header",
                title = { Text(stringResource(R.string.group_service)) },
            )

            item(key = "system_service_section") {
                SectionCard(
                    items =
                        listOf(
                            {
                                TogglePreferenceWithIcon(
                                    value = prefsState.enabled,
                                    onValueChange = { onSavePrefs(PrefsManager.KEY_ENABLED, it) },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_enable_service_title),
                                        )
                                    },
                                    summary = {
                                        val text =
                                            if (prefsState.enabled) {
                                                R.string.pref_enable_service_on
                                            } else {
                                                R.string.pref_enable_service_off
                                            }
                                        Text(stringResource(text))
                                    },
                                )
                            },
                        ),
                )
            }

            preferenceCategory(
                key = "system_visibility_header",
                title = { Text(stringResource(R.string.group_visibility)) },
            )

            item(key = "system_visibility_section") {
                SectionCard(
                    enabled = prefsState.enabled,
                    items =
                        listOf(
                            {
                                TogglePreferenceWithIcon(
                                    value = prefsState.showDownloadCount,
                                    onValueChange = {
                                        onSavePrefs(
                                            PrefsManager.KEY_SHOW_DOWNLOAD_COUNT,
                                            it,
                                        )
                                    },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_show_queue_count_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_show_queue_count_summary),
                                        )
                                    },
                                    enabled = prefsState.enabled,
                                )
                            },
                            {
                                TogglePreferenceWithIcon(
                                    value = prefsState.clockwise,
                                    onValueChange = { onSavePrefs(PrefsManager.KEY_CLOCKWISE, it) },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_invert_rotation_title),
                                        )
                                    },
                                    summary = {
                                        val text =
                                            if (prefsState.clockwise) {
                                                R.string.clockwise
                                            } else {
                                                R.string.counter_clockwise
                                            }
                                        Text(stringResource(text))
                                    },
                                    enabled = prefsState.enabled,
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
                    enabled = prefsState.enabled,
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
                                    enabled = prefsState.enabled,
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
                    enabled = prefsState.enabled,
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
                                    enabled = prefsState.enabled,
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
                                    enabled = prefsState.enabled,
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
            contentPadding = PaddingValues(),
        )
    }
}
