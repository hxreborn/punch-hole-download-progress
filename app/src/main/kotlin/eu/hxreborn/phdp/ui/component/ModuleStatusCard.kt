package eu.hxreborn.phdp.ui.component

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.ui.SettingsViewModel
import eu.hxreborn.phdp.ui.state.ModuleStatus
import eu.hxreborn.phdp.ui.state.SystemUiHook
import eu.hxreborn.phdp.ui.theme.Tokens
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

private val BadgeSize = 48.dp
private val BadgeIconSize = 28.dp
private val RECHECK_INTERVAL = 2.seconds

private enum class StatusAction { NONE, ADD_SCOPE, RELOAD, RESTART }

private data class StatusContent(
    val titleRes: Int,
    val body: String?,
    val healthy: Boolean,
    val checking: Boolean,
    val action: StatusAction,
)

@Composable
fun ModuleStatusSection(
    viewModel: SettingsViewModel,
    onRestartSystemUI: () -> Unit,
    onReloadModule: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val status by viewModel.moduleStatus.collectAsStateWithLifecycle()
    val waitingForSystemUi =
        (status as? ModuleStatus.Active)?.systemUi in
            setOf(SystemUiHook.NOT_LOADED, SystemUiHook.OUTDATED, SystemUiHook.FAILED)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, waitingForSystemUi) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            do {
                viewModel.refreshModuleStatus()
                delay(RECHECK_INTERVAL)
            } while (waitingForSystemUi)
        }
    }
    val haptics = LocalHapticFeedback.current
    val scopeFailed = stringResource(R.string.status_scope_failed)
    ModuleStatusCard(
        status = status,
        onAddScope = {
            viewModel.requestSystemUiScope { failure ->
                if (failure == null) {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                } else {
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    Toast.makeText(context, String.format(scopeFailed, failure), Toast.LENGTH_LONG).show()
                }
            }
        },
        onRestartSystemUI = onRestartSystemUI,
        onReloadModule = onReloadModule,
        modifier = modifier,
    )
}

@Composable
private fun ModuleStatus.content(): StatusContent =
    when (this) {
        ModuleStatus.Checking -> {
            StatusContent(R.string.status_checking, null, healthy = true, checking = true, action = StatusAction.NONE)
        }

        ModuleStatus.Inactive -> {
            StatusContent(
                R.string.status_inactive_title,
                stringResource(R.string.status_inactive_body),
                healthy = false,
                checking = false,
                action = StatusAction.NONE,
            )
        }

        is ModuleStatus.Active -> {
            val framework = stringResource(R.string.status_framework, frameworkName, frameworkVersion, apiVersion)
            when (systemUi) {
                SystemUiHook.RUNNING -> {
                    StatusContent(R.string.status_running_title, framework, true, false, StatusAction.NONE)
                }

                SystemUiHook.IN_SCOPE -> {
                    StatusContent(R.string.status_active_title, framework, true, false, StatusAction.NONE)
                }

                SystemUiHook.NOT_IN_SCOPE -> {
                    StatusContent(
                        R.string.status_scope_title,
                        stringResource(R.string.status_scope_body),
                        false,
                        false,
                        StatusAction.ADD_SCOPE,
                    )
                }

                SystemUiHook.NOT_LOADED -> {
                    StatusContent(
                        R.string.status_not_loaded_title,
                        stringResource(R.string.status_not_loaded_body),
                        false,
                        false,
                        StatusAction.RESTART,
                    )
                }

                SystemUiHook.OUTDATED -> {
                    StatusContent(
                        R.string.status_outdated_title,
                        stringResource(R.string.status_outdated_body),
                        false,
                        false,
                        StatusAction.RELOAD,
                    )
                }

                SystemUiHook.FAILED -> {
                    StatusContent(
                        R.string.status_failed_title,
                        stringResource(R.string.status_failed_body),
                        false,
                        false,
                        StatusAction.RESTART,
                    )
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModuleStatusCard(
    status: ModuleStatus,
    onAddScope: () -> Unit,
    onRestartSystemUI: () -> Unit,
    onReloadModule: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = status.content()
    val colors = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        if (content.healthy) colors.surfaceContainerHigh else colors.errorContainer,
        label = "statusContainer",
    )
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Tokens.SectionHorizontalMargin),
        shape = RoundedCornerShape(Tokens.RowCornerRadius),
        color = containerColor,
    ) {
        AnimatedContent(
            targetState = content,
            contentKey = { it.titleRes },
            transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
            label = "statusContent",
        ) { shown ->
            Column(
                modifier = Modifier.padding(Tokens.PreferencePadding),
                verticalArrangement = Arrangement.spacedBy(Tokens.SpacingLg),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Tokens.PreferenceHorizontalSpacing),
                ) {
                    StatusBadge(checking = shown.checking, healthy = shown.healthy)
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    ) {
                        Text(
                            text = stringResource(shown.titleRes),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (shown.healthy) colors.onSurface else colors.onErrorContainer,
                        )
                        shown.body?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (shown.healthy) colors.onSurfaceVariant else colors.onErrorContainer,
                            )
                        }
                    }
                }
                when (shown.action) {
                    StatusAction.NONE -> {}

                    StatusAction.ADD_SCOPE -> {
                        StatusActionButton(stringResource(R.string.status_scope_action), onAddScope)
                    }

                    StatusAction.RELOAD -> {
                        StatusActionButton(stringResource(R.string.reload_module), onReloadModule)
                    }

                    StatusAction.RESTART -> {
                        StatusActionButton(stringResource(R.string.restart_systemui), onRestartSystemUI)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusBadge(
    checking: Boolean,
    healthy: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    if (checking) {
        LoadingIndicator(modifier = Modifier.size(BadgeSize))
        return
    }
    if (healthy) {
        MorphingLogo(modifier = Modifier.size(BadgeSize)) {
            Box(Modifier.matchParentSize().background(colors.primary))
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(BadgeIconSize),
            )
        }
        return
    }
    Box(
        modifier =
            Modifier
                .size(BadgeSize)
                .background(color = colors.error, shape = MaterialShapes.Sunny.toShape()),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.PriorityHigh,
            contentDescription = null,
            tint = colors.onError,
            modifier = Modifier.size(BadgeIconSize),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusActionButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text)
    }
}
