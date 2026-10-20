package eu.hxreborn.phdp.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import eu.hxreborn.phdp.backup.ExportResult
import eu.hxreborn.phdp.prefs.ImportResult
import eu.hxreborn.phdp.prefs.PrefSpec
import eu.hxreborn.phdp.ui.state.AppPrefs
import eu.hxreborn.phdp.ui.state.ModuleStatus
import kotlinx.coroutines.flow.StateFlow

abstract class SettingsViewModel : ViewModel() {
    abstract val uiState: StateFlow<SettingsUiState>

    abstract val backupEvent: StateFlow<BackupEvent?>

    abstract val moduleStatus: StateFlow<ModuleStatus>

    abstract fun refreshModuleStatus()

    abstract fun requestSystemUiScope(onResult: (failure: String?) -> Unit)

    abstract fun <T : Any> savePref(
        pref: PrefSpec<T>,
        value: T,
    )

    abstract fun resetDefaults()

    abstract fun exportSettings(destination: Uri)

    abstract fun importSettings(source: Uri)

    abstract fun clearBackupEvent()

    abstract fun simulateSuccess(onFinished: () -> Unit = {})

    abstract fun simulateFailure(onFinished: () -> Unit = {})

    abstract fun clearDownloads()

    abstract fun setLauncherIconHidden(hidden: Boolean)
}

sealed interface BackupEvent {
    data class Exported(
        val result: ExportResult,
    ) : BackupEvent

    data object ExportFailed : BackupEvent

    data object FileUnreadable : BackupEvent

    data object FileTooLarge : BackupEvent

    data class Imported(
        val result: ImportResult,
    ) : BackupEvent
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Success(
        val prefs: AppPrefs,
        val isLauncherIconHidden: Boolean = false,
    ) : SettingsUiState
}
