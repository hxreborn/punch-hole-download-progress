package eu.hxreborn.phpm.ui.theme

import androidx.compose.material3.ShapeDefaults
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * App design tokens following M3 guidelines as the source of truth.
 *
 * Uses M3 shape, spacing, and emphasis values directly. Only app-specific
 * values (color picker, haptics) are defined here.
 *
 * References:
 * - https://m3.material.io/foundations/layout/understanding-layout
 * - https://m3.material.io/styles/shape/overview
 * - https://m3.material.io/foundations/content-design/text-emphasis
 */
object Tokens {
    // M3 Layout
    // M3 compact window horizontal margin = 16dp
    val ScreenHorizontalPadding: Dp = 16.dp

    // M3 Spacing Scale
    // M3 spacing units: 4, 8, 12, 16, 24, 32, 48, 64
    val SpacingXs: Dp = 4.dp
    val SpacingSm: Dp = 8.dp
    val SpacingMd: Dp = 12.dp
    val SpacingLg: Dp = 16.dp
    val SpacingXl: Dp = 24.dp

    // M3 Shapes (using ShapeDefaults directly where possible)
    // ShapeDefaults.Medium = 12.dp corner radius
    val CardShape: Shape = ShapeDefaults.Medium

    // ShapeDefaults.Large = 16.dp corner radius - used for prominent cards
    val CardShapeLarge: Shape = ShapeDefaults.Large

    // ShapeDefaults.ExtraLarge = 28.dp corner radius - M3 dialog standard
    val DialogShape: Shape = ShapeDefaults.ExtraLarge

    // M3 Text Emphasis (from M3 content design guidelines)
    // High emphasis: 87% opacity on dark, 100% on light (handled by colorScheme)
    // Medium emphasis: 60% opacity - used for secondary text
    const val MEDIUM_EMPHASIS_ALPHA: Float = 0.60f

    // Disabled: 38% opacity - M3 standard
    const val DISABLED_ALPHA: Float = 0.38f

    // M3 Dialog
    // M3 standard dialog padding = 24dp
    val DialogPadding: Dp = 24.dp

    // M3 dialog title-to-content spacing = 16dp
    val DialogTitleSpacing: Dp = 16.dp

    // M3 dialog content-to-actions spacing = 24dp
    val DialogActionsSpacing: Dp = 24.dp

    // M3 dialog tonal elevation level 3 = 6dp
    val DialogElevation: Dp = 6.dp

    // M3 List Item
    // M3 list item horizontal padding = 16dp
    val ListItemHorizontalPadding: Dp = 16.dp

    // M3 list item vertical padding = 12dp (two-line)
    val ListItemVerticalPadding: Dp = 12.dp

    // M3 list item leading-to-headline spacing = 16dp
    val ListItemLeadingSpacing: Dp = 16.dp

    // App-Specific: Settings Layout (ColorBlendr style)
    // Each row is its own surface with small gap between
    val GroupSpacing: Dp = 6.dp // vertical margin around groups
    val RowGap: Dp = 2.dp // gap between individual row surfaces
    val RowCornerRadius: Dp = 24.dp // corner radius for each row

    // Settings row padding (matches ColorBlendr: 22dp h, 16dp v)
    val SettingsRowHorizontalPadding: Dp = 22.dp
    val SettingsRowVerticalPadding: Dp = 16.dp

    // App-Specific: Section Header
    // Indented from screen edge to align with group content
    val SectionHeaderStartPadding: Dp = 32.dp

    // App-Specific: Color Picker
    // Custom sizes for color selection UI
    val ColorPreviewSize: Dp = 40.dp
    val ColorPreviewSizeLarge: Dp = 56.dp
    val ColorSwatchSize: Dp = 48.dp
    val ColorGridSpacing: Dp = 12.dp
    const val COLOR_GRID_COLUMNS: Int = 4
    val ColorCheckIconSize: Dp = 24.dp
    val ColorBorderWidth: Dp = 2.dp
    val ColorBorderWidthSelected: Dp = 3.dp

    // App-Specific: Animation
    // Base duration (scaled by system animator duration scale at runtime)
    const val ANIMATION_DURATION_MS: Int = 200

    // App-Specific: Haptic Feedback
    // Minimum interval between haptic feedback events to prevent buzz
    const val HAPTIC_THROTTLE_MS: Long = 50L
}
