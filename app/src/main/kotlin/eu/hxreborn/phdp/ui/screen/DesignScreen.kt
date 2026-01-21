package eu.hxreborn.phdp.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.PrefsManager
import eu.hxreborn.phdp.ui.component.SectionCard
import eu.hxreborn.phdp.ui.component.preference.ColorPreference
import eu.hxreborn.phdp.ui.component.preference.SelectPreference
import eu.hxreborn.phdp.ui.component.preference.SliderPreferenceWithReset
import eu.hxreborn.phdp.ui.component.preference.TogglePreferenceWithIcon
import eu.hxreborn.phdp.ui.state.PrefsState
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.Tokens
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceCategory

@Composable
fun DesignScreen(
    prefsState: PrefsState,
    onSavePrefs: (key: String, value: Any) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
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
                key = "design_colors_header",
                title = { Text(stringResource(R.string.group_colors)) },
            )

            item(key = "design_colors_section") {
                SectionCard(
                    items =
                        listOf(
                            {
                                ColorPreference(
                                    value = prefsState.color,
                                    onValueChange = { onSavePrefs(PrefsManager.KEY_COLOR, it) },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_progress_color_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_progress_color_summary),
                                        )
                                    },
                                )
                            },
                            {
                                ColorPreference(
                                    value = prefsState.finishFlashColor,
                                    onValueChange = {
                                        onSavePrefs(
                                            PrefsManager.KEY_FINISH_FLASH_COLOR,
                                            it,
                                        )
                                    },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_success_color_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_success_color_summary),
                                        )
                                    },
                                )
                            },
                        ),
                )
            }

            preferenceCategory(
                key = "design_geometry_header",
                title = { Text(stringResource(R.string.group_geometry)) },
            )

            item(key = "design_geometry_section") {
                SectionCard(
                    items =
                        listOf(
                            {
                                SliderPreferenceWithReset(
                                    value = prefsState.strokeWidth,
                                    onValueChange = {
                                        onSavePrefs(
                                            PrefsManager.KEY_STROKE_WIDTH,
                                            it,
                                        )
                                    },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_stroke_width_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_stroke_width_summary),
                                        )
                                    },
                                    valueRange =
                                        PrefsManager.MIN_STROKE_WIDTH..PrefsManager.MAX_STROKE_WIDTH,
                                    defaultValue = PrefsManager.DEFAULT_STROKE_WIDTH,
                                    onReset = {
                                        onSavePrefs(
                                            PrefsManager.KEY_STROKE_WIDTH,
                                            PrefsManager.DEFAULT_STROKE_WIDTH,
                                        )
                                    },
                                    valueText = { Text("%.1fdp".format(it)) },
                                )
                            },
                            {
                                SliderPreferenceWithReset(
                                    value = prefsState.ringGap,
                                    onValueChange = { onSavePrefs(PrefsManager.KEY_RING_GAP, it) },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_cutout_padding_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_cutout_padding_summary),
                                        )
                                    },
                                    valueRange =
                                        PrefsManager.MIN_RING_GAP..PrefsManager.MAX_RING_GAP,
                                    defaultValue = PrefsManager.DEFAULT_RING_GAP,
                                    onReset = {
                                        onSavePrefs(
                                            PrefsManager.KEY_RING_GAP,
                                            PrefsManager.DEFAULT_RING_GAP,
                                        )
                                    },
                                    valueText = { Text("%.2fx".format(it)) },
                                )
                            },
                            {
                                SliderPreferenceWithReset(
                                    value = prefsState.opacity.toFloat(),
                                    onValueChange = {
                                        onSavePrefs(
                                            PrefsManager.KEY_OPACITY,
                                            it.toInt(),
                                        )
                                    },
                                    title = { Text(stringResource(R.string.pref_opacity_title)) },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_opacity_summary),
                                        )
                                    },
                                    valueRange =
                                        PrefsManager.MIN_OPACITY.toFloat()..PrefsManager.MAX_OPACITY.toFloat(),
                                    defaultValue = PrefsManager.DEFAULT_OPACITY.toFloat(),
                                    onReset = {
                                        onSavePrefs(
                                            PrefsManager.KEY_OPACITY,
                                            PrefsManager.DEFAULT_OPACITY,
                                        )
                                    },
                                    valueText = { Text("${it.toInt()}%") },
                                )
                            },
                        ),
                )
            }

            preferenceCategory(
                key = "design_percent_header",
                title = { Text(stringResource(R.string.group_percent_text)) },
            )

            item(key = "design_percent_section") {
                SectionCard(
                    items =
                        listOf(
                            {
                                TogglePreferenceWithIcon(
                                    value = prefsState.percentTextEnabled,
                                    onValueChange = {
                                        onSavePrefs(
                                            PrefsManager.KEY_PERCENT_TEXT_ENABLED,
                                            it,
                                        )
                                    },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_show_percentage_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_show_percentage_summary),
                                        )
                                    },
                                )
                            },
                            {
                                SelectPreference(
                                    value = prefsState.percentTextPosition,
                                    onValueChange = {
                                        onSavePrefs(
                                            PrefsManager.KEY_PERCENT_TEXT_POSITION,
                                            it,
                                        )
                                    },
                                    values =
                                        listOf(
                                            "left",
                                            "right",
                                            "top_left",
                                            "top_right",
                                            "bottom_left",
                                            "bottom_right",
                                            "top",
                                            "bottom",
                                        ),
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_text_position_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_text_position_summary),
                                        )
                                    },
                                    enabled = prefsState.percentTextEnabled,
                                    valueToText = { positionLabelPlain(it) },
                                )
                            },
                        ),
                )
            }

            preferenceCategory(
                key = "design_filename_header",
                title = { Text(stringResource(R.string.group_filename_text)) },
            )

            item(key = "design_filename_section") {
                SectionCard(
                    items =
                        listOf(
                            {
                                TogglePreferenceWithIcon(
                                    value = prefsState.filenameTextEnabled,
                                    onValueChange = {
                                        onSavePrefs(
                                            PrefsManager.KEY_FILENAME_TEXT_ENABLED,
                                            it,
                                        )
                                    },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_show_filename_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_show_filename_summary),
                                        )
                                    },
                                )
                            },
                            {
                                SelectPreference(
                                    value = prefsState.filenameTextPosition,
                                    onValueChange = {
                                        onSavePrefs(
                                            PrefsManager.KEY_FILENAME_TEXT_POSITION,
                                            it,
                                        )
                                    },
                                    values =
                                        listOf(
                                            "left",
                                            "right",
                                            "top_left",
                                            "top_right",
                                            "bottom_left",
                                            "bottom_right",
                                            "top",
                                            "bottom",
                                        ),
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_filename_position_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_filename_position_summary),
                                        )
                                    },
                                    enabled = prefsState.filenameTextEnabled,
                                    valueToText = { positionLabelPlain(it) },
                                )
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun positionLabel(position: String): String =
    when (position) {
        "left" -> stringResource(R.string.position_left)
        "right" -> stringResource(R.string.position_right)
        "top_left" -> stringResource(R.string.position_top_left)
        "top_right" -> stringResource(R.string.position_top_right)
        else -> position
    }

private fun positionLabelPlain(position: String): String =
    when (position) {
        "left" -> "Left"
        "right" -> "Right"
        "top_left" -> "Top Left"
        "top_right" -> "Top Right"
        "bottom_left" -> "Bottom Left"
        "bottom_right" -> "Bottom Right"
        "top" -> "Top"
        "bottom" -> "Bottom"
        else -> position
    }

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DesignScreenPreview() {
    AppTheme(darkTheme = true) {
        DesignScreen(
            prefsState = PrefsState(),
            onSavePrefs = { _, _ -> },
            contentPadding = PaddingValues(),
        )
    }
}
