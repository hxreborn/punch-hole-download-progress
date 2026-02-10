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
import eu.hxreborn.phdp.prefs.BoolPref
import eu.hxreborn.phdp.prefs.FloatPref
import eu.hxreborn.phdp.prefs.IntPref
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.prefs.StringPref
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

@Composable
fun TextCalibrationScreen(
    titleRes: Int,
    offsetX: Float,
    offsetY: Float,
    offsetXPref: FloatPref,
    offsetYPref: FloatPref,
    onSavePrefs: (key: String, value: Any) -> Unit,
    onNavigateBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    fontSize: Float? = null,
    fontSizePref: FloatPref? = null,
    bold: Boolean? = null,
    boldPref: BoolPref? = null,
    italic: Boolean? = null,
    italicPref: BoolPref? = null,
    truncateEnabled: Boolean? = null,
    truncateEnabledPref: BoolPref? = null,
    extraInt: Int? = null,
    extraIntPref: IntPref? = null,
    extraIntTitleRes: Int? = null,
    extraIntSuffix: String? = null,
    ellipsize: String? = null,
    ellipsizePref: StringPref? = null,
    ellipsizeValues: List<String>? = null,
    previewText: String? = null,
    previewTextPref: StringPref? = null,
) {
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        onSavePrefs(Prefs.persistentPreview.key, true)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        onSavePrefs(Prefs.persistentPreview.key, false)
    }
    DisposableEffect(Unit) {
        onDispose { onSavePrefs(Prefs.persistentPreview.key, false) }
    }

    val hasTypography = fontSize != null || bold != null || italic != null
    val hasLayout =
        truncateEnabled != null || extraInt != null ||
            ellipsize != null || previewText != null
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
                                    typographyItems(
                                        fontSize,
                                        fontSizePref,
                                        bold,
                                        boldPref,
                                        italic,
                                        italicPref,
                                        onSavePrefs,
                                    )
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
                                    offsetItems(
                                        offsetX,
                                        offsetXPref,
                                        offsetY,
                                        offsetYPref,
                                        onSavePrefs,
                                    )
                                    layoutItems(
                                        truncateEnabled,
                                        truncateEnabledPref,
                                        extraInt,
                                        extraIntPref,
                                        extraIntTitleRes,
                                        extraIntSuffix,
                                        ellipsize,
                                        ellipsizePref,
                                        ellipsizeValues,
                                        previewText,
                                        previewTextPref,
                                        onSavePrefs,
                                    )
                                },
                        )
                    }
                } else {
                    // Single card for simple screens (badge calibration)
                    item(key = "text_cal_all") {
                        SectionCard(
                            items =
                                buildList {
                                    typographyItems(
                                        fontSize,
                                        fontSizePref,
                                        bold,
                                        boldPref,
                                        italic,
                                        italicPref,
                                        onSavePrefs,
                                    )
                                    offsetItems(
                                        offsetX,
                                        offsetXPref,
                                        offsetY,
                                        offsetYPref,
                                        onSavePrefs,
                                    )
                                },
                        )
                    }
                }
            }
        }
    }
}

private fun MutableList<@Composable () -> Unit>.typographyItems(
    fontSize: Float?,
    fontSizePref: FloatPref?,
    bold: Boolean?,
    boldPref: BoolPref?,
    italic: Boolean?,
    italicPref: BoolPref?,
    onSavePrefs: (String, Any) -> Unit,
) {
    if (fontSize != null && fontSizePref != null) {
        add {
            SliderPreferenceWithStepper(
                value = fontSize,
                onValueChange = { onSavePrefs(fontSizePref.key, it) },
                title = { Text(stringResource(R.string.pref_font_size_title)) },
                valueRange = fontSizePref.range!!,
                defaultValue = fontSizePref.default,
                onReset = { onSavePrefs(fontSizePref.key, fontSizePref.default) },
                stepSize = 0.5f,
                decimalPlaces = 1,
                suffix = "sp",
            )
        }
    }
    if (bold != null && boldPref != null && italic != null && italicPref != null) {
        add {
            TextStyleChipsPreference(
                bold = bold,
                onBoldChange = { onSavePrefs(boldPref.key, it) },
                italic = italic,
                onItalicChange = { onSavePrefs(italicPref.key, it) },
                title = { Text(stringResource(R.string.pref_text_style_title)) },
            )
        }
    } else if (bold != null && boldPref != null) {
        add {
            TogglePreferenceWithIcon(
                value = bold,
                onValueChange = { onSavePrefs(boldPref.key, it) },
                title = { Text(stringResource(R.string.pref_bold_title)) },
            )
        }
    }
}

private fun MutableList<@Composable () -> Unit>.offsetItems(
    offsetX: Float,
    offsetXPref: FloatPref,
    offsetY: Float,
    offsetYPref: FloatPref,
    onSavePrefs: (String, Any) -> Unit,
) {
    add {
        SliderPreferenceWithStepper(
            value = offsetX,
            onValueChange = { onSavePrefs(offsetXPref.key, it) },
            title = { Text(stringResource(R.string.pref_ring_offset_x_title)) },
            valueRange = offsetXPref.range!!,
            defaultValue = offsetXPref.default,
            onReset = { onSavePrefs(offsetXPref.key, offsetXPref.default) },
            stepSize = 1f,
            decimalPlaces = 0,
            suffix = "px",
        )
    }
    add {
        SliderPreferenceWithStepper(
            value = offsetY,
            onValueChange = { onSavePrefs(offsetYPref.key, it) },
            title = { Text(stringResource(R.string.pref_ring_offset_y_title)) },
            valueRange = offsetYPref.range!!,
            defaultValue = offsetYPref.default,
            onReset = { onSavePrefs(offsetYPref.key, offsetYPref.default) },
            stepSize = 1f,
            decimalPlaces = 0,
            suffix = "px",
        )
    }
}

private fun MutableList<@Composable () -> Unit>.layoutItems(
    truncateEnabled: Boolean?,
    truncateEnabledPref: BoolPref?,
    extraInt: Int?,
    extraIntPref: IntPref?,
    extraIntTitleRes: Int?,
    extraIntSuffix: String?,
    ellipsize: String?,
    ellipsizePref: StringPref?,
    ellipsizeValues: List<String>?,
    previewText: String?,
    previewTextPref: StringPref?,
    onSavePrefs: (String, Any) -> Unit,
) {
    if (truncateEnabled != null && truncateEnabledPref != null) {
        add {
            TogglePreferenceWithIcon(
                value = truncateEnabled,
                onValueChange = { onSavePrefs(truncateEnabledPref.key, it) },
                title = { Text(stringResource(R.string.pref_filename_truncate_title)) },
            )
        }
    }
    if (extraInt != null && extraIntPref != null && extraIntTitleRes != null) {
        add {
            SliderPreferenceWithStepper(
                value = extraInt.toFloat(),
                onValueChange = { onSavePrefs(extraIntPref.key, it.toInt()) },
                title = { Text(stringResource(extraIntTitleRes)) },
                valueRange =
                    extraIntPref.range!!.let {
                        it.first.toFloat()..it.last.toFloat()
                    },
                defaultValue = extraIntPref.default.toFloat(),
                onReset = { onSavePrefs(extraIntPref.key, extraIntPref.default) },
                stepSize = 1f,
                decimalPlaces = 0,
                suffix = extraIntSuffix ?: "",
                enabled = truncateEnabled ?: true,
            )
        }
    }
    if (ellipsize != null && ellipsizePref != null && ellipsizeValues != null) {
        add {
            SelectPreference(
                value = ellipsize,
                onValueChange = { onSavePrefs(ellipsizePref.key, it) },
                values = ellipsizeValues,
                title = { Text(stringResource(R.string.pref_ellipsize_title)) },
                enabled = truncateEnabled ?: true,
                valueToText = { it.replaceFirstChar { c -> c.uppercase() } },
            )
        }
    }
    if (previewText != null && previewTextPref != null) {
        add {
            TextInputPreference(
                value = previewText,
                onValueChange = { onSavePrefs(previewTextPref.key, it) },
                title = { Text(stringResource(R.string.pref_preview_text_title)) },
                defaultValue = previewTextPref.default,
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TextCalibrationScreenPreview() {
    AppTheme(darkThemeConfig = DarkThemeConfig.DARK) {
        TextCalibrationScreen(
            titleRes = R.string.pref_calibrate_filename_title,
            offsetX = 0f,
            offsetY = 0f,
            offsetXPref = Prefs.filenameTextOffsetX,
            offsetYPref = Prefs.filenameTextOffsetY,
            onSavePrefs = { _, _ -> },
            onNavigateBack = {},
            contentPadding = PaddingValues(),
            fontSize = 7f,
            fontSizePref = Prefs.filenameTextSize,
            bold = false,
            boldPref = Prefs.filenameTextBold,
            italic = false,
            italicPref = Prefs.filenameTextItalic,
            truncateEnabled = true,
            truncateEnabledPref = Prefs.filenameTruncateEnabled,
            extraInt = 20,
            extraIntPref = Prefs.filenameMaxChars,
            extraIntTitleRes = R.string.pref_filename_max_chars_title,
            ellipsize = "middle",
            ellipsizePref = Prefs.filenameEllipsize,
            ellipsizeValues = listOf("start", "middle", "end"),
            previewText = Prefs.previewFilenameText.default,
            previewTextPref = Prefs.previewFilenameText,
        )
    }
}
