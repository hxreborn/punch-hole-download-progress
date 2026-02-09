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
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.ui.component.SectionCard
import eu.hxreborn.phdp.ui.component.preference.ColorPreference
import eu.hxreborn.phdp.ui.component.preference.NavigationPreference
import eu.hxreborn.phdp.ui.component.preference.SelectPreference
import eu.hxreborn.phdp.ui.component.preference.SliderPreferenceWithReset
import eu.hxreborn.phdp.ui.component.preference.TogglePreferenceWithIcon
import eu.hxreborn.phdp.ui.state.PrefsState
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig
import eu.hxreborn.phdp.ui.theme.MaterialPalette
import eu.hxreborn.phdp.ui.theme.Tokens
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceCategory

@Composable
fun AppearanceScreen(
    prefsState: PrefsState,
    onSavePrefs: (key: String, value: Any) -> Unit,
    onNavigateToCalibration: () -> Unit,
    onNavigateToPercentCalibration: () -> Unit,
    onNavigateToFilenameCalibration: () -> Unit,
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
                                    onValueChange = { onSavePrefs(Prefs.color.key, it) },
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
                                    onValueChange = { onSavePrefs(Prefs.finishFlashColor.key, it) },
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
                            {
                                ColorPreference(
                                    value = prefsState.errorColor,
                                    onValueChange = { onSavePrefs(Prefs.errorColor.key, it) },
                                    title = { Text(stringResource(R.string.pref_error_color_title)) },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_error_color_summary),
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
                                    onValueChange = { onSavePrefs(Prefs.strokeWidth.key, it) },
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
                                    valueRange = Prefs.strokeWidth.range!!,
                                    defaultValue = Prefs.strokeWidth.default,
                                    onReset = {
                                        onSavePrefs(Prefs.strokeWidth.key, Prefs.strokeWidth.default)
                                    },
                                    valueText = { Text("%.1fdp".format(it)) },
                                )
                            },
                            {
                                SliderPreferenceWithReset(
                                    value = prefsState.ringGap,
                                    onValueChange = { onSavePrefs(Prefs.ringGap.key, it) },
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
                                    valueRange = Prefs.ringGap.range!!,
                                    defaultValue = Prefs.ringGap.default,
                                    onReset = {
                                        onSavePrefs(Prefs.ringGap.key, Prefs.ringGap.default)
                                    },
                                    valueText = { Text("%.2fx".format(it)) },
                                )
                            },
                            {
                                val opacityRange = Prefs.opacity.range!!
                                SliderPreferenceWithReset(
                                    value = prefsState.opacity.toFloat(),
                                    onValueChange = { onSavePrefs(Prefs.opacity.key, it.toInt()) },
                                    title = { Text(stringResource(R.string.pref_opacity_title)) },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_opacity_summary),
                                        )
                                    },
                                    valueRange = opacityRange.first.toFloat()..opacityRange.last.toFloat(),
                                    defaultValue = Prefs.opacity.default.toFloat(),
                                    onReset = {
                                        onSavePrefs(Prefs.opacity.key, Prefs.opacity.default)
                                    },
                                    valueText = { Text("${it.toInt()}%") },
                                )
                            },
                            {
                                SelectPreference(
                                    value = prefsState.strokeCapStyle,
                                    onValueChange = {
                                        onSavePrefs(Prefs.strokeCapStyle.key, it)
                                    },
                                    values = listOf("flat", "round", "square"),
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_stroke_cap_style_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_stroke_cap_style_summary),
                                        )
                                    },
                                    valueToText = { strokeCapLabel(it) },
                                )
                            },
                            {
                                NavigationPreference(
                                    onClick = onNavigateToCalibration,
                                    title = {
                                        Text(stringResource(R.string.pref_calibrate_ring_title))
                                    },
                                    summary = {
                                        Text(stringResource(R.string.pref_calibrate_ring_summary))
                                    },
                                )
                            },
                        ),
                )
            }

            preferenceCategory(
                key = "design_background_ring_header",
                title = { Text(stringResource(R.string.group_background_ring)) },
            )

            item(key = "design_background_ring_section") {
                SectionCard(
                    enabled = prefsState.backgroundRingEnabled,
                    items =
                        listOf(
                            {
                                TogglePreferenceWithIcon(
                                    value = prefsState.backgroundRingEnabled,
                                    onValueChange = {
                                        onSavePrefs(Prefs.backgroundRingEnabled.key, it)
                                    },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_background_ring_enabled_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_background_ring_enabled_summary),
                                        )
                                    },
                                )
                            },
                            {
                                ColorPreference(
                                    value = prefsState.backgroundRingColor,
                                    onValueChange = {
                                        onSavePrefs(Prefs.backgroundRingColor.key, it)
                                    },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_background_ring_color_title),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_background_ring_color_summary),
                                        )
                                    },
                                    enabled = prefsState.backgroundRingEnabled,
                                    colors = MaterialPalette.backgroundColors,
                                )
                            },
                            {
                                val opacityRange = Prefs.backgroundRingOpacity.range!!
                                SliderPreferenceWithReset(
                                    value = prefsState.backgroundRingOpacity.toFloat(),
                                    onValueChange = {
                                        onSavePrefs(Prefs.backgroundRingOpacity.key, it.toInt())
                                    },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_background_ring_opacity_title),
                                        )
                                    },
                                    valueRange = opacityRange.first.toFloat()..opacityRange.last.toFloat(),
                                    defaultValue = Prefs.backgroundRingOpacity.default.toFloat(),
                                    onReset = {
                                        onSavePrefs(
                                            Prefs.backgroundRingOpacity.key,
                                            Prefs.backgroundRingOpacity.default,
                                        )
                                    },
                                    valueText = { Text("${it.toInt()}%") },
                                    enabled = prefsState.backgroundRingEnabled,
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
                                        onSavePrefs(Prefs.percentTextEnabled.key, it)
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
                                NavigationPreference(
                                    onClick = onNavigateToPercentCalibration,
                                    title = {
                                        Text(
                                            stringResource(
                                                R.string.pref_calibrate_percent_title,
                                            ),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(
                                                R.string.pref_calibrate_percent_summary,
                                            ),
                                        )
                                    },
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
                                        onSavePrefs(Prefs.filenameTextEnabled.key, it)
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
                                NavigationPreference(
                                    onClick = onNavigateToFilenameCalibration,
                                    title = {
                                        Text(
                                            stringResource(
                                                R.string.pref_calibrate_filename_title,
                                            ),
                                        )
                                    },
                                    summary = {
                                        Text(
                                            stringResource(
                                                R.string.pref_calibrate_filename_summary,
                                            ),
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

private fun strokeCapLabel(style: String): String =
    when (style) {
        "flat" -> "Flat"
        "round" -> "Semicircle"
        "square" -> "Square"
        else -> style
    }

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppearanceScreenPreview() {
    AppTheme(darkThemeConfig = DarkThemeConfig.DARK) {
        AppearanceScreen(
            prefsState = PrefsState(),
            onSavePrefs = { _, _ -> },
            onNavigateToCalibration = {},
            onNavigateToPercentCalibration = {},
            onNavigateToFilenameCalibration = {},
            contentPadding = PaddingValues(),
        )
    }
}
