package eu.hxreborn.phdp.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.ui.component.SectionCard
import eu.hxreborn.phdp.ui.component.preference.SelectPreference
import eu.hxreborn.phdp.ui.component.preference.TogglePreferenceWithIcon
import eu.hxreborn.phdp.ui.state.PrefsState
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig
import eu.hxreborn.phdp.ui.theme.Tokens
import eu.hxreborn.phdp.util.labelFromValues
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceCategory

@Composable
fun BehaviorScreen(
    prefsState: PrefsState,
    onSavePrefs: (key: String, value: Any) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val finishStyleEntries = stringArrayResource(R.array.finish_style_entries).toList()
    val finishStyleValues = stringArrayResource(R.array.finish_style_values).toList()

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
                                    onValueChange = { onSavePrefs(Prefs.finishStyle.key, it) },
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
                                        labelFromValues(it, finishStyleEntries, finishStyleValues)
                                            ?: it
                                    },
                                )
                            },
                            {
                                TogglePreferenceWithIcon(
                                    value = prefsState.clockwise,
                                    onValueChange = { onSavePrefs(Prefs.clockwise.key, it) },
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
                                    onValueChange = { onSavePrefs(Prefs.showDownloadCount.key, it) },
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
                                    onValueChange = { onSavePrefs(Prefs.hooksFeedback.key, it) },
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
                                        onSavePrefs(Prefs.completionPulseEnabled.key, it)
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BehaviorScreenPreview() {
    AppTheme(darkThemeConfig = DarkThemeConfig.DARK) {
        BehaviorScreen(
            prefsState = PrefsState(),
            onSavePrefs = { _, _ -> },
            contentPadding = PaddingValues(),
        )
    }
}
