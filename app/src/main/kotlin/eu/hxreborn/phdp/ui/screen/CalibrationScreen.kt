package eu.hxreborn.phdp.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.ui.component.SectionCard
import eu.hxreborn.phdp.ui.component.SettingsScaffold
import eu.hxreborn.phdp.ui.component.preference.SliderPreferenceWithStepper
import eu.hxreborn.phdp.ui.component.preference.TogglePreferenceWithIcon
import eu.hxreborn.phdp.ui.state.PrefsState
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig
import eu.hxreborn.phdp.ui.theme.Tokens
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceCategory

@Composable
fun CalibrationScreen(
    prefsState: PrefsState,
    onSavePrefs: (key: String, value: Any) -> Unit,
    onNavigateBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    // Enable preview when screen is visible, disable when backgrounded or navigating away
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        onSavePrefs(Prefs.persistentPreview.key, true)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        onSavePrefs(Prefs.persistentPreview.key, false)
    }
    DisposableEffect(Unit) {
        onDispose { onSavePrefs(Prefs.persistentPreview.key, false) }
    }

    SettingsScaffold(
        title = stringResource(R.string.pref_calibrate_ring_title),
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    ) { innerPadding ->
        ProvidePreferenceLocals {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        top = innerPadding.calculateTopPadding() + Tokens.SpacingLg,
                        bottom = contentPadding.calculateBottomPadding() + Tokens.SpacingLg,
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
                                            onSavePrefs(Prefs.ringOffsetX.key, it)
                                        },
                                        title = {
                                            Text(stringResource(R.string.pref_ring_offset_x_title))
                                        },
                                        valueRange = Prefs.ringOffsetX.range!!,
                                        defaultValue = Prefs.ringOffsetX.default,
                                        onReset = {
                                            onSavePrefs(
                                                Prefs.ringOffsetX.key,
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
                                            onSavePrefs(Prefs.ringOffsetY.key, it)
                                        },
                                        title = {
                                            Text(stringResource(R.string.pref_ring_offset_y_title))
                                        },
                                        valueRange = Prefs.ringOffsetY.range!!,
                                        defaultValue = Prefs.ringOffsetY.default,
                                        onReset = {
                                            onSavePrefs(
                                                Prefs.ringOffsetY.key,
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
                                            onSavePrefs(Prefs.ringScaleLinked.key, it)
                                            if (it) {
                                                onSavePrefs(
                                                    Prefs.ringScaleY.key,
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
                                            onSavePrefs(Prefs.ringScaleX.key, it)
                                            if (prefsState.ringScaleLinked) {
                                                onSavePrefs(Prefs.ringScaleY.key, it)
                                            }
                                        },
                                        title = {
                                            Text(stringResource(R.string.pref_ring_scale_x_title))
                                        },
                                        valueRange = Prefs.ringScaleX.range!!,
                                        defaultValue = Prefs.ringScaleX.default,
                                        onReset = {
                                            onSavePrefs(
                                                Prefs.ringScaleX.key,
                                                Prefs.ringScaleX.default,
                                            )
                                            if (prefsState.ringScaleLinked) {
                                                onSavePrefs(
                                                    Prefs.ringScaleY.key,
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
                                            onSavePrefs(Prefs.ringScaleY.key, it)
                                        },
                                        title = {
                                            Text(stringResource(R.string.pref_ring_scale_y_title))
                                        },
                                        valueRange = Prefs.ringScaleY.range!!,
                                        defaultValue = Prefs.ringScaleY.default,
                                        onReset = {
                                            onSavePrefs(
                                                Prefs.ringScaleY.key,
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CalibrationScreenPreview() {
    AppTheme(darkThemeConfig = DarkThemeConfig.DARK) {
        CalibrationScreen(
            prefsState = PrefsState(),
            onSavePrefs = { _, _ -> },
            onNavigateBack = {},
            contentPadding = PaddingValues(),
        )
    }
}
