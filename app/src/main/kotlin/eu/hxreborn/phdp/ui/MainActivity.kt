package eu.hxreborn.phdp.ui

import android.content.Context
import android.content.SharedPreferences
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
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import eu.hxreborn.phdp.PHDPApp
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.prefs.PrefsRepository
import eu.hxreborn.phdp.prefs.PrefsRepositoryImpl
import eu.hxreborn.phdp.ui.theme.AppTheme
import eu.hxreborn.phdp.ui.theme.DarkThemeConfig
import eu.hxreborn.phdp.util.RootUtils
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity :
    ComponentActivity(),
    XposedServiceHelper.OnServiceListener {
    private lateinit var prefs: SharedPreferences
    private lateinit var repository: PrefsRepository
    private lateinit var viewModel: SettingsViewModel

    private var xposedService: XposedService? = null
    private var remotePrefs: SharedPreferences? = null

    private var showRestartDialog by mutableStateOf(false)
    private var showResetDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(Prefs.GROUP, Context.MODE_PRIVATE)
        repository = PrefsRepositoryImpl(prefs) { remotePrefs }
        viewModel = ViewModelProvider(this, SettingsViewModelFactory(repository))[SettingsViewModel::class.java]

        PHDPApp.addServiceListener(this)

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
                        prefsState = state.prefs,
                        onSavePrefs = viewModel::save,
                        onMenuAction = { action ->
                            when (action) {
                                MenuAction.RestartSystemUI -> showRestartDialog = true
                                MenuAction.Reset -> showResetDialog = true
                            }
                        },
                        onTestSuccess = ::simulateSuccess,
                        onTestFailure = ::simulateFailure,
                        onClearDownloads = ::clearDownloads,
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

    private fun simulateSuccess() {
        lifecycleScope.launch {
            for (progress in 0..100 step 5) {
                viewModel.save(Prefs.testProgress.key, progress)
                delay(100)
            }
            viewModel.save(Prefs.testProgress.key, -1)
        }
    }

    private fun clearDownloads() {
        viewModel.save(Prefs.clearDownloadsTrigger.key, System.currentTimeMillis())
        Toast.makeText(this, R.string.clear_downloads_done, Toast.LENGTH_SHORT).show()
    }

    private fun simulateFailure() {
        lifecycleScope.launch {
            for (progress in 0..60 step 10) {
                viewModel.save(Prefs.testProgress.key, progress)
                delay(100)
            }
            viewModel.save(Prefs.testError.key, true)
            delay(100)
            viewModel.save(Prefs.testProgress.key, -1)
            viewModel.save(Prefs.testError.key, false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PHDPApp.removeServiceListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        xposedService = service
        remotePrefs = service.getRemotePreferences(Prefs.GROUP)
    }

    override fun onServiceDied(service: XposedService) {
        xposedService = null
        remotePrefs = null
    }

    override fun onResume() {
        super.onResume()
        remotePrefs?.edit(commit = true) { putBoolean(Prefs.appVisible.key, true) }
    }

    override fun onPause() {
        super.onPause()
        remotePrefs?.edit(commit = true) { putBoolean(Prefs.appVisible.key, false) }
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
            RootUtils
                .restartSystemUI()
                .onFailure { e ->
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
        @JvmStatic
        fun isXposedEnabled(): Boolean = false
    }
}
