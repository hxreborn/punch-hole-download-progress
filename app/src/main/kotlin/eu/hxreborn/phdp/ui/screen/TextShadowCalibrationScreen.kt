package eu.hxreborn.phdp.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.BoundPref
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.prefs.bind
import eu.hxreborn.phdp.ui.SettingsViewModel
import eu.hxreborn.phdp.ui.component.SectionCard
import eu.hxreborn.phdp.ui.component.SettingsScaffold
import eu.hxreborn.phdp.ui.component.preference.ColorPreference
import eu.hxreborn.phdp.ui.component.preference.SelectPreference
import eu.hxreborn.phdp.ui.component.preference.SliderPreferenceWithReset
import eu.hxreborn.phdp.ui.state.AppPrefs
import eu.hxreborn.phdp.ui.theme.MaterialPalette
import eu.hxreborn.phdp.ui.theme.Tokens
import me.zhanghai.compose.preference.ProvidePreferenceLocals

data class TextShadowBindings(
    val mode: BoundPref<String>,
    val color: BoundPref<Int>,
    val radius: BoundPref<Float>,
    val dy: BoundPref<Float>,
    val opacity: BoundPref<Int>,
    val strokeWidth: BoundPref<Float>,
    val strokeColor: BoundPref<Int>,
    val radiusRange: ClosedFloatingPointRange<Float>,
    val dyRange: ClosedFloatingPointRange<Float>,
    val opacityRange: IntRange,
    val strokeWidthRange: ClosedFloatingPointRange<Float>,
) {
    companion object {
        fun forPercent(prefs: AppPrefs): TextShadowBindings =
            TextShadowBindings(
                mode = Prefs.percentTextShadowMode bind prefs.percentTextShadowMode,
                color = Prefs.percentTextShadowColor bind prefs.percentTextShadowColor,
                radius = Prefs.percentTextShadowRadius bind prefs.percentTextShadowRadius,
                dy = Prefs.percentTextShadowDy bind prefs.percentTextShadowDy,
                opacity = Prefs.percentTextShadowOpacity bind prefs.percentTextShadowOpacity,
                strokeWidth = Prefs.percentTextStrokeWidth bind prefs.percentTextStrokeWidth,
                strokeColor = Prefs.percentTextStrokeColor bind prefs.percentTextStrokeColor,
                radiusRange = Prefs.percentTextShadowRadius.range!!,
                dyRange = Prefs.percentTextShadowDy.range!!,
                opacityRange = Prefs.percentTextShadowOpacity.range!!,
                strokeWidthRange = Prefs.percentTextStrokeWidth.range!!,
            )

        fun forFilename(prefs: AppPrefs): TextShadowBindings =
            TextShadowBindings(
                mode = Prefs.filenameTextShadowMode bind prefs.filenameTextShadowMode,
                color = Prefs.filenameTextShadowColor bind prefs.filenameTextShadowColor,
                radius = Prefs.filenameTextShadowRadius bind prefs.filenameTextShadowRadius,
                dy = Prefs.filenameTextShadowDy bind prefs.filenameTextShadowDy,
                opacity = Prefs.filenameTextShadowOpacity bind prefs.filenameTextShadowOpacity,
                strokeWidth = Prefs.filenameTextStrokeWidth bind prefs.filenameTextStrokeWidth,
                strokeColor = Prefs.filenameTextStrokeColor bind prefs.filenameTextStrokeColor,
                radiusRange = Prefs.filenameTextShadowRadius.range!!,
                dyRange = Prefs.filenameTextShadowDy.range!!,
                opacityRange = Prefs.filenameTextShadowOpacity.range!!,
                strokeWidthRange = Prefs.filenameTextStrokeWidth.range!!,
            )
    }
}

@Composable
fun TextShadowCalibrationScreen(
    titleRes: Int,
    bindings: TextShadowBindings,
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomNavPadding: Dp = 0.dp,
) {
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.savePref(Prefs.persistentPreview, true)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.savePref(Prefs.persistentPreview, false)
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.savePref(Prefs.persistentPreview, false) }
    }

    SettingsScaffold(
        title = stringResource(titleRes),
        onNavigateBack = onNavigateBack,
        bottomPadding = bottomNavPadding,
        modifier = modifier,
    ) { innerPadding ->
        ProvidePreferenceLocals {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        top = innerPadding.calculateTopPadding() + Tokens.SpacingLg,
                        bottom = innerPadding.calculateBottomPadding() + Tokens.SpacingLg,
                    ),
            ) {
                item(key = "text_shadow_cal_section") {
                    SectionCard(
                        items =
                            buildList {
                                add {
                                    val perGlyph = stringResource(R.string.pref_text_shadow_mode_per_glyph)
                                    val oval = stringResource(R.string.pref_text_shadow_mode_oval)
                                    SelectPreference(
                                        value = bindings.mode.value,
                                        onValueChange = { viewModel.savePref(bindings.mode.spec, it) },
                                        values = listOf("per_glyph", "oval"),
                                        title = { Text(stringResource(R.string.pref_text_shadow_mode_title)) },
                                        summary = { Text(stringResource(R.string.pref_text_shadow_mode_summary)) },
                                        valueToText = { mode -> if (mode == "oval") oval else perGlyph },
                                    )
                                }
                                add {
                                    ColorPreference(
                                        value = bindings.color.value,
                                        onValueChange = { viewModel.savePref(bindings.color.spec, it) },
                                        title = { Text(stringResource(R.string.pref_text_shadow_color_title)) },
                                        summary = { Text(stringResource(R.string.pref_text_shadow_color_summary)) },
                                        colors = MaterialPalette.materialColors,
                                    )
                                }
                                add {
                                    SliderPreferenceWithReset(
                                        value = bindings.radius.value,
                                        onValueChange = { viewModel.savePref(bindings.radius.spec, it) },
                                        title = { Text(stringResource(R.string.pref_text_shadow_radius_title)) },
                                        summary = { Text(stringResource(R.string.pref_text_shadow_radius_summary)) },
                                        valueRange = bindings.radiusRange,
                                        defaultValue = bindings.radius.spec.default,
                                        onReset = {
                                            viewModel.savePref(bindings.radius.spec, bindings.radius.spec.default)
                                        },
                                        valueText = { Text("%.1fdp".format(it)) },
                                    )
                                }
                                add {
                                    SliderPreferenceWithReset(
                                        value = bindings.dy.value,
                                        onValueChange = { viewModel.savePref(bindings.dy.spec, it) },
                                        title = { Text(stringResource(R.string.pref_text_shadow_dy_title)) },
                                        summary = { Text(stringResource(R.string.pref_text_shadow_dy_summary)) },
                                        valueRange = bindings.dyRange,
                                        defaultValue = bindings.dy.spec.default,
                                        onReset = {
                                            viewModel.savePref(bindings.dy.spec, bindings.dy.spec.default)
                                        },
                                        valueText = { Text("%.1fdp".format(it)) },
                                    )
                                }
                                add {
                                    SliderPreferenceWithReset(
                                        value = bindings.opacity.value.toFloat(),
                                        onValueChange = {
                                            viewModel.savePref(bindings.opacity.spec, it.toInt())
                                        },
                                        title = { Text(stringResource(R.string.pref_text_shadow_opacity_title)) },
                                        summary = { Text(stringResource(R.string.pref_text_shadow_opacity_summary)) },
                                        valueRange = bindings.opacityRange.first.toFloat()..bindings.opacityRange.last.toFloat(),
                                        defaultValue =
                                            bindings.opacity.spec.default
                                                .toFloat(),
                                        onReset = {
                                            viewModel.savePref(
                                                bindings.opacity.spec,
                                                bindings.opacity.spec.default,
                                            )
                                        },
                                        valueText = { Text("${it.toInt()}%") },
                                    )
                                }
                                add {
                                    SliderPreferenceWithReset(
                                        value = bindings.strokeWidth.value,
                                        onValueChange = { viewModel.savePref(bindings.strokeWidth.spec, it) },
                                        title = { Text(stringResource(R.string.pref_text_stroke_width_title)) },
                                        summary = { Text(stringResource(R.string.pref_text_stroke_width_summary)) },
                                        valueRange = bindings.strokeWidthRange,
                                        defaultValue = bindings.strokeWidth.spec.default,
                                        onReset = {
                                            viewModel.savePref(
                                                bindings.strokeWidth.spec,
                                                bindings.strokeWidth.spec.default,
                                            )
                                        },
                                        valueText = { Text("%.1fdp".format(it)) },
                                    )
                                }
                                if (bindings.strokeWidth.value > 0f) {
                                    add {
                                        ColorPreference(
                                            value = bindings.strokeColor.value,
                                            onValueChange = {
                                                viewModel.savePref(bindings.strokeColor.spec, it)
                                            },
                                            title = { Text(stringResource(R.string.pref_text_stroke_color_title)) },
                                            summary = { Text(stringResource(R.string.pref_text_stroke_color_summary)) },
                                            colors = MaterialPalette.materialColors,
                                        )
                                    }
                                }
                            },
                    )
                }
            }
        }
    }
}
