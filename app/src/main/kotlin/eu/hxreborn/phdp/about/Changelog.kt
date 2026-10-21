package eu.hxreborn.phdp.about

import android.content.res.AssetManager
import java.time.LocalDate

data class Release(
    val versionCode: Int,
    val versionName: String,
    val entries: List<String>,
    val releasedOn: LocalDate? = null,
)

private const val CHANGELOG_DIR = "changelogs"
private const val RELEASE_DATES = "release-dates.txt"

private val bulletPrefix = Regex("""^[-*•]\s+""")

private val legacyVersionNames =
    mapOf(
        10129 to "1.0.0",
        10136 to "1.1.0",
        10137 to "1.2.0",
        10150 to "1.3.0",
        10154 to "1.4.0",
        10155 to "1.4.1",
        10156 to "1.4.2",
        10167 to "1.5.0",
        10174 to "1.6.0",
        10184 to "1.7.0",
        10192 to "1.8.0",
        10195 to "1.8.1",
        10201 to "1.8.2",
        10207 to "1.9.0",
        10219 to "1.10.0",
        20228 to "2.0.0",
    )

fun versionNameOf(code: Int): String =
    legacyVersionNames[code]
        ?: if (code >= 100_000) {
            "${code / 100_000}.${code % 100_000 / 1000}.${code % 1000}"
        } else {
            "${code / 10_000}.${code % 10_000 / 1000}.${code % 1000}"
        }

fun parseEntries(text: String): List<String> =
    text
        .lines()
        .map { it.trim().replace(bulletPrefix, "").trim() }
        .filter { it.isNotEmpty() }

fun parseReleaseDates(text: String): Map<String, LocalDate> =
    text
        .lines()
        .mapNotNull { line ->
            val (tag, date) =
                line.trim().split(' ').takeIf { it.size == 2 }
                    ?: return@mapNotNull null
            val parsed = runCatching { LocalDate.parse(date) }.getOrNull() ?: return@mapNotNull null
            tag.removePrefix("v") to parsed
        }.toMap()

fun loadReleases(assets: AssetManager): List<Release> {
    val dates =
        runCatching {
            assets
                .open(
                    RELEASE_DATES,
                ).bufferedReader()
                .use { parseReleaseDates(it.readText()) }
        }.getOrDefault(emptyMap())
    return assets
        .list(CHANGELOG_DIR)
        .orEmpty()
        .mapNotNull { file ->
            val code = file.removeSuffix(".txt").toIntOrNull() ?: return@mapNotNull null
            val text = assets.open("$CHANGELOG_DIR/$file").bufferedReader().use { it.readText() }
            val name = versionNameOf(code)
            Release(code, name, parseEntries(text), dates[name])
        }.sortedByDescending { it.versionCode }
}
