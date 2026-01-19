package eu.hxreborn.phdp.util

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootUtils {
    private val commands =
        listOf(
            "killall com.android.systemui",
            "pkill -f com.android.systemui",
            "kill -9 $(pidof com.android.systemui)",
        )

    suspend fun isRootAvailable(): Boolean =
        withContext(Dispatchers.IO) {
            Shell.isAppGrantedRoot() == true || Shell.cmd("id").exec().isSuccess
        }

    suspend fun restartSystemUI(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                var lastErr: String? = null
                for (cmd in commands) {
                    val res = Shell.cmd(cmd).exec()
                    if (res.isSuccess) return@runCatching
                    lastErr = (res.err + res.out).joinToString().ifBlank { "exit ${res.code}" }
                }
                error(lastErr ?: "All methods failed")
            }
        }
}
