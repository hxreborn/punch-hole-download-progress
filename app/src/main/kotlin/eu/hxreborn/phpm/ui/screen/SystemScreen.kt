package eu.hxreborn.phpm.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Power
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.hxreborn.phpm.R
import eu.hxreborn.phpm.prefs.PrefsManager
import eu.hxreborn.phpm.ui.component.MasterSwitch
import eu.hxreborn.phpm.ui.component.SectionHeader
import eu.hxreborn.phpm.ui.component.TweakButton
import eu.hxreborn.phpm.ui.component.TweakSelection
import eu.hxreborn.phpm.ui.component.TweakSwitch
import eu.hxreborn.phpm.ui.state.PrefsState

@Composable
fun SystemScreen(
    prefsState: PrefsState,
    onSavePrefs: (key: String, value: Any) -> Unit,
    onTestSuccess: () -> Unit,
    onTestFailure: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val powerSaverEntries = context.resources.getStringArray(R.array.power_saver_entries).toList()
    val powerSaverValues = context.resources.getStringArray(R.array.power_saver_values).toList()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                top = contentPadding.calculateTopPadding() + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
    ) {
        item {
            MasterSwitch(
                title = stringResource(R.string.master_enabled),
                summaryOn = stringResource(R.string.master_enabled_on),
                summaryOff = stringResource(R.string.master_enabled_off),
                checked = prefsState.enabled,
                onCheckedChange = { checked ->
                    onSavePrefs(PrefsManager.KEY_ENABLED, checked)
                },
                icon = Icons.Default.Power,
            )
        }

        item {
            SectionHeader(title = stringResource(R.string.group_visibility))
        }

        item {
            TweakSwitch(
                title = stringResource(R.string.display_item_count),
                description = stringResource(R.string.display_item_count_desc),
                checked = prefsState.showDownloadCount,
                onCheckedChange = { checked ->
                    onSavePrefs(PrefsManager.KEY_SHOW_DOWNLOAD_COUNT, checked)
                },
                enabled = prefsState.enabled,
            )
        }

        item {
            TweakSwitch(
                title = stringResource(R.string.fill_direction),
                description =
                    if (prefsState.clockwise) {
                        stringResource(R.string.clockwise)
                    } else {
                        stringResource(R.string.counter_clockwise)
                    },
                checked = prefsState.clockwise,
                onCheckedChange = { checked ->
                    onSavePrefs(PrefsManager.KEY_CLOCKWISE, checked)
                },
                enabled = prefsState.enabled,
            )
        }

        item {
            SectionHeader(title = stringResource(R.string.group_power))
        }

        item {
            TweakSelection(
                title = stringResource(R.string.battery_saver_mode),
                description = stringResource(R.string.battery_saver_mode_desc),
                currentValue = prefsState.powerSaverMode,
                entries = powerSaverEntries,
                values = powerSaverValues,
                onValueSelected = { value ->
                    onSavePrefs(PrefsManager.KEY_POWER_SAVER_MODE, value)
                },
                enabled = prefsState.enabled,
            )
        }

        item {
            SectionHeader(title = stringResource(R.string.group_diagnostics))
        }

        item {
            TweakButton(
                title = stringResource(R.string.test_success),
                onClick = onTestSuccess,
                enabled = prefsState.enabled,
            )
        }

        item {
            TweakButton(
                title = stringResource(R.string.test_failure),
                onClick = onTestFailure,
                enabled = prefsState.enabled,
            )
        }
    }
}
