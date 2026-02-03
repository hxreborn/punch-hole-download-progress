package eu.hxreborn.phdp.ui.screen

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import eu.hxreborn.phdp.BuildConfig
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.ui.component.AppTypeChip
import eu.hxreborn.phdp.ui.component.ExpressiveCheckbox
import eu.hxreborn.phdp.ui.component.OverflowMenu
import eu.hxreborn.phdp.ui.component.OverflowMenuItem
import eu.hxreborn.phdp.ui.component.OverflowMenuToggle
import eu.hxreborn.phdp.ui.component.drawVerticalScrollbar
import eu.hxreborn.phdp.ui.state.PrefsState
import eu.hxreborn.phdp.ui.theme.Tokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class AppItem(
    val applicationInfo: ApplicationInfo,
    val label: String,
    val packageName: String,
    val lastUpdateTime: Long,
    val isSystem: Boolean,
)

private data class AppLoadState(
    val apps: List<AppItem>,
    val isLoading: Boolean,
)

@SuppressLint("QueryPermissionsNeeded")
private suspend fun loadApps(pm: PackageManager): List<AppItem> =
    withContext(Dispatchers.IO) {
        pm.getInstalledPackages(PackageManager.GET_META_DATA).mapNotNull { pkg ->
            val appInfo = pkg.applicationInfo ?: return@mapNotNull null
            AppItem(
                applicationInfo = appInfo,
                label = appInfo.loadLabel(pm).toString(),
                packageName = pkg.packageName,
                lastUpdateTime = pkg.lastUpdateTime,
                isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            )
        }
    }

@Composable
private fun rememberInstalledApps(): AppLoadState {
    val context = LocalContext.current
    val pm = context.packageManager

    return produceState(initialValue = AppLoadState(emptyList(), isLoading = true)) {
        val apps = loadApps(pm)
        if (BuildConfig.DEBUG) Log.d("PHDP", "Package list loaded: ${apps.size}")
        value = AppLoadState(apps, isLoading = false)
    }.value
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    showSystemApps: Boolean,
    onSystemToggle: (Boolean) -> Unit,
    selectedCount: Int,
    onClearClick: () -> Unit,
    onApplyDefaults: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.packages_search_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search))
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                    }
                    OverflowMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                    ) {
                        OverflowMenuToggle(
                            text = stringResource(R.string.packages_filter_system),
                            icon = Icons.Outlined.Visibility,
                            checked = showSystemApps,
                            onCheckedChange = onSystemToggle,
                        )
                        HorizontalDivider()
                        OverflowMenuItem(
                            text = stringResource(R.string.clear_selection_count, selectedCount),
                            icon = Icons.Outlined.ClearAll,
                            onClick = {
                                showMenu = false
                                onClearClick()
                            },
                            enabled = selectedCount > 0,
                            textColor =
                                if (selectedCount > 0) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    Color.Unspecified
                                },
                            iconTint =
                                if (selectedCount > 0) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    LocalContentColor.current
                                },
                        )
                        OverflowMenuItem(
                            text = stringResource(R.string.apply_defaults),
                            icon = Icons.Outlined.SaveAlt,
                            onClick = {
                                showMenu = false
                                onApplyDefaults()
                            },
                        )
                        HorizontalDivider()
                        OverflowMenuItem(
                            text = stringResource(R.string.help),
                            icon = Icons.AutoMirrored.Outlined.HelpOutline,
                            onClick = {
                                showMenu = false
                                onHelp()
                            },
                        )
                    }
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(Tokens.RowCornerRadius),
        colors =
            OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            ),
    )
}

@Composable
private fun AppIcon(
    icon: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Tokens.AppIconCornerRadius),
        color = icon?.let { Color.Transparent } ?: MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        when (icon) {
            null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Tokens.LoadingIndicatorSize),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
            else -> Image(icon, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun AppListItem(
    app: AppItem,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = null, key1 = app.packageName) {
        value =
            withContext(Dispatchers.IO) {
                runCatching {
                    app.applicationInfo
                        .loadIcon(context.packageManager)
                        .toBitmap()
                        .asImageBitmap()
                }.getOrNull()
            }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Tokens.RowCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onCheckedChange(!isChecked) }
                    .padding(Tokens.PreferencePadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(icon = icon, modifier = Modifier.size(Tokens.AppIconSize))
            Spacer(modifier = Modifier.width(Tokens.PreferenceHorizontalSpacing))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(Tokens.SpacingSm))
                AppTypeChip(isSystem = app.isSystem)
            }
            Spacer(modifier = Modifier.width(Tokens.PreferenceHorizontalSpacing))
            ExpressiveCheckbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun ShimmerListItem(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "shimmer_translate",
    )

    val baseColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlightColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val shimmerBrush =
        Brush.linearGradient(
            colors = listOf(baseColor, highlightColor, baseColor),
            start = Offset(translateAnim - 500f, 0f),
            end = Offset(translateAnim, 0f),
        )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Tokens.RowCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(Tokens.PreferencePadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(Tokens.AppIconSize)
                        .background(shimmerBrush, RoundedCornerShape(Tokens.AppIconCornerRadius)),
            )
            Spacer(modifier = Modifier.width(Tokens.PreferenceHorizontalSpacing))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.6f)
                            .height(Tokens.SkeletonTitleHeight)
                            .background(shimmerBrush, RoundedCornerShape(Tokens.SmallCornerRadius)),
                )
                Spacer(modifier = Modifier.height(Tokens.SpacingSm))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.8f)
                            .height(Tokens.SkeletonSubtitleHeight)
                            .background(shimmerBrush, RoundedCornerShape(Tokens.SmallCornerRadius)),
                )
            }
            Spacer(modifier = Modifier.width(Tokens.PreferenceHorizontalSpacing))
            Box(
                modifier =
                    Modifier
                        .size(Tokens.CheckboxSize)
                        .background(shimmerBrush, RoundedCornerShape(Tokens.SmallCornerRadius)),
            )
        }
    }
}

@Composable
fun PackageSelectionScreen(
    prefsState: PrefsState,
    onSavePrefs: (key: String, value: Any) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val initialSelectedPackages = remember { prefsState.selectedPackages }
    val appState = rememberInstalledApps()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val saveSelection: (Set<String>) -> Unit = { selection ->
        onSavePrefs(Prefs.selectedPackages.key, selection)
    }

    val selectedCount = prefsState.selectedPackages.size

    LaunchedEffect(searchQuery) {
        delay(Tokens.SEARCH_DEBOUNCE_MS)
        debouncedQuery = searchQuery
    }

    // Show all apps filtered by search and system toggle
    val filteredApps =
        remember(appState.apps, debouncedQuery, showSystemApps) {
            appState.apps.filter { app ->
                val matchesSearch =
                    debouncedQuery.isEmpty() ||
                        app.label.contains(
                            debouncedQuery,
                            ignoreCase = true,
                        ) || app.packageName.contains(debouncedQuery, ignoreCase = true)
                val matchesSystem = showSystemApps || !app.isSystem
                matchesSearch && matchesSystem
            }
        }

    // Uses initialSelectedPackages to prevent re-sorting during selection changes
    val sortedApps =
        remember(filteredApps, initialSelectedPackages) {
            filteredApps.sortedWith(
                compareByDescending<AppItem> {
                    it.packageName in initialSelectedPackages
                }.thenBy { it.label.lowercase() },
            )
        }

    val clearSelectionMessage = stringResource(R.string.selection_cleared)
    val defaultsAppliedMessage = stringResource(R.string.defaults_applied)
    val defaultsNoneMessage = stringResource(R.string.defaults_none_added)
    val undoLabel = stringResource(R.string.undo)

    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        top = contentPadding.calculateTopPadding() + Tokens.SpacingLg,
                        bottom = contentPadding.calculateBottomPadding() + Tokens.SpacingLg,
                    ),
        ) {
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .drawVerticalScrollbar(listState),
            ) {
                item(key = "search") {
                    SearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        showSystemApps = showSystemApps,
                        onSystemToggle = { showSystemApps = it },
                        selectedCount = selectedCount,
                        onClearClick = { showClearDialog = true },
                        onApplyDefaults = {
                            val installedPackageNames = appState.apps.map { it.packageName }.toSet()
                            applyDefaults(
                                installedPackageNames = installedPackageNames,
                                currentSelection = prefsState.selectedPackages,
                                onSave = saveSelection,
                                snackbarHostState = snackbarHostState,
                                scope = scope,
                                appliedMessage = defaultsAppliedMessage,
                                noneMessage = defaultsNoneMessage,
                                undoLabel = undoLabel,
                            )
                        },
                        onHelp = { showHelpDialog = true },
                        modifier = Modifier.padding(horizontal = Tokens.SectionHorizontalMargin),
                    )
                }

                item { Spacer(modifier = Modifier.height(Tokens.SpacingLg)) }

                when {
                    appState.isLoading -> {
                        items(Tokens.SHIMMER_PLACEHOLDER_COUNT) {
                            ShimmerListItem(
                                modifier =
                                    Modifier.padding(
                                        horizontal = Tokens.SectionHorizontalMargin,
                                        vertical = Tokens.SpacingSm,
                                    ),
                            )
                        }
                    }

                    sortedApps.isEmpty() -> {
                        item(key = "empty") {
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { visible = true }

                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(Tokens.SpacingLg),
                                contentAlignment = Alignment.Center,
                            ) {
                                AnimatedVisibility(
                                    visible = visible,
                                    enter =
                                        slideInVertically(
                                            initialOffsetY = { -it * 2 },
                                            animationSpec =
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow,
                                                ),
                                        ) + fadeIn(animationSpec = tween(Tokens.EMPTY_STATE_FADE_MS)),
                                ) {
                                    Text(
                                        text = stringResource(R.string.packages_empty_search),
                                        modifier = Modifier.padding(vertical = Tokens.EmptyStatePadding),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        items(
                            items = sortedApps,
                            key = { it.packageName },
                        ) { app ->
                            val isSelected = app.packageName in prefsState.selectedPackages
                            AppListItem(
                                app = app,
                                isChecked = isSelected,
                                onCheckedChange = { checked ->
                                    val newSelection =
                                        if (checked) {
                                            prefsState.selectedPackages + app.packageName
                                        } else {
                                            prefsState.selectedPackages - app.packageName
                                        }
                                    saveSelection(newSelection)
                                },
                                modifier =
                                    Modifier.padding(
                                        horizontal = Tokens.SectionHorizontalMargin,
                                        vertical = Tokens.SpacingSm,
                                    ),
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = contentPadding.calculateBottomPadding()),
        )
    }

    if (showClearDialog) {
        ClearSelectionDialog(
            selectedCount = selectedCount,
            onConfirm = {
                val previousSelection = prefsState.selectedPackages
                saveSelection(emptySet())
                showClearDialog = false
                scope.launch {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = clearSelectionMessage,
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Short,
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        saveSelection(previousSelection)
                    }
                }
            },
            onDismiss = { showClearDialog = false },
        )
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }
}

private fun applyDefaults(
    installedPackageNames: Set<String>,
    currentSelection: Set<String>,
    onSave: (Set<String>) -> Unit,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    appliedMessage: String,
    noneMessage: String,
    undoLabel: String,
) {
    val defaultsToApply = Prefs.defaultSupportedPackages.filter { it in installedPackageNames }.toSet()
    val newlyAdded = defaultsToApply - currentSelection
    if (newlyAdded.isEmpty()) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = noneMessage,
                duration = SnackbarDuration.Short,
            )
        }
        return
    }

    val previousSelection = currentSelection
    onSave(currentSelection + defaultsToApply)
    scope.launch {
        val result =
            snackbarHostState.showSnackbar(
                message = appliedMessage.format(newlyAdded.size),
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
        if (result == SnackbarResult.ActionPerformed) {
            onSave(previousSelection)
        }
    }
}

@Composable
private fun ClearSelectionDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Tokens.DialogShape,
        title = { Text(stringResource(R.string.clear_selection)) },
        text = { Text(stringResource(R.string.clear_selection_confirm, selectedCount)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.clear))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Tokens.DialogShape,
        title = { Text(stringResource(R.string.help)) },
        text = { Text(stringResource(R.string.packages_help)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}
