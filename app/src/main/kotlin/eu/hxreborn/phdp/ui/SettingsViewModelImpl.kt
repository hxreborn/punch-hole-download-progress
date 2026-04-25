package eu.hxreborn.phdp.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eu.hxreborn.phdp.prefs.PrefSpec
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.prefs.PrefsRepository
import eu.hxreborn.phdp.util.LauncherIconHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class SettingsViewModelImpl(
    private val repository: PrefsRepository,
    private val applicationContext: Context,
) : SettingsViewModel() {
    private val launcherIconHidden =
        MutableStateFlow(!LauncherIconHelper.isLauncherIconVisible(applicationContext))

    override val uiState: StateFlow<SettingsUiState> =
        combine(repository.state, launcherIconHidden) { prefs, hidden ->
            SettingsUiState.Success(prefs, hidden)
        }.stateIn(
            scope = viewModelScope,
            started = WhileSubscribed(5.seconds.inWholeMilliseconds),
            initialValue = SettingsUiState.Loading,
        )

    override fun <T : Any> savePref(
        pref: PrefSpec<T>,
        value: T,
    ) {
        repository.save(pref, value)
    }

    override fun resetDefaults() {
        repository.resetDefaults()
    }

    override fun simulateSuccess() {
        viewModelScope.launch {
            for (progress in 0..100 step 5) {
                savePref(Prefs.testProgress, progress)
                delay(100)
            }
            savePref(Prefs.testProgress, -1)
        }
    }

    override fun simulateFailure() {
        viewModelScope.launch {
            for (progress in 0..60 step 10) {
                savePref(Prefs.testProgress, progress)
                delay(100)
            }
            savePref(Prefs.testError, true)
            delay(100)
            savePref(Prefs.testProgress, -1)
            savePref(Prefs.testError, false)
        }
    }

    override fun clearDownloads() {
        savePref(Prefs.clearDownloadsTrigger, System.currentTimeMillis().toInt())
    }

    override fun setLauncherIconHidden(hidden: Boolean) {
        LauncherIconHelper.setLauncherIconVisible(applicationContext, !hidden)
        launcherIconHidden.value = hidden
    }
}

class SettingsViewModelFactory(
    private val repository: PrefsRepository,
    private val applicationContext: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModelImpl(repository, applicationContext) as T
}
