package eu.hxreborn.phdp.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.ui.SettingsUiState
import eu.hxreborn.phdp.ui.SettingsViewModel
import eu.hxreborn.phdp.ui.component.SectionCard
import eu.hxreborn.phdp.ui.component.SettingsScaffold
import eu.hxreborn.phdp.ui.component.preference.SliderPreferenceWithStepper
import eu.hxreborn.phdp.ui.component.preference.TogglePreferenceWithIcon
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig
import eu.hxreborn.phdp.ui.theme.Tokens
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceCategory

@Composable
fun CalibrationScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomNavPadding: Dp = 0.dp,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefsState = (uiState as? SettingsUiState.Success)?.prefs ?: return

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.savePref(Prefs.persistentPreview, true)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.savePref(Prefs.persistentPreview, false)
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.savePref(Prefs.persistentPreview, false) }
    }

    SettingsScaffold(
        title = stringResource(R.string.pref_calibrate_ring_title),
        onNavigateBack = onNavigateBack,
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
                preferenceCategory(
                    key = "calibration_position_header",
                    title = { Text(stringResource(R.string.group_position)) },
                )

                item(key = "calibration_position_section") {
                    SectionCard(
                        items =
                            listOf(
                                {
                                    SliderPreferenceWithStepper(
                                        value = prefsState.ringOffsetX,
                                        onValueChange = {
                                            viewModel.savePref(Prefs.ringOffsetX, it)
                                        },
                                        title = {
                                            Text(stringResource(R.string.pref_ring_offset_x_title))
                                        },
                                        valueRange = Prefs.ringOffsetX.range!!,
                                        defaultValue = Prefs.ringOffsetX.default,
                                        onReset = {
                                            viewModel.savePref(
                                                Prefs.ringOffsetX,
                                                Prefs.ringOffsetX.default,
                                            )
                                        },
                                        stepSize = 1f,
                                        decimalPlaces = 0,
                                        suffix = "px",
                                    )
                                },
                                {
                                    SliderPreferenceWithStepper(
                                        value = prefsState.ringOffsetY,
                                        onValueChange = {
                                            viewModel.savePref(Prefs.ringOffsetY, it)
                                        },
                                        title = {
                                            Text(stringResource(R.string.pref_ring_offset_y_title))
                                        },
                                        valueRange = Prefs.ringOffsetY.range!!,
                                        defaultValue = Prefs.ringOffsetY.default,
                                        onReset = {
                                            viewModel.savePref(
                                                Prefs.ringOffsetY,
                                                Prefs.ringOffsetY.default,
                                            )
                                        },
                                        stepSize = 1f,
                                        decimalPlaces = 0,
                                        suffix = "px",
                                    )
                                },
                            ),
                    )
                }

                preferenceCategory(
                    key = "calibration_size_header",
                    title = { Text(stringResource(R.string.group_size)) },
                )

                item(key = "calibration_size_section") {
                    SectionCard(
                        items =
                            listOf(
                                {
                                    TogglePreferenceWithIcon(
                                        value = prefsState.ringScaleLinked,
                                        onValueChange = {
                                            viewModel.savePref(Prefs.ringScaleLinked, it)
                                            if (it) {
                                                viewModel.savePref(
                                                    Prefs.ringScaleY,
                                                    prefsState.ringScaleX,
                                                )
                                            }
                                        },
                                        title = {
                                            Text(stringResource(R.string.pref_ring_scale_linked_title))
                                        },
                                        summary = {
                                            Text(stringResource(R.string.pref_ring_scale_linked_summary))
                                        },
                                    )
                                },
                                {
                                    SliderPreferenceWithStepper(
                                        value = prefsState.ringScaleX,
                                        onValueChange = {
                                            viewModel.savePref(Prefs.ringScaleX, it)
                                            if (prefsState.ringScaleLinked) {
                                                viewModel.savePref(Prefs.ringScaleY, it)
                                            }
                                        },
                                        title = {
                                            Text(stringResource(R.string.pref_ring_scale_x_title))
                                        },
                                        valueRange = Prefs.ringScaleX.range!!,
                                        defaultValue = Prefs.ringScaleX.default,
                                        onReset = {
                                            viewModel.savePref(
                                                Prefs.ringScaleX,
                                                Prefs.ringScaleX.default,
                                            )
                                            if (prefsState.ringScaleLinked) {
                                                viewModel.savePref(
                                                    Prefs.ringScaleY,
                                                    Prefs.ringScaleY.default,
                                                )
                                            }
                                        },
                                        stepSize = 0.05f,
                                        decimalPlaces = 2,
                                        suffix = "x",
                                    )
                                },
                                {
                                    SliderPreferenceWithStepper(
                                        value = prefsState.ringScaleY,
                                        onValueChange = {
                                            viewModel.savePref(Prefs.ringScaleY, it)
                                        },
                                        title = {
                                            Text(stringResource(R.string.pref_ring_scale_y_title))
                                        },
                                        valueRange = Prefs.ringScaleY.range!!,
                                        defaultValue = Prefs.ringScaleY.default,
                                        onReset = {
                                            viewModel.savePref(
                                                Prefs.ringScaleY,
                                                Prefs.ringScaleY.default,
                                            )
                                        },
                                        stepSize = 0.05f,
                                        decimalPlaces = 2,
                                        suffix = "x",
                                        enabled = !prefsState.ringScaleLinked,
                                    )
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Suppress("ViewModelConstructorInComposable")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CalibrationScreenPreview() {
    AppTheme(darkThemeConfig = DarkThemeConfig.DARK) {
        CalibrationScreen(
            viewModel = PreviewViewModel(),
            onNavigateBack = {},
        )
    }
}
