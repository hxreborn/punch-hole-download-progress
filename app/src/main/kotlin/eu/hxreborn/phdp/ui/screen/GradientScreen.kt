package eu.hxreborn.phdp.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.GRADIENT_MAX_COLORS
import eu.hxreborn.phdp.prefs.GRADIENT_MIN_COLORS
import eu.hxreborn.phdp.prefs.GradientDirection
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.prefs.encodeGradientColors
import eu.hxreborn.phdp.ui.SettingsUiState
import eu.hxreborn.phdp.ui.SettingsViewModel
import eu.hxreborn.phdp.ui.component.SectionCard
import eu.hxreborn.phdp.ui.component.SettingsScaffold
import eu.hxreborn.phdp.ui.component.TestButtonsRow
import eu.hxreborn.phdp.ui.component.gradientBrush
import eu.hxreborn.phdp.ui.component.preference.ColorPickerDialog
import eu.hxreborn.phdp.ui.component.preference.SliderPreferenceWithReset
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig
import eu.hxreborn.phdp.ui.theme.MaterialPalette
import eu.hxreborn.phdp.ui.theme.Tokens
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomNavPadding: Dp = 0.dp,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefsState = (uiState as? SettingsUiState.Success)?.prefs ?: return

    SettingsScaffold(
        title = stringResource(R.string.pref_gradient_title),
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
                item(key = "gradient_preview") {
                    GradientPreviewRing(
                        colors = prefsState.gradientColors.map { Color(it) },
                        direction = prefsState.gradientDirection,
                        angleDegrees = prefsState.gradientAngle,
                        pathMode = prefsState.pathMode,
                        opacity = prefsState.opacity / 100f,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = Tokens.SpacingLg)
                                .height(PREVIEW_HEIGHT_DP.dp),
                    )
                }

                item(key = "gradient_colors") {
                    SectionCard(
                        items =
                            listOf(
                                {
                                    GradientColorsRow(
                                        colors = prefsState.gradientColors,
                                        onColorsChange = {
                                            viewModel.savePref(
                                                Prefs.gradientColors,
                                                encodeGradientColors(it),
                                            )
                                        },
                                    )
                                },
                            ),
                    )
                }

                preferenceCategory(
                    key = "gradient_type_header",
                    title = { Text(stringResource(R.string.gradient_type_title)) },
                )

                item(key = "gradient_direction") {
                    SingleChoiceSegmentedButtonRow(
                        modifier =
                            Modifier.fillMaxWidth().padding(
                                horizontal = Tokens.SectionHorizontalMargin,
                                vertical = Tokens.SpacingSm,
                            ),
                    ) {
                        val selected =
                            GradientDirection.fromStoredValue(prefsState.gradientDirection)
                        GradientDirection.entries.forEachIndexed { index, direction ->
                            SegmentedButton(
                                selected = selected == direction,
                                onClick = {
                                    viewModel.savePref(
                                        Prefs.gradientDirection,
                                        direction.storedValue,
                                    )
                                },
                                shape =
                                    SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = GradientDirection.entries.size,
                                    ),
                            ) {
                                Text(
                                    stringResource(
                                        when (direction) {
                                            GradientDirection.SWEEP -> {
                                                R.string.gradient_type_sweep
                                            }

                                            GradientDirection.LINEAR -> {
                                                R.string.gradient_type_linear
                                            }
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }

                item(key = "gradient_angle") {
                    SectionCard(
                        items =
                            listOf(
                                {
                                    val angleRange = Prefs.gradientAngle.range!!
                                    val isSweep =
                                        GradientDirection.fromStoredValue(
                                            prefsState.gradientDirection,
                                        ) == GradientDirection.SWEEP
                                    SliderPreferenceWithReset(
                                        value = prefsState.gradientAngle.toFloat(),
                                        onValueChange = {
                                            viewModel.savePref(
                                                Prefs.gradientAngle,
                                                it.toInt(),
                                            )
                                        },
                                        title = {
                                            Text(
                                                stringResource(
                                                    if (isSweep) {
                                                        R.string.gradient_start_position
                                                    } else {
                                                        R.string.gradient_angle_title
                                                    },
                                                ),
                                            )
                                        },
                                        valueRange =
                                            angleRange.first.toFloat()..angleRange.last.toFloat(),
                                        defaultValue = Prefs.gradientAngle.default.toFloat(),
                                        onReset = {
                                            viewModel.savePref(
                                                Prefs.gradientAngle,
                                                Prefs.gradientAngle.default,
                                            )
                                        },
                                        valueText = { Text("${it.toInt()}°") },
                                    )
                                },
                            ),
                    )
                }

                item(key = "test_buttons") {
                    TestButtonsRow(
                        onSimulateSuccess = { viewModel.simulateSuccess() },
                        onSimulateFailure = { viewModel.simulateFailure() },
                    )
                }
            }
        }
    }
}

@Composable
private fun GradientColorsRow(
    colors: List<Int>,
    onColorsChange: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    editingIndex?.let { index ->
        if (index in colors.indices) {
            ColorPickerDialog(
                initialColor = colors[index],
                colors = MaterialPalette.materialColors,
                onDismiss = { editingIndex = null },
                onColorSelected = { color ->
                    onColorsChange(colors.toMutableList().also { it[index] = color })
                    editingIndex = null
                },
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth().padding(Tokens.PreferencePadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.gradient_colors_title),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onColorsChange(colors.reversed()) }) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = stringResource(R.string.gradient_swap_colors),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            colors.forEachIndexed { index, color ->
                ColorDot(
                    color = color,
                    description =
                        stringResource(R.string.gradient_color_position, index + 1),
                    onClick = { editingIndex = index },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onColorsChange(colors.dropLast(1)) },
                enabled = colors.size > GRADIENT_MIN_COLORS,
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.gradient_remove_color),
                )
            }
            IconButton(
                onClick = { onColorsChange(colors + colors.last()) },
                enabled = colors.size < GRADIENT_MAX_COLORS,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.gradient_add_color),
                )
            }
        }
    }
}

@Composable
private fun ColorDot(
    color: Int,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .clickable(onClick = onClick, role = Role.Button)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(Tokens.ColorPreviewSize)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(
                        width = Tokens.ColorBorderWidth,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    ),
        )
    }
}

@Composable
private fun GradientPreviewRing(
    colors: List<Color>,
    direction: String,
    angleDegrees: Int,
    pathMode: Boolean,
    opacity: Float,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier = modifier) {
        val strokeWidth = PREVIEW_STROKE_DP.dp.toPx()
        val previewWidth =
            if (pathMode) {
                minOf(size.width - strokeWidth, PREVIEW_PATH_WIDTH_DP.dp.toPx())
            } else {
                minOf(minOf(size.width, size.height) - strokeWidth, PREVIEW_RING_SIZE_DP.dp.toPx())
            }
        val previewHeight = minOf(size.height - strokeWidth, PREVIEW_RING_SIZE_DP.dp.toPx())
        val left = (size.width - previewWidth) / 2f
        val top = (size.height - previewHeight) / 2f
        val bounds = Rect(left, top, left + previewWidth, top + previewHeight)
        val brush = gradientBrush(colors, direction, angleDegrees, bounds)
        val style = Stroke(width = strokeWidth, cap = StrokeCap.Round)

        if (pathMode) {
            val cornerRadius = CornerRadius(previewHeight / 2f)
            drawRoundRect(
                color = trackColor,
                topLeft = bounds.topLeft,
                size = bounds.size,
                cornerRadius = cornerRadius,
                style = style,
            )
            drawRoundRect(
                brush = brush,
                topLeft = bounds.topLeft,
                size = bounds.size,
                cornerRadius = cornerRadius,
                alpha = opacity,
                style = style,
            )
        } else {
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = bounds.topLeft,
                size = bounds.size,
                style = style,
            )
            drawArc(
                brush = brush,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = bounds.topLeft,
                size = bounds.size,
                alpha = opacity,
                style = style,
            )
        }
    }
}

@Suppress("ViewModelConstructorInComposable")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GradientScreenPreview() {
    AppTheme(darkThemeConfig = DarkThemeConfig.DARK) {
        GradientScreen(
            viewModel = PreviewViewModel(),
            onNavigateBack = {},
        )
    }
}

private const val PREVIEW_HEIGHT_DP = 144
private const val PREVIEW_RING_SIZE_DP = 112
private const val PREVIEW_PATH_WIDTH_DP = 176
private const val PREVIEW_STROKE_DP = 8
