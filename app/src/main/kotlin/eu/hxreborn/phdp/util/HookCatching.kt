package eu.hxreborn.phdp.util

internal inline fun <T> runHookCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(e)
    } catch (e: LinkageError) {
        Result.failure(e)
    }
