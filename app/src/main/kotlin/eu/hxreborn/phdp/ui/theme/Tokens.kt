package eu.hxreborn.phdp.ui.theme

import androidx.compose.material3.ShapeDefaults
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Tokens {
    // M3 Layout
    val ScreenHorizontalPadding: Dp = 16.dp

    // M3 Spacing Scale
    val SpacingXs: Dp = 4.dp
    val SpacingSm: Dp = 8.dp
    val SpacingMd: Dp = 12.dp
    val SpacingLg: Dp = 16.dp
    val SpacingXl: Dp = 24.dp

    // M3 shapes use ShapeDefaults where possible
    val CardShape: Shape = ShapeDefaults.Medium

    val CardShapeLarge: Shape = ShapeDefaults.Large

    val DialogShape: Shape = ShapeDefaults.ExtraLarge

    // M3 text emphasis from M3 content design guidelines
    const val MEDIUM_EMPHASIS_ALPHA: Float = 0.60f

    const val DISABLED_ALPHA: Float = 0.38f

    // M3 Dialog
    val DialogPadding: Dp = 24.dp

    val DialogTitleSpacing: Dp = 16.dp

    val DialogActionsSpacing: Dp = 24.dp

    val DialogElevation: Dp = 6.dp

    // M3 List Item
    val ListItemHorizontalPadding: Dp = 16.dp

    val ListItemVerticalPadding: Dp = 12.dp

    val ListItemLeadingSpacing: Dp = 16.dp

    // Preference layout from ComposePreference reference
    // https://github.com/zhanghai/ComposePreference
    val PreferencePadding: Dp = 16.dp
    val PreferenceHorizontalSpacing: Dp = 16.dp
    val PreferenceVerticalSpacing: Dp = 16.dp
    val PreferenceIconContainerMinWidth: Dp = 56.dp
    val PreferenceCategoryPaddingTop: Dp = 24.dp
    val PreferenceCategoryPaddingBottom: Dp = 8.dp

    // App specific settings layout ColorBlendr style
    // Each row is its own surface with small gap between
    val GroupSpacing: Dp = 6.dp
    val RowGap: Dp = 2.dp
    val RowCornerRadius: Dp = 24.dp

    // App specific section header
    // Indented from screen edge to align with group content
    val SectionHeaderStartPadding: Dp = 32.dp

    // App specific color picker
    // Custom sizes for color selection UI
    val ColorPreviewSize: Dp = 40.dp
    val ColorPreviewSizeLarge: Dp = 56.dp
    val ColorSwatchSize: Dp = 48.dp
    val ColorGridSpacing: Dp = 12.dp
    const val COLOR_GRID_COLUMNS: Int = 4
    val ColorCheckIconSize: Dp = 24.dp
    val ColorBorderWidth: Dp = 2.dp
    val ColorBorderWidthSelected: Dp = 3.dp

    // App specific animation
    // Base duration scaled by system animator duration scale at runtime
    const val ANIMATION_DURATION_MS: Int = 200

    // App specific haptic feedback
    // Minimum interval between haptic feedback events to prevent buzz
    const val HAPTIC_THROTTLE_MS: Long = 50L
}
