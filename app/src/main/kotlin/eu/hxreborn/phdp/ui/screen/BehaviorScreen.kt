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
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.PrefsManager
import eu.hxreborn.phdp.ui.component.SectionCard
import eu.hxreborn.phdp.ui.component.preference.SelectPreference
import eu.hxreborn.phdp.ui.component.preference.TogglePreferenceWithIcon
import eu.hxreborn.phdp.ui.state.PrefsState
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.Tokens
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceCategory

@Composable
fun BehaviorScreen(
    prefsState: PrefsState,
    onSavePrefs: (key: String, value: Any) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val finishStyleEntries = context.resources.getStringArray(R.array.finish_style_entries).toList()
    val finishStyleValues = context.resources.getStringArray(R.array.finish_style_values).toList()

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
                key = "motion_animation_header",
                title = { Text(stringResource(R.string.group_animation)) },
            )

            item(key = "motion_animation_section") {
                SectionCard(
                    items =
                        listOf(
                            {
                                SelectPreference(
                                    value = prefsState.finishStyle,
                                    onValueChange = {
                                        onSavePrefs(
                                            PrefsManager.KEY_FINISH_STYLE,
                                            it,
                                        )
                                    },
                                    values = finishStyleValues,
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_completion_style_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_completion_style_summary),
                                        )
                                    },
                                    valueToText = {
                                        finishStyleLabel(it, finishStyleEntries, finishStyleValues)
                                            ?: it
                                    },
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
                                )
                            },
                        ),
                )
            }

            preferenceCategory(
                key = "behavior_indicators_header",
                title = { Text(stringResource(R.string.group_indicators)) },
            )

            item(key = "behavior_indicators_section") {
                SectionCard(
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
                                )
                            },
                        ),
                )
            }

            preferenceCategory(
                key = "motion_feedback_header",
                title = { Text(stringResource(R.string.group_feedback)) },
            )

            item(key = "motion_feedback_section") {
                SectionCard(
                    items =
                        listOf(
                            {
                                TogglePreferenceWithIcon(
                                    value = prefsState.hooksFeedback,
                                    onValueChange = {
                                        onSavePrefs(
                                            PrefsManager.KEY_HOOKS_FEEDBACK,
                                            it,
                                        )
                                    },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_haptic_feedback_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_haptic_feedback_summary),
                                        )
                                    },
                                )
                            },
                            {
                                TogglePreferenceWithIcon(
                                    value = prefsState.completionPulseEnabled,
                                    onValueChange = {
                                        onSavePrefs(
                                            PrefsManager.KEY_COMPLETION_PULSE_ENABLED,
                                            it,
                                        )
                                    },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_pulse_flash_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_pulse_flash_summary),
                                        )
                                    },
                                )
                            },
                        ),
                )
            }
        }
    }
}

private fun finishStyleLabel(
    value: String,
    entries: List<String>,
    values: List<String>,
): String? = entries.getOrNull(values.indexOf(value))

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BehaviorScreenPreview() {
    AppTheme(darkTheme = true) {
        BehaviorScreen(
            prefsState = PrefsState(),
            onSavePrefs = { _, _ -> },
            contentPadding = PaddingValues(),
        )
    }
}
