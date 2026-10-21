package eu.hxreborn.phdp.ui.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.hxreborn.phdp.BuildConfig
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.about.loadReleases
import eu.hxreborn.phdp.ui.SettingsViewModel
import eu.hxreborn.phdp.ui.component.MorphingLogo
import eu.hxreborn.phdp.ui.component.SectionCard
import eu.hxreborn.phdp.ui.component.SettingsScaffold
import eu.hxreborn.phdp.ui.component.VerticalScrollbar
import eu.hxreborn.phdp.ui.component.sectionPreferenceTheme
import eu.hxreborn.phdp.ui.state.ModuleStatus
import eu.hxreborn.phdp.ui.theme.Tokens
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceCategory

private const val REPO_URL = "https://github.com/hxreborn/punch-hole-download-progress"
private const val ISSUE_CHOOSER_URL = "$REPO_URL/issues/new/choose"
private const val LICENSE_URL = "$REPO_URL/blob/main/LICENSE"
private const val KOFI_URL = "https://ko-fi.com/hxreborn"
private const val LICENSE_NAME = "GPL-3.0"
private const val ADAPTIVE_ICON_ZOOM = 1.5f
private val IconBadgeSize = 88.dp
private val ConnectedButtonHeight = 48.dp
private val releaseTag = Regex("""\d+\.\d+\.\d+""")

private fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
    }
}

private fun releaseUrl(): String =
    if (releaseTag.matches(BuildConfig.VERSION_NAME)) {
        "$REPO_URL/releases/tag/v${BuildConfig.VERSION_NAME}"
    } else {
        "$REPO_URL/releases"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChangelog: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    modifier: Modifier = Modifier,
    bottomNavPadding: Dp = 0.dp,
) {
    val context = LocalContext.current
    val status by viewModel.moduleStatus.collectAsStateWithLifecycle()
    val releases = remember(context) { loadReleases(context.assets) }
    val currentRelease = releases.firstOrNull { it.versionCode == BuildConfig.VERSION_CODE } ?: releases.firstOrNull()
    var showWhatsNew by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (showWhatsNew && currentRelease != null) {
        val closeSheet = { then: () -> Unit ->
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                showWhatsNew = false
                then()
            }
        }
        ModalBottomSheet(onDismissRequest = { showWhatsNew = false }, sheetState = sheetState) {
            WhatsNewSheet(
                release = currentRelease,
                onFullChangelog = { closeSheet(onNavigateToChangelog) },
                onClose = { closeSheet {} },
            )
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.group_about),
        onNavigateBack = onNavigateBack,
        bottomPadding = bottomNavPadding,
        modifier = modifier,
    ) { innerPadding ->
        ProvidePreferenceLocals(theme = sectionPreferenceTheme()) {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        top = innerPadding.calculateTopPadding() + Tokens.SpacingLg,
                        bottom = innerPadding.calculateBottomPadding() + Tokens.SpacingLg,
                    ),
            ) {
                item(key = "about_identity") {
                    IdentityCard(onOpenUrl = context::openUrl)
                }

                item(key = "about_links") {
                    SectionCard(
                        modifier = Modifier.padding(top = Tokens.SpacingLg),
                        items =
                            listOfNotNull(
                                currentRelease?.let { release ->
                                    {
                                        AboutRow(
                                            icon = Icons.Outlined.NewReleases,
                                            title = stringResource(R.string.whats_new),
                                            summary = stringResource(R.string.whats_new_summary, release.versionName),
                                            trailing = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            onClick = { showWhatsNew = true },
                                        )
                                    }
                                },
                                {
                                    AboutRow(
                                        icon = Icons.Outlined.History,
                                        title = stringResource(R.string.changelog),
                                        summary = stringResource(R.string.changelog_summary),
                                        trailing = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        onClick = onNavigateToChangelog,
                                    )
                                },
                                {
                                    AboutRow(
                                        painter = R.drawable.ic_github_24,
                                        title = stringResource(R.string.pref_git_repo),
                                        summary = stringResource(R.string.pref_git_repo_summary),
                                        trailing = Icons.AutoMirrored.Outlined.OpenInNew,
                                        onClick = { context.openUrl(REPO_URL) },
                                    )
                                },
                                {
                                    AboutRow(
                                        icon = Icons.Outlined.Lightbulb,
                                        title = stringResource(R.string.pref_report_issue),
                                        summary = stringResource(R.string.pref_report_issue_summary),
                                        trailing = Icons.AutoMirrored.Outlined.OpenInNew,
                                        onClick = { context.openUrl(ISSUE_CHOOSER_URL) },
                                    )
                                },
                                {
                                    AboutRow(
                                        icon = Icons.Outlined.Gavel,
                                        title = stringResource(R.string.pref_licenses),
                                        summary = stringResource(R.string.pref_licenses_summary),
                                        trailing = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        onClick = onNavigateToLicenses,
                                    )
                                },
                            ),
                    )
                }

                preferenceCategory(
                    key = "about_device_header",
                    title = { Text(stringResource(R.string.group_this_device)) },
                )

                item(key = "about_device") {
                    SectionCard(
                        items =
                            listOf(
                                {
                                    AboutRow(
                                        icon = Icons.Outlined.Android,
                                        title = stringResource(R.string.device_android),
                                        summary =
                                            stringResource(
                                                R.string.device_android_version,
                                                Build.VERSION.RELEASE,
                                                Build.VERSION.SDK_INT,
                                            ),
                                    )
                                },
                                {
                                    AboutRow(
                                        icon = Icons.Outlined.Extension,
                                        title = stringResource(R.string.device_framework),
                                        summary =
                                            when (val s = status) {
                                                is ModuleStatus.Active -> {
                                                    stringResource(
                                                        R.string.status_framework,
                                                        s.frameworkName,
                                                        s.frameworkVersion,
                                                        s.apiVersion,
                                                    )
                                                }

                                                ModuleStatus.Checking -> {
                                                    stringResource(R.string.status_checking)
                                                }

                                                ModuleStatus.Inactive -> {
                                                    stringResource(R.string.device_framework_none)
                                                }
                                            },
                                    )
                                },
                            ),
                    )
                }
            }
            VerticalScrollbar(listState, innerPadding)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun IdentityCard(onOpenUrl: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Tokens.SectionHorizontalMargin),
        shape = RoundedCornerShape(Tokens.RowCornerRadius),
        color = colors.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MorphingLogo(modifier = Modifier.size(IconBadgeSize)) {
                Box(Modifier.matchParentSize().background(colorResource(R.color.ic_launcher_background)))
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier =
                        Modifier.fillMaxSize().graphicsLayer {
                            scaleX = ADAPTIVE_ICON_ZOOM
                            scaleY = ADAPTIVE_ICON_ZOOM
                        },
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.app_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = { onOpenUrl(releaseUrl()) },
                    label = { Text("v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Sell, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                    },
                )
                AssistChip(
                    onClick = { onOpenUrl(LICENSE_URL) },
                    label = { Text(LICENSE_NAME) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Balance, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                    },
                )
            }
            LinkButtons(onOpenUrl = onOpenUrl, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LinkButtons(
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fontScale = LocalDensity.current.fontScale
    val windowWidth = LocalWindowInfo.current.containerSize.width
    var stacked by remember(fontScale, windowWidth) { mutableStateOf(false) }
    val onLabelLayout: (TextLayoutResult) -> Unit = { if (it.hasVisualOverflow) stacked = true }
    val starButton: @Composable (ButtonShapes, Modifier) -> Unit = { shapes, buttonModifier ->
        Button(onClick = { onOpenUrl(REPO_URL) }, shapes = shapes, modifier = buttonModifier.heightIn(min = ConnectedButtonHeight)) {
            Icon(Icons.Outlined.StarOutline, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.star_on_github), maxLines = 1, onTextLayout = onLabelLayout)
        }
    }
    val coffeeButton: @Composable (ButtonShapes, Modifier) -> Unit = { shapes, buttonModifier ->
        FilledTonalButton(
            onClick = { onOpenUrl(KOFI_URL) },
            shapes = shapes,
            modifier = buttonModifier.heightIn(min = ConnectedButtonHeight),
        ) {
            Icon(Icons.Outlined.Coffee, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.buy_me_a_coffee), maxLines = 1, onTextLayout = onLabelLayout)
        }
    }
    if (stacked) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            starButton(ButtonDefaults.shapes(), Modifier.fillMaxWidth())
            coffeeButton(ButtonDefaults.shapes(), Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            starButton(
                ButtonDefaults.shapes(
                    ButtonGroupDefaults.connectedLeadingButtonShape,
                    ButtonGroupDefaults.connectedLeadingButtonPressShape,
                ),
                Modifier.weight(1f),
            )
            coffeeButton(
                ButtonDefaults.shapes(
                    ButtonGroupDefaults.connectedTrailingButtonShape,
                    ButtonGroupDefaults.connectedTrailingButtonPressShape,
                ),
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AboutRow(
    title: String,
    summary: String,
    icon: ImageVector? = null,
    painter: Int? = null,
    trailing: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    Preference(
        onClick = onClick,
        enabled = true,
        icon = {
            when {
                icon != null -> Icon(icon, contentDescription = null)
                painter != null -> Icon(painterResource(painter), contentDescription = null)
            }
        },
        title = { Text(title) },
        summary = { Text(summary) },
        widgetContainer =
            trailing?.let {
                {
                    Icon(
                        it,
                        contentDescription = null,
                        modifier = Modifier.padding(horizontal = Tokens.PreferencePadding),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
    )
}
