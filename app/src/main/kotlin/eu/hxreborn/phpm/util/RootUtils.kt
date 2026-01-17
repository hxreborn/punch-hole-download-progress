package eu.hxreborn.phpm.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object RootUtils {
    suspend fun isRootAvailable(): Boolean = execAsRoot("id", 5.seconds).getOrNull()?.contains("uid=0") == true

    suspend fun restartSystemUI(): Result<Unit> =
        withContext(Dispatchers.IO) {
            val commands =
                listOf(
                    "killall com.android.systemui",
                    "pkill -f com.android.systemui",
                    "kill -9 $(pidof com.android.systemui)",
                )
            var lastError: Throwable? = null
            for (cmd in commands) {
                execAsRoot(cmd, 10.seconds)
                    .onSuccess { return@withContext Result.success(Unit) }
                    .onFailure { lastError = it }
            }
            Result.failure(lastError ?: Exception("All methods failed"))
        }

    private suspend fun execAsRoot(
        cmd: String,
        timeout: Duration,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                coroutineScope {
                    val process =
                        runCatching {
                            ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start()
                        }.getOrElse { error("Root access not available") }

                    val outputDeferred =
                        async {
                            process.inputStream.bufferedReader().use { it.readText().trim() }
                        }

                    if (!runInterruptible { process.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS) }) {
                        process.destroyForcibly()
                        outputDeferred.cancel()
                        error("Root prompt timed out")
                    }

                    val output = outputDeferred.await()
                    when {
                        process.exitValue() == 0 -> output
                        process.exitValue() == 1 && output.isBlank() -> error("Root permission denied")
                        else -> error("Command failed: $output")
                    }
                }
            }
        }
}
