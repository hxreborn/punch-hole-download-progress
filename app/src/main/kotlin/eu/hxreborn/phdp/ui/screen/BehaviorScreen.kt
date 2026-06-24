package eu.hxreborn.phdp.ui.screen

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.effects.EffectCatalog
import eu.hxreborn.phdp.effects.EffectOption
import eu.hxreborn.phdp.effects.EffectTiming
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.ui.SettingsUiState
import eu.hxreborn.phdp.ui.SettingsViewModel
import eu.hxreborn.phdp.ui.component.SectionCard
import eu.hxreborn.phdp.ui.component.preference.NavigationPreference
import eu.hxreborn.phdp.ui.component.preference.SelectPreference
import eu.hxreborn.phdp.ui.component.preference.SliderPreferenceWithReset
import eu.hxreborn.phdp.ui.component.preference.SliderPreferenceWithStepper
import eu.hxreborn.phdp.ui.component.preference.TogglePreferenceWithIcon
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig
import eu.hxreborn.phdp.ui.theme.Tokens
import eu.hxreborn.phdp.util.labelFromValues
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceCategory

@Composable
fun BehaviorScreen(
    viewModel: SettingsViewModel,
    onNavigateToBadgeCalibration: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefsState = (uiState as? SettingsUiState.Success)?.prefs ?: return

    val finishStyleEntries = stringArrayResource(R.array.finish_style_entries).toList()
    val finishStyleValues = stringArrayResource(R.array.finish_style_values).toList()
    val effectSpeedEntries = stringArrayResource(R.array.effect_speed_entries).toList()
    val effectSpeedValues = stringArrayResource(R.array.effect_speed_values).toList()
    val effectIntensityEntries = stringArrayResource(R.array.effect_intensity_entries).toList()
    val effectIntensityValues = stringArrayResource(R.array.effect_intensity_values).toList()
    val burnInHideEntries = stringArrayResource(R.array.burn_in_hide_entries).toList()
    val burnInHideValues = stringArrayResource(R.array.burn_in_hide_values).toList()
    val context = LocalContext.current
    val effectDescriptions = remember(context) { EffectCatalog.all.associate { it.id to context.getString(it.descRes) } }
    val effectOptions = EffectCatalog.byId(prefsState.finishStyle).options

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
                key = "progress_ring_header",
                title = { Text(stringResource(R.string.group_progress_ring)) },
            )

            item(key = "progress_ring_section") {
                SectionCard(
                    items =
                        listOf(
                            {
                                TogglePreferenceWithIcon(
                                    value = prefsState.clockwise,
                                    onValueChange = { viewModel.savePref(Prefs.clockwise, it) },
                                    title = {
                                        Text(stringResource(R.string.pref_invert_rotation_title))
                                    },
                                    summary = {
                                        val text = if (prefsState.clockwise) R.string.clockwise else R.string.counter_clockwise
                                        Text(stringResource(text))
                                    },
                                )
                            },
                        ),
                )
            }

            preferenceCategory(
                key = "completion_effect_header",
                title = { Text(stringResource(R.string.group_completion_effect)) },
            )

            item(key = "completion_effect_section") {
                SectionCard(
                    modifier = Modifier.animateContentSize(),
                    items =
                        buildList {
                            add {
                                SelectPreference(
                                    value = prefsState.finishStyle,
                                    onValueChange = { viewModel.savePref(Prefs.finishStyle, it) },
                                    values = finishStyleValues,
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_completion_style_title),
                                        )
                                    },
                                    summary = {
                                        Text(effectDescriptions[prefsState.finishStyle].orEmpty())
                                    },
                                    valueToText = {
                                        labelFromValues(it, finishStyleEntries, finishStyleValues) ?: it
                                    },
                                    valueToDescription = { effectDescriptions[it] },
                                )
                            }
                            if (EffectOption.SPEED in effectOptions) {
                                add {
                                    SelectPreference(
                                        value = prefsState.effectSpeed,
                                        onValueChange = {
                                            viewModel.savePref(Prefs.effectSpeed, it)
                                        },
                                        values = effectSpeedValues,
                                        title = {
                                            Text(stringResource(R.string.pref_effect_speed_title))
                                        },
                                        summary = {
                                            Text(stringResource(R.string.pref_effect_speed_summary))
                                        },
                                        valueToText = {
                                            labelFromValues(it, effectSpeedEntries, effectSpeedValues) ?: it
                                        },
                                    )
                                }
                            }
                            if (EffectOption.INTENSITY in effectOptions) {
                                add {
                                    SelectPreference(
                                        value = prefsState.effectIntensity,
                                        onValueChange = {
                                            viewModel.savePref(Prefs.effectIntensity, it)
                                        },
                                        values = effectIntensityValues,
                                        title = {
                                            Text(stringResource(R.string.pref_effect_intensity_title))
                                        },
                                        summary = {
                                            Text(stringResource(R.string.pref_effect_intensity_summary))
                                        },
                                        valueToText = {
                                            labelFromValues(it, effectIntensityEntries, effectIntensityValues) ?: it
                                        },
                                    )
                                }
                            }
                            if (EffectOption.DIRECTION in effectOptions) {
                                add {
                                    TogglePreferenceWithIcon(
                                        value = prefsState.effectReverse,
                                        onValueChange = {
                                            viewModel.savePref(Prefs.effectReverse, it)
                                        },
                                        title = {
                                            Text(stringResource(R.string.pref_effect_direction_title))
                                        },
                                        summary = {
                                            Text(stringResource(R.string.pref_effect_direction_summary))
                                        },
                                    )
                                }
                            }
                            if (EffectOption.REPEAT in effectOptions) {
                                add {
                                    val repeatRange = Prefs.effectRepeat.range!!
                                    SliderPreferenceWithReset(
                                        value = prefsState.effectRepeat.toFloat(),
                                        onValueChange = {
                                            viewModel.savePref(Prefs.effectRepeat, it.toInt())
                                        },
                                        title = {
                                            Text(stringResource(R.string.pref_effect_repeat_title))
                                        },
                                        summary = {
                                            val perCycle =
                                                EffectTiming.perCycleMs(
                                                    prefsState.finishStyle,
                                                    prefsState.finishHoldMs,
                                                    prefsState.finishExitMs,
                                                    prefsState.effectSpeed,
                                                    prefsState.effectRepeat,
                                                )
                                            val totalMs =
                                                EffectTiming.totalMs(
                                                    prefsState.finishStyle,
                                                    prefsState.finishHoldMs,
                                                    prefsState.finishExitMs,
                                                    prefsState.effectSpeed,
                                                )
                                            Column {
                                                Text(
                                                    stringResource(
                                                        R.string.pref_effect_repeat_timing,
                                                        perCycle,
                                                        totalMs,
                                                    ),
                                                )
                                                if (EffectTiming.isFlickerRisk(
                                                        prefsState.finishStyle,
                                                        prefsState.finishHoldMs,
                                                        prefsState.finishExitMs,
                                                        prefsState.effectSpeed,
                                                        prefsState.effectRepeat,
                                                    )
                                                ) {
                                                    Text(
                                                        stringResource(R.string.pref_effect_flicker_warning),
                                                        color = MaterialTheme.colorScheme.error,
                                                    )
                                                }
                                            }
                                        },
                                        valueRange = repeatRange.first.toFloat()..repeatRange.last.toFloat(),
                                        defaultValue = Prefs.effectRepeat.default.toFloat(),
                                        onReset = {
                                            viewModel.savePref(
                                                Prefs.effectRepeat,
                                                Prefs.effectRepeat.default,
                                            )
                                        },
                                        valueText = { Text("${it.toInt()}") },
                                        stepSize = 1f,
                                    )
                                }
                            }
                            if (prefsState.finishStyle == "segmented") {
                                add {
                                    val segCountRange = Prefs.segmentCount.range!!
                                    SliderPreferenceWithReset(
                                        value = prefsState.segmentCount.toFloat(),
                                        onValueChange = {
                                            viewModel.savePref(Prefs.segmentCount, it.toInt())
                                        },
                                        title = {
                                            Text(
                                                stringResource(R.string.pref_segment_count_title),
                                            )
                                        },
                                        summary = {
                                            Text(
                                                stringResource(R.string.pref_segment_count_summary),
                                            )
                                        },
                                        valueRange = segCountRange.first.toFloat()..segCountRange.last.toFloat(),
                                        defaultValue = Prefs.segmentCount.default.toFloat(),
                                        onReset = {
                                            viewModel.savePref(
                                                Prefs.segmentCount,
                                                Prefs.segmentCount.default,
                                            )
                                        },
                                        valueText = { Text("${it.toInt()}") },
                                        stepSize = 1f,
                                    )
                                }
                                add {
                                    SliderPreferenceWithReset(
                                        value = prefsState.segmentGapDegrees,
                                        onValueChange = {
                                            viewModel.savePref(Prefs.segmentGapDegrees, it)
                                        },
                                        title = {
                                            Text(
                                                stringResource(R.string.pref_segment_gap_title),
                                            )
                                        },
                                        summary = {
                                            Text(
                                                stringResource(R.string.pref_segment_gap_summary),
                                            )
                                        },
                                        valueRange = Prefs.segmentGapDegrees.range!!,
                                        defaultValue = Prefs.segmentGapDegrees.default,
                                        onReset = {
                                            viewModel.savePref(
                                                Prefs.segmentGapDegrees,
                                                Prefs.segmentGapDegrees.default,
                                            )
                                        },
                                        valueText = { Text("%.1f\u00B0".format(it)) },
                                        stepSize = 1f,
                                    )
                                }
                            }
                        },
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
                                    onValueChange = { viewModel.savePref(Prefs.showDownloadCount, it) },
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
                            {
                                NavigationPreference(
                                    onClick = onNavigateToBadgeCalibration,
                                    enabled = prefsState.showDownloadCount,
                                    title = {
                                        Text(
                                            stringResource(
                                                R.string.pref_calibrate_badge_title,
                                            ),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(
                                                R.string.pref_calibrate_badge_summary,
                                            ),
                                        )
                                    },
                                )
                            },
                            {
                                SelectPreference(
                                    value = prefsState.burnInHideMs.toString(),
                                    onValueChange = {
                                        viewModel.savePref(Prefs.burnInHideMs, it.toInt())
                                    },
                                    values = burnInHideValues,
                                    title = {
                                        Text(stringResource(R.string.pref_burn_in_hide_title))
                                    },
                                    summary = {
                                        Text(stringResource(R.string.pref_burn_in_hide_summary))
                                    },
                                    valueToText = { raw ->
                                        labelFromValues(raw, burnInHideEntries, burnInHideValues)
                                            ?: "${(raw.toIntOrNull() ?: 0) / 1000}s"
                                    },
                                )
                            },
                            {
                                SliderPreferenceWithStepper(
                                    value = prefsState.progressAnimMs.toFloat(),
                                    onValueChange = {
                                        viewModel.savePref(Prefs.progressAnimMs, it.toInt())
                                    },
                                    title = {
                                        Text(stringResource(R.string.pref_progress_anim_title))
                                    },
                                    valueRange = 0f..750f,
                                    defaultValue = Prefs.progressAnimMs.default.toFloat(),
                                    onReset = {
                                        viewModel.savePref(
                                            Prefs.progressAnimMs,
                                            Prefs.progressAnimMs.default,
                                        )
                                    },
                                    stepSize = 50f,
                                    suffix = "ms",
                                    summary = {
                                        Text(stringResource(R.string.pref_progress_anim_summary))
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
                                    onValueChange = { viewModel.savePref(Prefs.hooksFeedback, it) },
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
                        ),
                )
            }
        }
    }
}

@Suppress("ViewModelConstructorInComposable")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BehaviorScreenPreview() {
    AppTheme(darkThemeConfig = DarkThemeConfig.DARK) {
        BehaviorScreen(
            viewModel = PreviewViewModel(),
            onNavigateToBadgeCalibration = {},
            contentPadding = PaddingValues(),
        )
    }
}
