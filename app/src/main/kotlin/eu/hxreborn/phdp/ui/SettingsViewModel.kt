package eu.hxreborn.phdp.ui

import androidx.lifecycle.ViewModel
import eu.hxreborn.phdp.prefs.PrefSpec
import eu.hxreborn.phdp.ui.state.PrefsState
import kotlinx.coroutines.flow.StateFlow

abstract class SettingsViewModel : ViewModel() {
    abstract val uiState: StateFlow<SettingsUiState>

    abstract fun <T : Any> savePref(
        pref: PrefSpec<T>,
        value: T,
    )

    abstract fun resetDefaults()

    abstract fun simulateSuccess()

    abstract fun simulateFailure()

    abstract fun clearDownloads()
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Success(
        val prefs: PrefsState,
    ) : SettingsUiState
}
