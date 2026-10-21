package eu.hxreborn.phdp.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.hxreborn.phdp.BuildConfig
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.about.Note
import eu.hxreborn.phdp.about.Release
import eu.hxreborn.phdp.about.loadReleases
import eu.hxreborn.phdp.ui.component.SettingsScaffold
import eu.hxreborn.phdp.ui.theme.Tokens
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ChangelogScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomNavPadding: Dp = 0.dp,
) {
    val context = LocalContext.current
    val releases = remember(context) { loadReleases(context.assets) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val canScrollUp by remember { derivedStateOf { listState.canScrollBackward } }
    val canScrollDown by remember { derivedStateOf { listState.canScrollForward } }

    SettingsScaffold(
        title = stringResource(R.string.changelog),
        onNavigateBack = onNavigateBack,
        bottomPadding = bottomNavPadding,
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = Tokens.SectionHorizontalMargin,
                    end = Tokens.SectionHorizontalMargin,
                    top = innerPadding.calculateTopPadding() + Tokens.SpacingLg,
                    bottom = innerPadding.calculateBottomPadding() + JumpControlsClearance,
                ),
            verticalArrangement = Arrangement.spacedBy(Tokens.SpacingSm),
        ) {
            items(releases, key = { it.versionCode }) { release ->
                ReleaseCard(release)
            }
        }
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = Tokens.SpacingLg, bottom = innerPadding.calculateBottomPadding() + Tokens.SpacingLg),
            verticalArrangement = Arrangement.spacedBy(Tokens.SpacingSm),
        ) {
            JumpButton(
                visible = canScrollUp,
                icon = Icons.Rounded.KeyboardArrowUp,
                label = stringResource(R.string.changelog_jump_top),
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
            )
            JumpButton(
                visible = canScrollDown,
                icon = Icons.Rounded.KeyboardArrowDown,
                label = stringResource(R.string.changelog_jump_bottom),
                onClick = { scope.launch { listState.animateScrollToItem(releases.lastIndex.coerceAtLeast(0)) } },
            )
        }
    }
}

private val JumpControlsClearance = 128.dp

@Composable
private fun JumpButton(
    visible: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
        SmallFloatingActionButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
    }
}

@Composable
private fun ReleaseCard(release: Release) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Tokens.RowCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(Tokens.PreferencePadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Tokens.SpacingSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = release.versionName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                release.releasedOn?.let {
                    Text(
                        text = rememberDateText(it),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (release.versionCode == BuildConfig.VERSION_CODE) {
                    Surface(
                        shape = RoundedCornerShape(Tokens.SmallCornerRadius * 2),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.changelog_installed),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            ReleaseNotes(release.notes)
        }
    }
}

private val BadgeRadius = 8.dp
private val BadgeHeight = 22.dp
private val BadgeIconSize = 14.dp
private val NoteMinHeight = 40.dp

@Composable
private fun rememberDateText(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(date, locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date) }
}

@Composable
private fun ReleaseNotes(notes: List<Note>) {
    Column {
        notes.sortedBy { it.kind.ordinal }.forEach { NoteRow(it) }
    }
}

@Composable
private fun NoteRow(note: Note) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = NoteMinHeight)
                .padding(horizontal = 4.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KindBadge(note.kind)
        Text(
            text = note.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun KindBadge(kind: Note.Kind) {
    val scheme = MaterialTheme.colorScheme
    val neutral = scheme.surfaceContainerHighest to scheme.onSurfaceVariant
    val (label, icon, colors) =
        when (kind) {
            Note.Kind.NEW -> Triple(R.string.notes_new, Icons.Outlined.AutoAwesome, scheme.primaryContainer to scheme.onPrimaryContainer)
            Note.Kind.FIXED -> Triple(R.string.notes_fixed, Icons.Outlined.Build, scheme.tertiaryContainer to scheme.onTertiaryContainer)
            Note.Kind.CHANGED -> Triple(R.string.notes_changed, Icons.Outlined.Tune, neutral)
            Note.Kind.REMOVED -> Triple(R.string.notes_removed, Icons.Outlined.RemoveCircleOutline, neutral)
            Note.Kind.UPDATED -> Triple(R.string.notes_updated, Icons.Outlined.Update, neutral)
        }
    Surface(shape = RoundedCornerShape(BadgeRadius), color = colors.first, contentColor = colors.second) {
        Row(
            modifier = Modifier.heightIn(min = BadgeHeight).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(BadgeIconSize))
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelSmallEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun WhatsNewSheet(
    release: Release,
    onFullChangelog: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)) {
            Text(
                text = stringResource(R.string.whats_new_title, release.versionName),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            release.releasedOn?.let {
                Text(
                    text = stringResource(R.string.whats_new_released, rememberDateText(it)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ReleaseNotes(release.notes)
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onClose, shapes = ButtonDefaults.shapes()) {
                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.close), maxLines = 1)
            }
            FilledTonalButton(onClick = onFullChangelog, shapes = ButtonDefaults.shapes()) {
                Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.changelog_see_full), maxLines = 1)
            }
        }
    }
}
