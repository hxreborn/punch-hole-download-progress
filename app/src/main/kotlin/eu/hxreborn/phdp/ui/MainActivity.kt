package eu.hxreborn.phdp.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import eu.hxreborn.phdp.PHDPApp
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig
import eu.hxreborn.phdp.ui.theme.isDark
import eu.hxreborn.phdp.util.HotReload
import eu.hxreborn.phdp.util.RootUtils
import io.github.libxposed.service.HotReloadResult
import kotlinx.coroutines.launch

private val LIGHT_NAV_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DARK_NAV_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

class MainActivity : ComponentActivity() {
    private val viewModel: SettingsViewModel by viewModels<SettingsViewModelImpl> { SettingsViewModelImpl.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var showRestartDialog by rememberSaveable { mutableStateOf(false) }
            var showResetDialog by rememberSaveable { mutableStateOf(false) }
            val haptics = LocalHapticFeedback.current

            val (darkThemeConfig, useDynamicColor) =
                when (uiState) {
                    is SettingsUiState.Loading -> {
                        DarkThemeConfig.FOLLOW_SYSTEM to true
                    }

                    is SettingsUiState.Success -> {
                        val prefs = (uiState as SettingsUiState.Success).prefs
                        prefs.darkThemeConfig to prefs.useDynamicColor
                    }
                }

            val darkTheme = darkThemeConfig.isDark()
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_NAV_SCRIM, DARK_NAV_SCRIM) { darkTheme },
                )
                onDispose {}
            }

            AppTheme(
                darkThemeConfig = darkThemeConfig,
                useDynamicColor = useDynamicColor,
            ) {
                val state = uiState
                if (state is SettingsUiState.Success) {
                    PunchHoleProgressContent(
                        viewModel = viewModel,
                        onMenuAction = { action ->
                            when (action) {
                                MenuAction.RestartSystemUI -> showRestartDialog = true
                                MenuAction.ReloadModule -> performHotReload()
                                MenuAction.Reset -> showResetDialog = true
                            }
                        },
                    )
                }

                if (showRestartDialog) {
                    AlertDialog(
                        onDismissRequest = { showRestartDialog = false },
                        title = { Text(stringResource(R.string.restart_systemui)) },
                        text = { Text(stringResource(R.string.restart_systemui_confirm)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showRestartDialog = false
                                    performRestart()
                                },
                            ) {
                                Text(stringResource(R.string.restart))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRestartDialog = false }) {
                                Text(stringResource(android.R.string.cancel))
                            }
                        },
                    )
                }

                if (showResetDialog) {
                    AlertDialog(
                        onDismissRequest = { showResetDialog = false },
                        title = { Text(stringResource(R.string.reset_defaults)) },
                        text = { Text(stringResource(R.string.reset_confirm)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showResetDialog = false
                                    viewModel.resetDefaults()
                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                    Toast.makeText(this@MainActivity, R.string.reset_done, Toast.LENGTH_SHORT).show()
                                },
                            ) {
                                Text(stringResource(R.string.reset))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showResetDialog = false }) {
                                Text(stringResource(android.R.string.cancel))
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        PHDPApp.from(this).prefs.save(Prefs.appVisible, true)
    }

    override fun onPause() {
        super.onPause()
        PHDPApp.from(this).prefs.save(Prefs.appVisible, false)
    }

    private fun performHotReload() {
        val service = PHDPApp.from(this).service
        if (service == null || !HotReload.isSupported(service)) {
            Toast.makeText(this, R.string.reload_unsupported, Toast.LENGTH_LONG).show()
            return
        }
        HotReload.reload(service) { result, status ->
            runOnUiThread {
                Toast.makeText(this, result, Toast.LENGTH_LONG).show()
                when (status) {
                    HotReloadResult.Status.SUCCEEDED -> {
                        ViewCompat.performHapticFeedback(window.decorView, HapticFeedbackConstantsCompat.CONFIRM)
                    }

                    HotReloadResult.Status.IN_PROGRESS -> {}

                    else -> {
                        ViewCompat.performHapticFeedback(window.decorView, HapticFeedbackConstantsCompat.REJECT)
                    }
                }
            }
        }
    }

    private fun performRestart() {
        lifecycleScope.launch {
            if (!RootUtils.isRootAvailable()) {
                Toast
                    .makeText(
                        this@MainActivity,
                        R.string.root_not_granted,
                        Toast.LENGTH_LONG,
                    ).show()
                return@launch
            }
            RootUtils.restartSystemUI().onFailure { e ->
                Toast
                    .makeText(
                        this@MainActivity,
                        getString(R.string.restart_failed_detail, e.message),
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }
    }
}
