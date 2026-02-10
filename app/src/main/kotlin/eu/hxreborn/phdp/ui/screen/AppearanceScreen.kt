package eu.hxreborn.phdp.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.PrefSpec
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.ui.SettingsUiState
import eu.hxreborn.phdp.ui.SettingsViewModel
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceCategory

enum class CalibrationTarget { RING, PERCENT, FILENAME }

@Composable
fun AppearanceScreen(
    viewModel: SettingsViewModel,
    onNavigateToCalibration: (CalibrationTarget) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefsState = (uiState as? SettingsUiState.Success)?.prefs ?: return

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
                                    onValueChange = { viewModel.savePref(Prefs.color, it) },
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
                                    onValueChange = { viewModel.savePref(Prefs.finishFlashColor, it) },
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
                                    onValueChange = { viewModel.savePref(Prefs.errorColor, it) },
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
                                    onValueChange = { viewModel.savePref(Prefs.strokeWidth, it) },
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
                                        viewModel.savePref(Prefs.strokeWidth, Prefs.strokeWidth.default)
                                    },
                                    valueText = { Text("%.1fdp".format(it)) },
                                )
                            },
                            {
                                SliderPreferenceWithReset(
                                    value = prefsState.ringGap,
                                    onValueChange = { viewModel.savePref(Prefs.ringGap, it) },
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
                                        viewModel.savePref(Prefs.ringGap, Prefs.ringGap.default)
                                    },
                                    valueText = { Text("%.2fx".format(it)) },
                                )
                            },
                            {
                                val opacityRange = Prefs.opacity.range!!
                                SliderPreferenceWithReset(
                                    value = prefsState.opacity.toFloat(),
                                    onValueChange = { viewModel.savePref(Prefs.opacity, it.toInt()) },
                                    title = { Text(stringResource(R.string.pref_opacity_title)) },
                                    summary = {
                                        Text(
                                            stringResource(R.string.pref_opacity_summary),
                                        )
                                    },
                                    valueRange = opacityRange.first.toFloat()..opacityRange.last.toFloat(),
                                    defaultValue = Prefs.opacity.default.toFloat(),
                                    onReset = {
                                        viewModel.savePref(Prefs.opacity, Prefs.opacity.default)
                                    },
                                    valueText = { Text("${it.toInt()}%") },
                                )
                            },
                            {
                                SelectPreference(
                                    value = prefsState.strokeCapStyle,
                                    onValueChange = {
                                        viewModel.savePref(Prefs.strokeCapStyle, it)
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
                                    onClick = { onNavigateToCalibration(CalibrationTarget.RING) },
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
                                        viewModel.savePref(Prefs.backgroundRingEnabled, it)
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
                                        viewModel.savePref(Prefs.backgroundRingColor, it)
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
                                        viewModel.savePref(Prefs.backgroundRingOpacity, it.toInt())
                                    },
                                    title = {
                                        Text(
                                            stringResource(R.string.pref_background_ring_opacity_title),
                                        )
                                    },
                                    valueRange = opacityRange.first.toFloat()..opacityRange.last.toFloat(),
                                    defaultValue = Prefs.backgroundRingOpacity.default.toFloat(),
                                    onReset = {
                                        viewModel.savePref(
                                            Prefs.backgroundRingOpacity,
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
                                        viewModel.savePref(Prefs.percentTextEnabled, it)
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
                                    onClick = { onNavigateToCalibration(CalibrationTarget.PERCENT) },
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
                                        viewModel.savePref(Prefs.filenameTextEnabled, it)
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
                                    onClick = { onNavigateToCalibration(CalibrationTarget.FILENAME) },
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

@Suppress("ViewModelConstructorInComposable")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppearanceScreenPreview() {
    AppTheme(darkThemeConfig = DarkThemeConfig.DARK) {
        AppearanceScreen(
            viewModel = PreviewViewModel(),
            onNavigateToCalibration = {},
            contentPadding = PaddingValues(),
        )
    }
}

internal class PreviewViewModel : SettingsViewModel() {
    override val uiState: StateFlow<SettingsUiState> =
        MutableStateFlow(SettingsUiState.Success(PrefsState())).asStateFlow()

    override fun <T : Any> savePref(
        pref: PrefSpec<T>,
        value: T,
    ) {}

    override fun resetDefaults() {}

    override fun simulateSuccess() {}

    override fun simulateFailure() {}

    override fun clearDownloads() {}
}
