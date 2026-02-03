package eu.hxreborn.phdp.ui.theme

import androidx.compose.material3.ShapeDefaults
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Tokens {
    // M3 Layout
    val ScreenHorizontalPadding: Dp = 16.dp

    // M3 LargeTopAppBar
    val LargeAppBarExpandedHeight: Dp = 152.dp

    // M3 Spacing Scale
    val SpacingSm: Dp = 8.dp
    val SpacingLg: Dp = 16.dp

    // M3 shapes use ShapeDefaults where possible
    val CardShape: Shape = ShapeDefaults.Medium

    val DialogShape: Shape = ShapeDefaults.ExtraLarge

    // M3 text emphasis from M3 content design guidelines
    const val MEDIUM_EMPHASIS_ALPHA: Float = 0.60f

    const val DISABLED_ALPHA: Float = 0.38f

    // M3 Dialog
    val DialogPadding: Dp = 24.dp

    val DialogTitleSpacing: Dp = 16.dp

    val DialogActionsSpacing: Dp = 24.dp

    val DialogElevation: Dp = 6.dp

    // Preference layout from ComposePreference reference
    // https://github.com/zhanghai/ComposePreference
    val PreferencePadding: Dp = 16.dp
    val PreferenceHorizontalSpacing: Dp = 16.dp
    val PreferenceIconContainerMinWidth: Dp = 56.dp

    // App specific settings layout ColorBlendr style
    // Each row is its own surface with small gap between
    val GroupSpacing: Dp = 6.dp
    val RowGap: Dp = 2.dp
    val RowCornerRadius: Dp = 24.dp
    val SectionHorizontalMargin: Dp = 12.dp

    // App icon in list items
    val AppIconSize: Dp = 48.dp
    val AppIconCornerRadius: Dp = 12.dp
    val LoadingIndicatorSize: Dp = 24.dp

    // M3 checkbox size for skeleton
    val CheckboxSize: Dp = 24.dp

    // Loading skeleton placeholder
    val SkeletonTitleHeight: Dp = 16.dp
    val SkeletonSubtitleHeight: Dp = 12.dp
    val SmallCornerRadius: Dp = 4.dp

    // Package selection screen
    const val SEARCH_DEBOUNCE_MS: Long = 200
    const val SHIMMER_PLACEHOLDER_COUNT: Int = 6
    const val EMPTY_STATE_FADE_MS: Int = 500
    val EmptyStatePadding: Dp = 48.dp

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

    // Overflow menu
    val MenuCornerRadius: Dp = 28.dp
    const val MENU_SWITCH_SCALE: Float = 0.8f

    // Expressive checkbox
    val CheckboxCornerRadius: Dp = 8.dp
    val CheckboxIconSize: Dp = 18.dp
}
