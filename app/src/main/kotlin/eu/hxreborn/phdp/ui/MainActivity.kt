package eu.hxreborn.phdp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import eu.hxreborn.phdp.PHDPApp
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig
import eu.hxreborn.phdp.util.RootUtils
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: SettingsViewModel

    private var showRestartDialog by mutableStateOf(false)
    private var showResetDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val repository = PHDPApp.from(this).prefs
        viewModel =
            ViewModelProvider(
                this,
                SettingsViewModelFactory(repository, applicationContext),
            )[SettingsViewModelImpl::class.java]

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

    companion object {
        // Xposed hook target. Module replaces return value at runtime to signal active state
        @JvmStatic
        @Suppress("SameReturnValue")
        fun isXposedEnabled(): Boolean = false
    }
}
