package eu.hxreborn.phdp.about

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val DIFFSTAT_BLOCKS = 5

private const val CONTRIBUTORS_FILE = "contributors.json"

data class ContributorEntry(
    val login: String,
    val owner: Boolean,
    val added: Int,
    val removed: Int,
    val url: String,
    val avatarPath: String,
)

data class Contributor(
    val login: String,
    val owner: Boolean,
    val added: Int,
    val removed: Int,
    val url: String,
    val avatar: ImageBitmap?,
)

fun parseContributors(text: String): List<ContributorEntry> =
    (Json.parseToJsonElement(text) as JsonArray).map { element ->
        val entry = element.jsonObject
        ContributorEntry(
            login = entry.getValue("login").jsonPrimitive.content,
            owner = entry["owner"]?.jsonPrimitive?.boolean ?: false,
            added = entry.getValue("added").jsonPrimitive.int,
            removed = entry.getValue("removed").jsonPrimitive.int,
            url = entry.getValue("url").jsonPrimitive.content,
            avatarPath = entry.getValue("avatar").jsonPrimitive.content,
        )
    }

fun greenBlocks(
    added: Int,
    removed: Int,
): Int = blocksOf(added, added + removed)

fun redBlocks(
    added: Int,
    removed: Int,
): Int = blocksOf(removed, added + removed)

private fun blocksOf(
    part: Int,
    total: Int,
): Int = if (total == 0) 0 else (DIFFSTAT_BLOCKS * part.toLong() / total).toInt()

private fun AssetManager.decodeAvatar(path: String): ImageBitmap? =
    runCatching { open(path).use { BitmapFactory.decodeStream(it) }?.asImageBitmap() }.getOrNull()

fun loadContributors(assets: AssetManager): List<Contributor> {
    val entries =
        runCatching {
            assets.open(CONTRIBUTORS_FILE).bufferedReader().use { parseContributors(it.readText()) }
        }.getOrDefault(emptyList())
    return entries.map { entry ->
        Contributor(
            login = entry.login,
            owner = entry.owner,
            added = entry.added,
            removed = entry.removed,
            url = entry.url,
            avatar = assets.decodeAvatar(entry.avatarPath),
        )
    }
}
