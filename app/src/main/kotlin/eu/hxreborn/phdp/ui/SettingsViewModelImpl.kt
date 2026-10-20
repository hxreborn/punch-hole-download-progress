package eu.hxreborn.phdp.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import eu.hxreborn.phdp.BuildConfig
import eu.hxreborn.phdp.PHDPApp
import eu.hxreborn.phdp.backup.BackupMeta
import eu.hxreborn.phdp.prefs.PrefSpec
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.prefs.PrefsRepository
import eu.hxreborn.phdp.ui.state.ModuleStatus
import eu.hxreborn.phdp.ui.state.SystemUiHook
import eu.hxreborn.phdp.util.LauncherIconHelper
import io.github.libxposed.service.HookedTarget
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

internal const val MAX_BACKUP_BYTES = 512 * 1024

internal fun InputStream.readAtMost(limit: Int): ByteArray? {
    val collected = ByteArrayOutputStream()
    val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val count = read(chunk)
        if (count < 0) return collected.toByteArray()
        if (collected.size() + count > limit) return null
        collected.write(chunk, 0, count)
    }
}

class SettingsViewModelImpl(
    private val repository: PrefsRepository,
    private val applicationContext: Context,
) : SettingsViewModel() {
    private val launcherIconHidden = MutableStateFlow(!LauncherIconHelper.isLauncherIconVisible(applicationContext))

    override val backupEvent = MutableStateFlow<BackupEvent?>(null)

    private val simulating = AtomicBoolean(false)

    private val app = PHDPApp.from(applicationContext)

    override val moduleStatus = MutableStateFlow<ModuleStatus>(ModuleStatus.Checking)

    private val scopeRequestPending = AtomicBoolean(false)

    private val serviceListener =
        object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) = refreshModuleStatus()

            override fun onServiceDied(service: XposedService) {
                scopeRequestPending.set(false)
                refreshModuleStatus()
            }
        }

    init {
        app.addServiceListener(serviceListener)
    }

    override fun onCleared() {
        app.removeServiceListener(serviceListener)
    }

    override fun refreshModuleStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val service =
                app.service ?: run {
                    if (moduleStatus.value == ModuleStatus.Checking) delay(SERVICE_BIND_GRACE)
                    app.service
                }
            moduleStatus.value = service?.let(::readModuleStatus) ?: ModuleStatus.Inactive
        }
    }

    override fun requestSystemUiScope(onResult: (failure: String?) -> Unit) {
        val service = app.service ?: return
        if (!scopeRequestPending.compareAndSet(false, true)) return
        val listener =
            object : XposedService.OnScopeEventListener {
                override fun onScopeRequestApproved(approved: List<String>) {
                    scopeRequestPending.set(false)
                    refreshModuleStatus()
                    viewModelScope.launch { onResult(null) }
                }

                override fun onScopeRequestFailed(message: String) {
                    scopeRequestPending.set(false)
                    viewModelScope.launch { onResult(message) }
                }
            }
        runCatching { service.requestScope(listOf(SYSTEMUI), listener) }
            .onFailure { listener.onScopeRequestFailed(it.message ?: it.toString()) }
    }

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

    override fun exportSettings(destination: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val written =
                runCatching {
                    val payload = repository.exportSettings(currentBackupMeta())
                    applicationContext.contentResolver.openOutputStream(destination, "wt")?.use {
                        it.write(payload.json.toByteArray())
                    } ?: error("no output stream for $destination")
                    payload
                }
            backupEvent.value =
                written.fold(
                    onSuccess = { BackupEvent.Exported(it) },
                    onFailure = { BackupEvent.ExportFailed },
                )
        }
    }

    override fun importSettings(source: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val read =
                runCatching {
                    val stream =
                        applicationContext.contentResolver.openInputStream(source)
                            ?: error("no input stream for $source")
                    stream.use { it.readAtMost(MAX_BACKUP_BYTES) }
                }
            val bytes = read.getOrNull()
            val text =
                bytes?.let {
                    runCatching { it.decodeToString(throwOnInvalidSequence = true) }.getOrNull()
                }
            backupEvent.value =
                when {
                    read.isFailure -> BackupEvent.FileUnreadable
                    bytes == null -> BackupEvent.FileTooLarge
                    text == null -> BackupEvent.FileUnreadable
                    else -> BackupEvent.Imported(repository.importSettings(text))
                }
        }
    }

    override fun clearBackupEvent() {
        backupEvent.value = null
    }

    private fun currentBackupMeta() =
        BackupMeta(
            exportedAt = Instant.now().toString(),
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            deviceModel = Build.MODEL.orEmpty(),
            androidSdk = Build.VERSION.SDK_INT,
        )

    override fun simulateSuccess(onFinished: () -> Unit) {
        simulate(onFinished) {
            for (progress in 0..100 step 5) {
                savePref(Prefs.testProgress, progress)
                delay(100)
            }
        }
    }

    override fun simulateFailure(onFinished: () -> Unit) {
        simulate(onFinished) {
            for (progress in 0..60 step 10) {
                savePref(Prefs.testProgress, progress)
                delay(100)
            }
            savePref(Prefs.testError, true)
            delay(100)
        }
    }

    private fun simulate(
        onFinished: () -> Unit,
        steps: suspend () -> Unit,
    ) {
        if (!simulating.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                steps()
                onFinished()
            } finally {
                savePref(Prefs.testProgress, -1)
                savePref(Prefs.testError, false)
                simulating.set(false)
            }
        }
    }

    override fun clearDownloads() {
        savePref(Prefs.clearDownloadsTrigger, System.currentTimeMillis().toInt())
    }

    override fun setLauncherIconHidden(hidden: Boolean) {
        LauncherIconHelper.setLauncherIconVisible(applicationContext, !hidden)
        launcherIconHidden.value = hidden
    }

    companion object {
        private const val SYSTEMUI = "com.android.systemui"
        private val SERVICE_BIND_GRACE = 2.seconds

        private fun readModuleStatus(service: XposedService): ModuleStatus =
            runCatching {
                val systemUi =
                    when {
                        SYSTEMUI !in service.scope -> {
                            SystemUiHook.NOT_IN_SCOPE
                        }

                        service.apiVersion < XposedService.API_102 -> {
                            SystemUiHook.IN_SCOPE
                        }

                        else -> {
                            when (service.runningTargets.firstOrNull { it.processName == SYSTEMUI }?.state) {
                                null -> SystemUiHook.NOT_LOADED
                                HookedTarget.State.STALE -> SystemUiHook.OUTDATED
                                HookedTarget.State.FAILED -> SystemUiHook.FAILED
                                else -> SystemUiHook.RUNNING
                            }
                        }
                    }
                ModuleStatus.Active(service.frameworkName, service.frameworkVersion, service.apiVersion, systemUi)
            }.getOrElse { ModuleStatus.Inactive }

        val Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as PHDPApp
                    SettingsViewModelImpl(app.prefs, app)
                }
            }
    }
}
