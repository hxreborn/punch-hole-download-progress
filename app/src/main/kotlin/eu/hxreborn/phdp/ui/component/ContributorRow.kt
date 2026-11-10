package eu.hxreborn.phdp.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.about.Contributor
import eu.hxreborn.phdp.about.DIFFSTAT_BLOCKS
import eu.hxreborn.phdp.about.greenBlocks
import eu.hxreborn.phdp.about.redBlocks
import eu.hxreborn.phdp.ui.theme.Tokens
import eu.hxreborn.phdp.ui.theme.extendedColors
import me.zhanghai.compose.preference.Preference
import java.text.NumberFormat

private val AvatarSize = 40.dp
private val AvatarGap = 12.dp
private val TitleHeight = 24.dp
private val OwnerBadgeHeight = 20.dp
private val OwnerBadgePaddingHorizontal = 8.dp
private val OwnerBadgeText =
    TextStyle(
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
private val BlockSize = 9.dp
private val BlockGap = 2.dp
private val HatchSpacing = 3.dp
private const val MINUS = '−'

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val avatarShapes =
    listOf(
        MaterialShapes.Cookie12Sided,
        MaterialShapes.Cookie9Sided,
        MaterialShapes.Cookie7Sided,
        MaterialShapes.Cookie6Sided,
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContributorRow(
    contributor: Contributor,
    shapeIndex: Int,
    onClick: () -> Unit,
) {
    val shape = avatarShapes[shapeIndex % avatarShapes.size].toShape()
    val added = pluralStringResource(R.plurals.contributor_lines_added, contributor.added, contributor.added)
    val removed = pluralStringResource(R.plurals.contributor_lines_removed, contributor.removed, contributor.removed)
    val avatarModifier = Modifier.padding(end = AvatarGap).size(AvatarSize)
    Preference(
        onClick = onClick,
        enabled = true,
        icon = {
            val avatar = contributor.avatar
            if (avatar != null) {
                Image(
                    bitmap = avatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = avatarModifier.clip(shape),
                )
            } else {
                Box(
                    modifier =
                        avatarModifier.background(MaterialTheme.colorScheme.primaryContainer, shape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = contributor.login.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        },
        title = {
            Row(
                modifier = Modifier.heightIn(min = TitleHeight),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    contributor.login,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (contributor.owner) OwnerBadge()
            }
        },
        summary = { DiffStat(contributor = contributor, description = "$added, $removed") },
        widgetContainer = {
            Icon(
                Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                modifier = Modifier.padding(horizontal = Tokens.PreferencePadding),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun OwnerBadge() {
    Surface(
        modifier = Modifier.height(OwnerBadgeHeight),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Box(Modifier.padding(horizontal = OwnerBadgePaddingHorizontal), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.contributor_owner),
                style = OwnerBadgeText.merge(MaterialTheme.typography.labelSmall),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DiffStat(
    contributor: Contributor,
    description: String,
) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val added = remember(contributor.added, formatter) { "+${formatter.format(contributor.added)}" }
    val removed = remember(contributor.removed, formatter) { "$MINUS${formatter.format(contributor.removed)}" }
    val statStyle = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum")
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(added, style = statStyle, color = MaterialTheme.extendedColors.diffAdded)
        Text(removed, style = statStyle, color = MaterialTheme.colorScheme.error)
        DiffBlocks(
            greenBlocks = greenBlocks(contributor.added, contributor.removed),
            redBlocks = redBlocks(contributor.added, contributor.removed),
        )
    }
}

@Composable
private fun DiffBlocks(
    greenBlocks: Int,
    redBlocks: Int,
) {
    val addedColor = MaterialTheme.extendedColors.diffAdded
    val removedColor = MaterialTheme.colorScheme.error
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val width = BlockSize * DIFFSTAT_BLOCKS + BlockGap * (DIFFSTAT_BLOCKS - 1)
    Box(
        Modifier.size(width, BlockSize).drawWithCache {
            val block = BlockSize.toPx()
            val gap = BlockGap.toPx()
            val corner = CornerRadius(1.dp.toPx())
            val size = Size(block, block)
            val hatch = hatchPath(block, HatchSpacing.toPx())
            val hatchStroke = Stroke(width = 1.dp.toPx())
            onDrawBehind {
                repeat(DIFFSTAT_BLOCKS) { index ->
                    val left = index * (block + gap)
                    val topLeft = Offset(left, 0f)
                    when {
                        index < greenBlocks -> {
                            drawRoundRect(addedColor, topLeft, size, corner)
                        }

                        index < greenBlocks + redBlocks -> {
                            drawRoundRect(removedColor.copy(alpha = HATCH_FILL_ALPHA), topLeft, size, corner)
                            clipRect(left, 0f, left + block, block) {
                                translate(left, 0f) { drawPath(hatch, removedColor, style = hatchStroke) }
                            }
                        }

                        else -> {
                            drawRoundRect(emptyColor, topLeft, size, corner)
                        }
                    }
                }
            }
        },
    )
}

private const val HATCH_FILL_ALPHA = 0.25f

private fun hatchPath(
    block: Float,
    spacing: Float,
): Path =
    Path().apply {
        var offset = spacing
        while (offset < block * 2) {
            moveTo(offset, 0f)
            lineTo(offset - block, block)
            offset += spacing
        }
    }
