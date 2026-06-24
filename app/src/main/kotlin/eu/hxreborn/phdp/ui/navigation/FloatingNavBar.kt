package eu.hxreborn.phdp.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.dropUnlessResumed
import eu.hxreborn.phdp.ui.theme.Tokens

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FloatingNavBar(
    items: List<BottomNavItem>,
    selectedKey: Screen?,
    onSelect: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonBounds = remember { mutableStateMapOf<Int, Rect>() }
    val currentIndex = items.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)
    val targetRect = buttonBounds[currentIndex]
    val anchorRect = buttonBounds[0]
    val pillTargetX = (targetRect?.left ?: 0f) - (anchorRect?.left ?: 0f)
    val pillTargetWidth = targetRect?.width ?: 0f

    val pillAnimatedX by animateFloatAsState(
        targetValue = pillTargetX,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "navPillX",
    )
    val pillAnimatedWidth by animateFloatAsState(
        targetValue = pillTargetWidth,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "navPillWidth",
    )
    val pillColor = MaterialTheme.colorScheme.primary

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = Tokens.FloatingBarBottomPadding),
        contentAlignment = Alignment.BottomCenter,
    ) {
        HorizontalFloatingToolbar(
            expanded = true,
            colors =
                FloatingToolbarDefaults.vibrantFloatingToolbarColors(
                    toolbarContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    toolbarContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
        ) {
            CompositionLocalProvider(LocalRippleConfiguration provides null) {
                items.forEachIndexed { index, item ->
                    val selected = selectedKey == item.key
                    val onItemSelect = dropUnlessResumed { onSelect(item) }

                    ToggleButton(
                        checked = selected,
                        onCheckedChange = { if (!selected) onItemSelect() },
                        modifier =
                            Modifier
                                .height(Tokens.FloatingBarItemHeight)
                                .onGloballyPositioned { coords ->
                                    buttonBounds[index] = coords.boundsInParent()
                                }.then(
                                    if (index == 0) {
                                        Modifier.drawWithContent {
                                            if (pillAnimatedWidth > 0f) {
                                                drawRoundRect(
                                                    color = pillColor,
                                                    topLeft = Offset(pillAnimatedX, 0f),
                                                    size = Size(pillAnimatedWidth, size.height),
                                                    cornerRadius = CornerRadius(size.height / 2f),
                                                )
                                            }
                                            drawContent()
                                        }
                                    } else {
                                        Modifier
                                    },
                                ),
                        colors =
                            ToggleButtonDefaults.toggleButtonColors(
                                containerColor = Color.Transparent,
                                checkedContainerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        shapes =
                            ToggleButtonDefaults.shapes(
                                shape = CircleShape,
                                pressedShape = CircleShape,
                                checkedShape = CircleShape,
                            ),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = stringResource(item.titleRes),
                            )
                            AnimatedVisibility(
                                visible = selected,
                                enter = expandHorizontally(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
                                exit = shrinkHorizontally(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
                            ) {
                                Text(
                                    text = stringResource(item.titleRes),
                                    modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                                    style =
                                        MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
