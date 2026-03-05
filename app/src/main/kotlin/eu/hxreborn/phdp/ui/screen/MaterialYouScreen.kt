package eu.hxreborn.phdp.ui.screen

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.IntPref
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.prefs.StringPref
import eu.hxreborn.phdp.ui.SettingsUiState
import eu.hxreborn.phdp.ui.SettingsViewModel
import eu.hxreborn.phdp.ui.component.SettingsScaffold
import eu.hxreborn.phdp.ui.state.PrefsState
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig
import eu.hxreborn.phdp.ui.theme.Tokens

private data class MaterialYouPreset(
    val labelRes: Int,
    val progressPalette: String,
    val progressShade: Int,
    val successPalette: String,
    val successShade: Int,
    val errorPalette: String,
    val errorShade: Int,
) {
    fun matches(state: PrefsState): Boolean =
        progressPalette == state.materialYouProgressPalette && progressShade == state.materialYouProgressShade &&
            successPalette == state.materialYouSuccessPalette &&
            successShade == state.materialYouSuccessShade &&
            errorPalette == state.materialYouErrorPalette &&
            errorShade == state.materialYouErrorShade
}

private val presets =
    listOf(
        MaterialYouPreset(
            labelRes = R.string.material_you_preset_default,
            progressPalette = "accent1",
            progressShade = 500,
            successPalette = "accent2",
            successShade = 500,
            errorPalette = "accent3",
            errorShade = 500,
        ),
        MaterialYouPreset(
            labelRes = R.string.material_you_preset_primary,
            progressPalette = "accent1",
            progressShade = 500,
            successPalette = "accent1",
            successShade = 300,
            errorPalette = "accent3",
            errorShade = 500,
        ),
        MaterialYouPreset(
            labelRes = R.string.material_you_preset_muted,
            progressPalette = "accent2",
            progressShade = 500,
            successPalette = "accent2",
            successShade = 300,
            errorPalette = "neutral1",
            errorShade = 500,
        ),
        MaterialYouPreset(
            labelRes = R.string.material_you_preset_mono,
            progressPalette = "neutral1",
            progressShade = 400,
            successPalette = "neutral1",
            successShade = 200,
            errorPalette = "neutral1",
            errorShade = 700,
        ),
    )

private data class ColorSection(
    val labelRes: Int,
    val paletteKey: StringPref,
    val shadeKey: IntPref,
    val paletteOf: (PrefsState) -> String,
    val shadeOf: (PrefsState) -> Int,
)

private val colorSections =
    listOf(
        ColorSection(
            labelRes = R.string.group_material_you_progress,
            paletteKey = Prefs.materialYouProgressPalette,
            shadeKey = Prefs.materialYouProgressShade,
            paletteOf = { it.materialYouProgressPalette },
            shadeOf = { it.materialYouProgressShade },
        ),
        ColorSection(
            labelRes = R.string.group_material_you_success,
            paletteKey = Prefs.materialYouSuccessPalette,
            shadeKey = Prefs.materialYouSuccessShade,
            paletteOf = { it.materialYouSuccessPalette },
            shadeOf = { it.materialYouSuccessShade },
        ),
        ColorSection(
            labelRes = R.string.group_material_you_error,
            paletteKey = Prefs.materialYouErrorPalette,
            shadeKey = Prefs.materialYouErrorShade,
            paletteOf = { it.materialYouErrorPalette },
            shadeOf = { it.materialYouErrorShade },
        ),
    )

private fun SettingsViewModel.applyPreset(preset: MaterialYouPreset) {
    with(preset) {
        savePref(Prefs.materialYouProgressPalette, progressPalette)
        savePref(Prefs.materialYouProgressShade, progressShade)
        savePref(Prefs.materialYouSuccessPalette, successPalette)
        savePref(Prefs.materialYouSuccessShade, successShade)
        savePref(Prefs.materialYouErrorPalette, errorPalette)
        savePref(Prefs.materialYouErrorShade, errorShade)
    }
}

private val palettes =
    listOf(
        "accent1" to R.string.material_you_row_primary,
        "accent2" to R.string.material_you_row_secondary,
        "accent3" to R.string.material_you_row_tertiary,
        "neutral1" to R.string.material_you_row_neutral,
        "neutral2" to R.string.material_you_row_neutral_variant,
    )

private val gridShades = listOf(100, 200, 300, 400, 500, 600, 700, 800, 900)

private const val SWATCH_SIZE_DP = 36
private const val SWATCH_SPACING_DP = 4
private const val LABEL_WIDTH_DP = 72
private const val RING_SIZE_DP = 120
private const val RING_STROKE_DP = 8
private const val LEGEND_DOT_SIZE_DP = 12

@Composable
private fun resolveSystemColor(
    palette: String,
    shade: Int,
): Color {
    val context = LocalContext.current
    return remember(palette, shade) {
        context.resources
            .getIdentifier("system_${palette}_$shade", "color", "android")
            .takeIf { it != 0 }
            ?.let { Color(context.getColor(it)) } ?: Color.Gray
    }
}

@Composable
fun MaterialYouScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomNavPadding: Dp = 0.dp,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefsState = (uiState as? SettingsUiState.Success)?.prefs ?: return

    val activePreset = presets.indexOfFirst { it.matches(prefsState) }

    var selectedSection by rememberSaveable { mutableIntStateOf(0) }

    val progressColor =
        resolveSystemColor(
            prefsState.materialYouProgressPalette,
            prefsState.materialYouProgressShade,
        )
    val completionColor =
        resolveSystemColor(
            prefsState.materialYouSuccessPalette,
            prefsState.materialYouSuccessShade,
        )
    val errorColor =
        resolveSystemColor(
            prefsState.materialYouErrorPalette,
            prefsState.materialYouErrorShade,
        )

    SettingsScaffold(
        title = stringResource(R.string.pref_material_you_title),
        onNavigateBack = onNavigateBack,
        bottomPadding = bottomNavPadding,
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = innerPadding.calculateTopPadding() + Tokens.SpacingLg,
                    bottom = innerPadding.calculateBottomPadding() + Tokens.SpacingLg,
                ),
        ) {
            item(key = "preview_ring") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Tokens.SpacingLg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IndicatorPreviewRing(
                        progressColor = progressColor,
                        completionColor = completionColor,
                        errorColor = errorColor,
                    )
                    Spacer(modifier = Modifier.height(Tokens.SpacingSm))
                    PreviewLegend(
                        progressColor = progressColor,
                        completionColor = completionColor,
                        errorColor = errorColor,
                    )
                }
            }

            item(key = "presets") {
                Row(
                    modifier =
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(
                            horizontal = Tokens.SpacingLg,
                            vertical = Tokens.SpacingSm,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.SpacingSm),
                ) {
                    presets.forEachIndexed { index, preset ->
                        FilterChip(
                            selected = activePreset == index,
                            onClick = { viewModel.applyPreset(preset) },
                            label = { Text(stringResource(preset.labelRes)) },
                        )
                    }
                    FilterChip(
                        selected = activePreset == -1,
                        onClick = {},
                        label = {
                            Text(stringResource(R.string.material_you_preset_custom))
                        },
                    )
                }
            }

            item(key = "section_selector") {
                SingleChoiceSegmentedButtonRow(
                    modifier =
                        Modifier.fillMaxWidth().padding(
                            horizontal = Tokens.SectionHorizontalMargin,
                            vertical = Tokens.SpacingSm,
                        ),
                ) {
                    colorSections.forEachIndexed { index, section ->
                        SegmentedButton(
                            selected = selectedSection == index,
                            onClick = { selectedSection = index },
                            shape =
                                SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = colorSections.size,
                                ),
                        ) {
                            Text(stringResource(section.labelRes))
                        }
                    }
                }
            }

            item(key = "color_grid") {
                val activeSection = colorSections[selectedSection]
                ColorGrid(
                    selectedPalette = activeSection.paletteOf(prefsState),
                    selectedShade = activeSection.shadeOf(prefsState),
                    onColorSelected = { palette, shade ->
                        viewModel.savePref(activeSection.paletteKey, palette)
                        viewModel.savePref(activeSection.shadeKey, shade)
                    },
                )
            }

            item(key = "test_buttons") {
                Surface(
                    shape = Tokens.CardShape,
                    tonalElevation = 1.dp,
                    modifier =
                        Modifier.padding(
                            horizontal = Tokens.SectionHorizontalMargin,
                            vertical = Tokens.SpacingLg,
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        TextButton(onClick = { viewModel.simulateSuccess() }) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(Tokens.SpacingSm))
                            Text(stringResource(R.string.group_material_you_success))
                        }
                        TextButton(onClick = { viewModel.simulateFailure() }) {
                            Icon(
                                imageVector = Icons.Outlined.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(Tokens.SpacingSm))
                            Text(stringResource(R.string.group_material_you_error))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IndicatorPreviewRing(
    progressColor: Color,
    completionColor: Color,
    errorColor: Color,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateColorAsState(progressColor, label = "progress")
    val animatedCompletion by animateColorAsState(completionColor, label = "completion")
    val animatedError by animateColorAsState(errorColor, label = "error")
    val bgColor = MaterialTheme.colorScheme.surfaceContainerHighest

    val arcs =
        listOf(
            Triple(bgColor, 0f, 360f),
            Triple(animatedProgress, -90f, 252f),
            Triple(animatedCompletion, 162f, 72f),
            Triple(animatedError, 234f, 36f),
        )

    Canvas(modifier = modifier.size(RING_SIZE_DP.dp)) {
        val strokeWidth = RING_STROKE_DP.dp.toPx()
        val arcStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val inset = strokeWidth / 2f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(inset, inset)

        arcs.forEach { (color, startAngle, sweepAngle) ->
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = arcStroke,
            )
        }
    }
}

@Composable
private fun PreviewLegend(
    progressColor: Color,
    completionColor: Color,
    errorColor: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Tokens.SpacingLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(
            color = progressColor,
            label = stringResource(R.string.group_material_you_progress),
        )
        LegendItem(
            color = completionColor,
            label = stringResource(R.string.group_material_you_success),
        )
        LegendItem(
            color = errorColor,
            label = stringResource(R.string.group_material_you_error),
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
) {
    val animatedColor by animateColorAsState(color, label = "legend")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SWATCH_SPACING_DP.dp),
    ) {
        Box(
            modifier = Modifier.size(LEGEND_DOT_SIZE_DP.dp).background(animatedColor, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun ColorGrid(
    selectedPalette: String,
    selectedShade: Int,
    onColorSelected: (palette: String, shade: Int) -> Unit,
) {
    val scrollState = rememberScrollState()

    Surface(
        shape = Tokens.CardShape,
        tonalElevation = 1.dp,
        modifier = Modifier.padding(horizontal = Tokens.SectionHorizontalMargin),
    ) {
        Column(
            modifier = Modifier.padding(Tokens.SpacingLg),
            verticalArrangement = Arrangement.spacedBy(Tokens.GroupSpacing),
        ) {
            palettes.forEach { (palette, labelRes) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(LABEL_WIDTH_DP.dp),
                    )
                    SwatchRow(
                        scrollState = scrollState,
                        palette = palette,
                        selectedPalette = selectedPalette,
                        selectedShade = selectedShade,
                        onColorSelected = onColorSelected,
                    )
                }
            }

            Row {
                Spacer(modifier = Modifier.width(LABEL_WIDTH_DP.dp))
                Row(
                    modifier = Modifier.horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(SWATCH_SPACING_DP.dp),
                ) {
                    gridShades.forEach { shade ->
                        Text(
                            text = shade.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = Tokens.MEDIUM_EMPHASIS_ALPHA,
                                ),
                            modifier = Modifier.width(SWATCH_SIZE_DP.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwatchRow(
    scrollState: ScrollState,
    palette: String,
    selectedPalette: String,
    selectedShade: Int,
    onColorSelected: (palette: String, shade: Int) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(SWATCH_SPACING_DP.dp),
    ) {
        gridShades.forEach { shade ->
            val color = resolveSystemColor(palette, shade)
            val isSelected = palette == selectedPalette && shade == selectedShade
            ColorSwatch(
                color = color,
                selected = isSelected,
                onClick = { onColorSelected(palette, shade) },
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue =
            if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        label = "swatchBorder",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) Tokens.ColorBorderWidthSelected else 1.dp,
        label = "swatchBorderWidth",
    )

    Box(
        modifier =
            Modifier
                .size(SWATCH_SIZE_DP.dp)
                .clip(CircleShape)
                .background(color, CircleShape)
                .border(borderWidth, borderColor, CircleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Suppress("ViewModelConstructorInComposable")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MaterialYouScreenPreview() {
    AppTheme(darkThemeConfig = DarkThemeConfig.DARK) {
        MaterialYouScreen(
            viewModel = PreviewViewModel(),
            onNavigateBack = {},
        )
    }
}
