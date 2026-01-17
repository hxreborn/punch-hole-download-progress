package eu.hxreborn.phpm.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.hxreborn.phpm.R
import eu.hxreborn.phpm.prefs.PrefsManager
import eu.hxreborn.phpm.ui.component.SectionHeader
import eu.hxreborn.phpm.ui.component.SettingsGroup
import eu.hxreborn.phpm.ui.component.TweakColorPicker
import eu.hxreborn.phpm.ui.component.TweakSelection
import eu.hxreborn.phpm.ui.component.TweakSlider
import eu.hxreborn.phpm.ui.component.TweakSwitch
import eu.hxreborn.phpm.ui.state.PrefsState
import eu.hxreborn.phpm.ui.theme.Tokens

@Composable
fun DesignScreen(
    prefsState: PrefsState,
    onSavePrefs: (key: String, value: Any) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                top = contentPadding.calculateTopPadding() + Tokens.SpacingLg,
                bottom = contentPadding.calculateBottomPadding() + Tokens.SpacingLg,
            ),
    ) {
        item(key = "design_colors_header") {
            SectionHeader(title = stringResource(R.string.group_colors))
        }

        item(key = "design_colors_group") {
            SettingsGroup {
                item {
                    TweakColorPicker(
                        title = stringResource(R.string.active_progress),
                        description = stringResource(R.string.active_progress_desc),
                        currentColor = prefsState.color,
                        onColorSelected = { color ->
                            onSavePrefs(PrefsManager.KEY_COLOR, color)
                        },
                    )
                }
                item {
                    TweakColorPicker(
                        title = stringResource(R.string.on_completion),
                        description = stringResource(R.string.on_completion_desc),
                        currentColor = prefsState.finishFlashColor,
                        onColorSelected = { color ->
                            onSavePrefs(PrefsManager.KEY_FINISH_FLASH_COLOR, color)
                        },
                    )
                }
            }
        }

        item(key = "design_geometry_header") {
            SectionHeader(title = stringResource(R.string.group_geometry))
        }

        item(key = "design_geometry_group") {
            SettingsGroup {
                item {
                    TweakSlider(
                        title = stringResource(R.string.ring_thickness),
                        description = stringResource(R.string.ring_thickness_desc),
                        value = prefsState.strokeWidth,
                        onValueChange = { value ->
                            onSavePrefs(PrefsManager.KEY_STROKE_WIDTH, value)
                        },
                        onReset = {
                            onSavePrefs(PrefsManager.KEY_STROKE_WIDTH, PrefsManager.DEFAULT_STROKE_WIDTH)
                        },
                        valueRange = PrefsManager.MIN_STROKE_WIDTH..PrefsManager.MAX_STROKE_WIDTH,
                        valueLabel = { "%.1fdp".format(it) },
                        defaultValue = PrefsManager.DEFAULT_STROKE_WIDTH,
                        stepSize = 0.1f,
                        hapticInterval = 0.5f,
                    )
                }
                item {
                    TweakSlider(
                        title = stringResource(R.string.camera_gap),
                        description = stringResource(R.string.camera_gap_desc),
                        value = prefsState.ringGap,
                        onValueChange = { value ->
                            onSavePrefs(PrefsManager.KEY_RING_GAP, value)
                        },
                        onReset = {
                            onSavePrefs(PrefsManager.KEY_RING_GAP, PrefsManager.DEFAULT_RING_GAP)
                        },
                        valueRange = PrefsManager.MIN_RING_GAP..PrefsManager.MAX_RING_GAP,
                        valueLabel = { "%.2fx".format(it) },
                        defaultValue = PrefsManager.DEFAULT_RING_GAP,
                        stepSize = 0.01f,
                        hapticInterval = 0.05f,
                    )
                }
                item {
                    TweakSlider(
                        title = stringResource(R.string.opacity),
                        description = stringResource(R.string.opacity_desc),
                        value = prefsState.opacity.toFloat(),
                        onValueChange = { value ->
                            onSavePrefs(PrefsManager.KEY_OPACITY, value.toInt())
                        },
                        onReset = {
                            onSavePrefs(PrefsManager.KEY_OPACITY, PrefsManager.DEFAULT_OPACITY)
                        },
                        valueRange = PrefsManager.MIN_OPACITY.toFloat()..PrefsManager.MAX_OPACITY.toFloat(),
                        valueLabel = { "${it.toInt()}%" },
                        defaultValue = PrefsManager.DEFAULT_OPACITY.toFloat(),
                        stepSize = 1f,
                        hapticInterval = 5f,
                    )
                }
            }
        }

        item(key = "design_percent_header") {
            SectionHeader(title = stringResource(R.string.group_percent_text))
        }

        item(key = "design_percent_group") {
            SettingsGroup {
                item {
                    TweakSwitch(
                        title = stringResource(R.string.percent_text),
                        description = stringResource(R.string.percent_text_desc),
                        checked = prefsState.percentTextEnabled,
                        onCheckedChange = { checked ->
                            onSavePrefs(PrefsManager.KEY_PERCENT_TEXT_ENABLED, checked)
                        },
                    )
                }
                item {
                    TweakSelection(
                        title = stringResource(R.string.percent_text_position),
                        description = stringResource(R.string.percent_text_position_desc),
                        currentValue = prefsState.percentTextPosition,
                        entries =
                            listOf(
                                stringResource(R.string.position_left),
                                stringResource(R.string.position_right),
                            ),
                        values = listOf("left", "right"),
                        onValueSelected = { value ->
                            onSavePrefs(PrefsManager.KEY_PERCENT_TEXT_POSITION, value)
                        },
                        enabled = prefsState.percentTextEnabled,
                    )
                }
            }
        }

        item(key = "design_filename_header") {
            SectionHeader(title = stringResource(R.string.group_filename_text))
        }

        item(key = "design_filename_group") {
            SettingsGroup {
                item {
                    TweakSwitch(
                        title = stringResource(R.string.filename_text),
                        description = stringResource(R.string.filename_text_desc),
                        checked = prefsState.filenameTextEnabled,
                        onCheckedChange = { checked ->
                            onSavePrefs(PrefsManager.KEY_FILENAME_TEXT_ENABLED, checked)
                        },
                    )
                }
                item {
                    TweakSelection(
                        title = stringResource(R.string.filename_text_position),
                        description = stringResource(R.string.filename_text_position_desc),
                        currentValue = prefsState.filenameTextPosition,
                        entries =
                            listOf(
                                stringResource(R.string.position_left),
                                stringResource(R.string.position_right),
                                stringResource(R.string.position_top_left),
                                stringResource(R.string.position_top_right),
                            ),
                        values = listOf("left", "right", "top_left", "top_right"),
                        onValueSelected = { value ->
                            onSavePrefs(PrefsManager.KEY_FILENAME_TEXT_POSITION, value)
                        },
                        enabled = prefsState.filenameTextEnabled,
                    )
                }
            }
        }
    }
}
