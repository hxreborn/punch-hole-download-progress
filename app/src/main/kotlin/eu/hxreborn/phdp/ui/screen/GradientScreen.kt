package eu.hxreborn.phdp.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.East
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.South
import androidx.compose.material.icons.outlined.SouthEast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.GradientDirection
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.ui.SettingsUiState
import eu.hxreborn.phdp.ui.SettingsViewModel
import eu.hxreborn.phdp.ui.component.SectionCard
import eu.hxreborn.phdp.ui.component.SettingsScaffold
import eu.hxreborn.phdp.ui.component.gradientBrush
import eu.hxreborn.phdp.ui.component.preference.ColorPreference
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig
import eu.hxreborn.phdp.ui.theme.Tokens
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceCategory

private data class DirectionOption(
    val direction: GradientDirection,
    val icon: ImageVector,
    val labelRes: Int,
)

private val directionOptions =
    listOf(
        DirectionOption(
            GradientDirection.SWEEP,
            Icons.Outlined.DonutLarge,
            R.string.gradient_direction_sweep,
        ),
        DirectionOption(
            GradientDirection.LEFT_TO_RIGHT,
            Icons.Outlined.East,
            R.string.gradient_direction_left_to_right,
        ),
        DirectionOption(
            GradientDirection.TOP_TO_BOTTOM,
            Icons.Outlined.South,
            R.string.gradient_direction_top_to_bottom,
        ),
        DirectionOption(
            GradientDirection.TOP_LEFT_TO_BOTTOM_RIGHT,
            Icons.Outlined.SouthEast,
            R.string.gradient_direction_top_left_to_bottom_right,
        ),
        DirectionOption(
            GradientDirection.BOTTOM_LEFT_TO_TOP_RIGHT,
            Icons.Outlined.NorthEast,
            R.string.gradient_direction_bottom_left_to_top_right,
        ),
    )

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
        actions = {
            SwapColorsAction {
                viewModel.savePref(Prefs.gradientStartColor, prefsState.gradientEndColor)
                viewModel.savePref(Prefs.gradientEndColor, prefsState.gradientStartColor)
            }
        },
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
                        startColor = Color(prefsState.gradientStartColor),
                        endColor = Color(prefsState.gradientEndColor),
                        direction = prefsState.gradientDirection,
                        pathMode = prefsState.pathMode,
                        opacity = prefsState.opacity / 100f,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = Tokens.SpacingLg)
                                .height(PREVIEW_HEIGHT_DP.dp),
                    )
                }

                preferenceCategory(
                    key = "gradient_colors_header",
                    title = { Text(stringResource(R.string.group_colors)) },
                )

                item(key = "gradient_colors") {
                    SectionCard(
                        items =
                            listOf(
                                {
                                    ColorPreference(
                                        value = prefsState.gradientStartColor,
                                        onValueChange = {
                                            viewModel.savePref(Prefs.gradientStartColor, it)
                                        },
                                        title = {
                                            Text(stringResource(R.string.gradient_start_color_title))
                                        },
                                        summary = {
                                            Text(stringResource(R.string.gradient_start_color_summary))
                                        },
                                    )
                                },
                                {
                                    ColorPreference(
                                        value = prefsState.gradientEndColor,
                                        onValueChange = {
                                            viewModel.savePref(Prefs.gradientEndColor, it)
                                        },
                                        title = {
                                            Text(stringResource(R.string.gradient_end_color_title))
                                        },
                                        summary = {
                                            Text(stringResource(R.string.gradient_end_color_summary))
                                        },
                                    )
                                },
                            ),
                    )
                }

                preferenceCategory(
                    key = "gradient_direction_header",
                    title = { Text(stringResource(R.string.gradient_direction_title)) },
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
                        directionOptions.forEachIndexed { index, option ->
                            val label = stringResource(option.labelRes)
                            SegmentedButton(
                                selected = selected == option.direction,
                                onClick = {
                                    viewModel.savePref(
                                        Prefs.gradientDirection,
                                        option.direction.storedValue,
                                    )
                                },
                                shape =
                                    SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = directionOptions.size,
                                    ),
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = label,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwapColorsAction(onClick: () -> Unit) {
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip {
                Text(stringResource(R.string.gradient_swap_colors))
            }
        },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = stringResource(R.string.gradient_swap_colors),
            )
        }
    }
}

@Composable
private fun GradientPreviewRing(
    startColor: Color,
    endColor: Color,
    direction: String,
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
                minOf(size.width, size.height) - strokeWidth
            }
        val previewHeight = minOf(size.height - strokeWidth, PREVIEW_RING_SIZE_DP.dp.toPx())
        val left = (size.width - previewWidth) / 2f
        val top = (size.height - previewHeight) / 2f
        val bounds = Rect(left, top, left + previewWidth, top + previewHeight)
        val brush = gradientBrush(startColor, endColor, direction, bounds)
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
