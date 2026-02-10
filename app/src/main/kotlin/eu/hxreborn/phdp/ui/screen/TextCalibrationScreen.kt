package eu.hxreborn.phdp.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.BoundPref
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.prefs.bind
import eu.hxreborn.phdp.ui.SettingsViewModel
import eu.hxreborn.phdp.ui.component.SectionCard
import eu.hxreborn.phdp.ui.component.SettingsScaffold
import eu.hxreborn.phdp.ui.component.preference.SelectPreference
import eu.hxreborn.phdp.ui.component.preference.SliderPreferenceWithStepper
import eu.hxreborn.phdp.ui.component.preference.TextInputPreference
import eu.hxreborn.phdp.ui.component.preference.TextStyleChipsPreference
import eu.hxreborn.phdp.ui.component.preference.TogglePreferenceWithIcon
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig
import eu.hxreborn.phdp.ui.theme.Tokens
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceCategory

data class TypographyConfig(
    val fontSize: BoundPref<Float>,
    val bold: BoundPref<Boolean>? = null,
    val italic: BoundPref<Boolean>? = null,
)

data class LayoutConfig(
    val truncateEnabled: BoundPref<Boolean>,
    val maxLength: BoundPref<Int>? = null,
    val maxLengthTitleRes: Int? = null,
    val maxLengthSuffix: String? = null,
    val ellipsize: BoundPref<String>? = null,
    val ellipsizeValues: List<String>? = null,
    val previewText: BoundPref<String>? = null,
)

@Composable
fun TextCalibrationScreen(
    titleRes: Int,
    offsetX: BoundPref<Float>,
    offsetY: BoundPref<Float>,
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    typography: TypographyConfig? = null,
    layout: LayoutConfig? = null,
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

    val hasTypography = typography != null
    val hasLayout = layout != null
    val splitCards = hasTypography && hasLayout

    SettingsScaffold(
        title = stringResource(titleRes),
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    ) { innerPadding ->
        ProvidePreferenceLocals {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        top = innerPadding.calculateTopPadding() + Tokens.SpacingLg,
                        bottom = contentPadding.calculateBottomPadding() + Tokens.SpacingLg,
                    ),
            ) {
                if (splitCards) {
                    preferenceCategory(
                        key = "text_cal_typography_header",
                        title = { Text(stringResource(R.string.group_typography)) },
                    )
                    item(key = "text_cal_typography") {
                        SectionCard(
                            items =
                                buildList {
                                    typographyItems(typography, viewModel)
                                },
                        )
                    }
                    preferenceCategory(
                        key = "text_cal_layout_header",
                        title = { Text(stringResource(R.string.group_layout)) },
                    )
                    item(key = "text_cal_layout") {
                        SectionCard(
                            items =
                                buildList {
                                    offsetItems(offsetX, offsetY, viewModel)
                                    layoutItems(layout, viewModel)
                                },
                        )
                    }
                } else {
                    // Single card for simple screens (badge calibration)
                    item(key = "text_cal_all") {
                        SectionCard(
                            items =
                                buildList {
                                    typographyItems(typography, viewModel)
                                    offsetItems(offsetX, offsetY, viewModel)
                                },
                        )
                    }
                }
            }
        }
    }
}

private fun MutableList<@Composable () -> Unit>.typographyItems(
    typography: TypographyConfig?,
    viewModel: SettingsViewModel,
) {
    if (typography == null) return
    val (fontSize, bold, italic) = typography
    add {
        SliderPreferenceWithStepper(
            value = fontSize.value,
            onValueChange = { viewModel.savePref(fontSize.spec, it) },
            title = { Text(stringResource(R.string.pref_font_size_title)) },
            valueRange = (fontSize.spec as eu.hxreborn.phdp.prefs.FloatPref).range!!,
            defaultValue = fontSize.spec.default,
            onReset = { viewModel.savePref(fontSize.spec, fontSize.spec.default) },
            stepSize = 0.5f,
            decimalPlaces = 1,
            suffix = "sp",
        )
    }
    if (bold != null && italic != null) {
        add {
            TextStyleChipsPreference(
                bold = bold.value,
                onBoldChange = { viewModel.savePref(bold.spec, it) },
                italic = italic.value,
                onItalicChange = { viewModel.savePref(italic.spec, it) },
                title = { Text(stringResource(R.string.pref_text_style_title)) },
            )
        }
    } else if (bold != null) {
        add {
            TogglePreferenceWithIcon(
                value = bold.value,
                onValueChange = { viewModel.savePref(bold.spec, it) },
                title = { Text(stringResource(R.string.pref_bold_title)) },
            )
        }
    }
}

private fun MutableList<@Composable () -> Unit>.offsetItems(
    offsetX: BoundPref<Float>,
    offsetY: BoundPref<Float>,
    viewModel: SettingsViewModel,
) {
    val xSpec = offsetX.spec as eu.hxreborn.phdp.prefs.FloatPref
    val ySpec = offsetY.spec as eu.hxreborn.phdp.prefs.FloatPref
    add {
        SliderPreferenceWithStepper(
            value = offsetX.value,
            onValueChange = { viewModel.savePref(xSpec, it) },
            title = { Text(stringResource(R.string.pref_ring_offset_x_title)) },
            valueRange = xSpec.range!!,
            defaultValue = xSpec.default,
            onReset = { viewModel.savePref(xSpec, xSpec.default) },
            stepSize = 1f,
            decimalPlaces = 0,
            suffix = "px",
        )
    }
    add {
        SliderPreferenceWithStepper(
            value = offsetY.value,
            onValueChange = { viewModel.savePref(ySpec, it) },
            title = { Text(stringResource(R.string.pref_ring_offset_y_title)) },
            valueRange = ySpec.range!!,
            defaultValue = ySpec.default,
            onReset = { viewModel.savePref(ySpec, ySpec.default) },
            stepSize = 1f,
            decimalPlaces = 0,
            suffix = "px",
        )
    }
}

private fun MutableList<@Composable () -> Unit>.layoutItems(
    layout: LayoutConfig?,
    viewModel: SettingsViewModel,
) {
    if (layout == null) return
    val truncateEnabled = layout.truncateEnabled
    add {
        TogglePreferenceWithIcon(
            value = truncateEnabled.value,
            onValueChange = { viewModel.savePref(truncateEnabled.spec, it) },
            title = { Text(stringResource(R.string.pref_filename_truncate_title)) },
        )
    }
    val maxLength = layout.maxLength
    if (maxLength != null && layout.maxLengthTitleRes != null) {
        val intSpec = maxLength.spec as eu.hxreborn.phdp.prefs.IntPref
        add {
            SliderPreferenceWithStepper(
                value = maxLength.value.toFloat(),
                onValueChange = { viewModel.savePref(intSpec, it.toInt()) },
                title = { Text(stringResource(layout.maxLengthTitleRes)) },
                valueRange =
                    intSpec.range!!.let {
                        it.first.toFloat()..it.last.toFloat()
                    },
                defaultValue = intSpec.default.toFloat(),
                onReset = { viewModel.savePref(intSpec, intSpec.default) },
                stepSize = 1f,
                decimalPlaces = 0,
                suffix = layout.maxLengthSuffix ?: "",
                enabled = truncateEnabled.value,
            )
        }
    }
    val ellipsize = layout.ellipsize
    if (ellipsize != null && layout.ellipsizeValues != null) {
        add {
            SelectPreference(
                value = ellipsize.value,
                onValueChange = { viewModel.savePref(ellipsize.spec, it) },
                values = layout.ellipsizeValues,
                title = { Text(stringResource(R.string.pref_ellipsize_title)) },
                enabled = truncateEnabled.value,
                valueToText = { it.replaceFirstChar { c -> c.uppercase() } },
            )
        }
    }
    val previewText = layout.previewText
    if (previewText != null) {
        add {
            TextInputPreference(
                value = previewText.value,
                onValueChange = { viewModel.savePref(previewText.spec, it) },
                title = { Text(stringResource(R.string.pref_preview_text_title)) },
                defaultValue = previewText.spec.default,
            )
        }
    }
}

@Suppress("ViewModelConstructorInComposable")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TextCalibrationScreenPreview() {
    AppTheme(darkThemeConfig = DarkThemeConfig.DARK) {
        TextCalibrationScreen(
            titleRes = R.string.pref_calibrate_filename_title,
            offsetX = Prefs.filenameTextOffsetX bind 0f,
            offsetY = Prefs.filenameTextOffsetY bind 0f,
            viewModel = PreviewViewModel(),
            onNavigateBack = {},
            contentPadding = PaddingValues(),
            typography =
                TypographyConfig(
                    fontSize = Prefs.filenameTextSize bind 7f,
                    bold = Prefs.filenameTextBold bind false,
                    italic = Prefs.filenameTextItalic bind false,
                ),
            layout =
                LayoutConfig(
                    truncateEnabled = Prefs.filenameTruncateEnabled bind true,
                    maxLength = Prefs.filenameMaxChars bind 20,
                    maxLengthTitleRes = R.string.pref_filename_max_chars_title,
                    ellipsize = Prefs.filenameEllipsize bind "middle",
                    ellipsizeValues = listOf("start", "middle", "end"),
                    previewText = Prefs.previewFilenameText bind Prefs.previewFilenameText.default,
                ),
        )
    }
}
