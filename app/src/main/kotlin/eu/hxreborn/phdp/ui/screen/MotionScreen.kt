package eu.hxreborn.phdp.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.PrefsManager
import eu.hxreborn.phdp.ui.component.SectionHeader
import eu.hxreborn.phdp.ui.component.SettingsGroup
import eu.hxreborn.phdp.ui.component.TweakSelection
import eu.hxreborn.phdp.ui.component.TweakSlider
import eu.hxreborn.phdp.ui.component.TweakSwitch
import eu.hxreborn.phdp.ui.state.PrefsState
import eu.hxreborn.phdp.ui.theme.Tokens

@Composable
fun MotionScreen(
    prefsState: PrefsState,
    onSavePrefs: (key: String, value: Any) -> Unit,
    onPreviewAnimation: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val finishStyleEntries = context.resources.getStringArray(R.array.finish_style_entries).toList()
    val finishStyleValues = context.resources.getStringArray(R.array.finish_style_values).toList()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                top = contentPadding.calculateTopPadding() + Tokens.SpacingLg,
                bottom = contentPadding.calculateBottomPadding() + Tokens.SpacingLg,
            ),
    ) {
        item(key = "motion_animation_header") {
            SectionHeader(title = stringResource(R.string.group_animation))
        }

        item(key = "motion_animation_group") {
            SettingsGroup {
                item {
                    TweakSelection(
                        title = stringResource(R.string.completion_style),
                        description = stringResource(R.string.completion_style_desc),
                        currentValue = prefsState.finishStyle,
                        entries = finishStyleEntries,
                        values = finishStyleValues,
                        onValueSelected = { value ->
                            onSavePrefs(PrefsManager.KEY_FINISH_STYLE, value)
                        },
                        longPressHint = stringResource(R.string.long_press_preview_hint),
                        onLongPress = { value ->
                            onSavePrefs(PrefsManager.KEY_FINISH_STYLE, value)
                            onPreviewAnimation()
                        },
                    )
                }
            }
        }

        item(key = "motion_timing_header") {
            SectionHeader(title = stringResource(R.string.group_timing))
        }

        item(key = "motion_timing_group") {
            SettingsGroup {
                item {
                    TweakSlider(
                        title = stringResource(R.string.completion_hold),
                        description = stringResource(R.string.completion_hold_desc),
                        value = prefsState.finishHoldMs.toFloat(),
                        onValueChange = { value ->
                            onSavePrefs(PrefsManager.KEY_FINISH_HOLD_MS, value.toInt())
                        },
                        onReset = {
                            onSavePrefs(PrefsManager.KEY_FINISH_HOLD_MS, PrefsManager.DEFAULT_FINISH_HOLD_MS)
                        },
                        valueRange = PrefsManager.MIN_FINISH_HOLD_MS.toFloat()..PrefsManager.MAX_FINISH_HOLD_MS.toFloat(),
                        valueLabel = { "${it.toInt()}ms" },
                        defaultValue = PrefsManager.DEFAULT_FINISH_HOLD_MS.toFloat(),
                        stepSize = 50f,
                        hapticInterval = 100f,
                    )
                }
                item {
                    TweakSlider(
                        title = stringResource(R.string.exit_duration),
                        description = stringResource(R.string.exit_duration_desc),
                        value = prefsState.finishExitMs.toFloat(),
                        onValueChange = { value ->
                            onSavePrefs(PrefsManager.KEY_FINISH_EXIT_MS, value.toInt())
                        },
                        onReset = {
                            onSavePrefs(PrefsManager.KEY_FINISH_EXIT_MS, PrefsManager.DEFAULT_FINISH_EXIT_MS)
                        },
                        valueRange = PrefsManager.MIN_FINISH_EXIT_MS.toFloat()..PrefsManager.MAX_FINISH_EXIT_MS.toFloat(),
                        valueLabel = { "${it.toInt()}ms" },
                        defaultValue = PrefsManager.DEFAULT_FINISH_EXIT_MS.toFloat(),
                        stepSize = 50f,
                        hapticInterval = 100f,
                    )
                }
            }
        }

        item(key = "motion_feedback_header") {
            SectionHeader(title = stringResource(R.string.group_feedback))
        }

        item(key = "motion_feedback_group") {
            SettingsGroup {
                item {
                    TweakSwitch(
                        title = stringResource(R.string.finish_vibration),
                        description = stringResource(R.string.finish_vibration_desc),
                        checked = prefsState.hooksFeedback,
                        onCheckedChange = { checked ->
                            onSavePrefs(PrefsManager.KEY_HOOKS_FEEDBACK, checked)
                        },
                    )
                }
                item {
                    TweakSwitch(
                        title = stringResource(R.string.completion_pulse),
                        description = stringResource(R.string.completion_pulse_desc),
                        checked = prefsState.completionPulseEnabled,
                        onCheckedChange = { checked ->
                            onSavePrefs(PrefsManager.KEY_COMPLETION_PULSE_ENABLED, checked)
                        },
                    )
                }
            }
        }

        item(key = "motion_fast_header") {
            SectionHeader(title = stringResource(R.string.group_fast_downloads))
        }

        item(key = "motion_fast_group") {
            SettingsGroup {
                item {
                    TweakSwitch(
                        title = stringResource(R.string.min_visibility),
                        description = stringResource(R.string.min_visibility_desc),
                        checked = prefsState.minVisibilityEnabled,
                        onCheckedChange = { checked ->
                            onSavePrefs(PrefsManager.KEY_MIN_VISIBILITY_ENABLED, checked)
                        },
                    )
                }
                item {
                    TweakSlider(
                        title = stringResource(R.string.min_visibility_duration),
                        description = stringResource(R.string.min_visibility_duration_desc),
                        value = prefsState.minVisibilityMs.toFloat(),
                        onValueChange = { value ->
                            onSavePrefs(PrefsManager.KEY_MIN_VISIBILITY_MS, value.toInt())
                        },
                        onReset = {
                            onSavePrefs(PrefsManager.KEY_MIN_VISIBILITY_MS, PrefsManager.DEFAULT_MIN_VISIBILITY_MS)
                        },
                        valueRange = PrefsManager.MIN_MIN_VISIBILITY_MS.toFloat()..PrefsManager.MAX_MIN_VISIBILITY_MS.toFloat(),
                        valueLabel = { "${it.toInt()}ms" },
                        defaultValue = PrefsManager.DEFAULT_MIN_VISIBILITY_MS.toFloat(),
                        stepSize = 50f,
                        hapticInterval = 100f,
                        enabled = prefsState.minVisibilityEnabled,
                    )
                }
            }
        }
    }
}
