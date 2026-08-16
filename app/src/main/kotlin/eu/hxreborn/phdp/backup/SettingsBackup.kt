package eu.hxreborn.phdp.backup

import android.content.SharedPreferences
import eu.hxreborn.phdp.prefs.BoolPref
import eu.hxreborn.phdp.prefs.FloatPref
import eu.hxreborn.phdp.prefs.FoldScales
import eu.hxreborn.phdp.prefs.FoldScalesPref
import eu.hxreborn.phdp.prefs.IntPref
import eu.hxreborn.phdp.prefs.OffsetPx
import eu.hxreborn.phdp.prefs.PrefSpec
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.prefs.RingOffsetsPref
import eu.hxreborn.phdp.prefs.RotationOffsets
import eu.hxreborn.phdp.prefs.RotationOffsetsPref
import eu.hxreborn.phdp.prefs.ScaleXy
import eu.hxreborn.phdp.prefs.SetPref
import eu.hxreborn.phdp.prefs.StringPref
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.math.abs

const val BACKUP_FORMAT = "phdp-settings"
const val BACKUP_FORMAT_VERSION = 1

private const val MAX_TEXT_LENGTH = 4096
private const val MAX_SET_ENTRIES = 4096
private const val MAX_OFFSET_PX = 10000f
private const val MAX_DISPLAY_LENGTH = 72

data class BackupMeta(
    val formatVersion: Int = BACKUP_FORMAT_VERSION,
    val exportedAt: String = "",
    val appVersionName: String = "",
    val appVersionCode: Int = 0,
    val deviceModel: String = "",
    val androidSdk: Int = 0,
)

enum class FileProblem {
    NOT_JSON,
    NOT_A_BACKUP,
    FROM_NEWER_APP,
}

sealed interface RejectReason {
    data object NotABoolean : RejectReason

    data object NotAWholeNumber : RejectReason

    data object NotANumber : RejectReason

    data object NotText : RejectReason

    data object NotAList : RejectReason

    data object EntriesNotText : RejectReason

    data object MalformedOffsets : RejectReason

    data class TooLong(
        val limit: Int,
    ) : RejectReason

    data class TooManyEntries(
        val limit: Int,
    ) : RejectReason

    data class NotAllowed(
        val allowed: Set<String>,
    ) : RejectReason

    data class OffsetOutOfRange(
        val limit: Float,
    ) : RejectReason

    data class MalformedLegacyKeys(
        val keys: List<String>,
    ) : RejectReason
}

sealed interface AdaptNote {
    data class ClampedWhole(
        val from: Int,
        val min: Int,
        val max: Int,
    ) : AdaptNote

    data class ClampedNumber(
        val from: Float,
        val min: Float,
        val max: Float,
    ) : AdaptNote

    data class CollapsedDuplicates(
        val count: Int,
    ) : AdaptNote

    data class MigratedFromKeys(
        val keys: List<String>,
    ) : AdaptNote

    data class RenamedFromKey(
        val key: String,
    ) : AdaptNote
}

sealed interface FailureReason {
    data object NotSaved : FailureReason

    data object NotSavedOrRestored : FailureReason

    data class WriteFailed(
        val detail: String,
    ) : FailureReason
}

sealed interface RestoreOutcome {
    data object Applied : RestoreOutcome

    data object Unchanged : RestoreOutcome

    data class Migrated(
        val notes: List<AdaptNote>,
    ) : RestoreOutcome

    data object Unknown : RestoreOutcome

    data object Deprecated : RestoreOutcome

    data object Excluded : RestoreOutcome

    data class Rejected(
        val reason: RejectReason,
    ) : RestoreOutcome

    data class Failed(
        val reason: FailureReason,
    ) : RestoreOutcome
}

data class RestoreEntry(
    val key: String,
    val outcome: RestoreOutcome,
    val previous: String? = null,
    val next: String? = null,
)

data class ExportEntry(
    val key: String,
    val value: String,
)

data class ExportResult(
    val json: String,
    val entries: List<ExportEntry>,
    val excluded: Set<String> = Prefs.excludedFromBackup,
)

class PendingWrite(
    val spec: PrefSpec<*>,
    val value: Any,
)

data class RestoreReport(
    val meta: BackupMeta,
    val entries: List<RestoreEntry>,
    val excludedByPolicy: Set<String> = Prefs.excludedFromBackup,
) {
    private inline fun <reified T : RestoreOutcome> of() = entries.filter { it.outcome is T }

    val applied get() = of<RestoreOutcome.Applied>()
    val unchanged get() = of<RestoreOutcome.Unchanged>()
    val migrated get() = of<RestoreOutcome.Migrated>()
    val unknown get() = of<RestoreOutcome.Unknown>()
    val deprecated get() = of<RestoreOutcome.Deprecated>()
    val excluded get() = of<RestoreOutcome.Excluded>()
    val rejected get() = of<RestoreOutcome.Rejected>()
    val failed get() = of<RestoreOutcome.Failed>()

    val restoredCount get() = applied.size + migrated.size
}

sealed interface ParsedBackup {
    data class Usable(
        val meta: BackupMeta,
        val entries: List<RestoreEntry>,
        val writes: List<PendingWrite>,
    ) : ParsedBackup

    data class Unusable(
        val problem: FileProblem,
    ) : ParsedBackup
}

private sealed interface Decoded {
    data class Ok(
        val value: Any,
        val normalized: JsonElement,
        val note: AdaptNote? = null,
    ) : Decoded

    data class Err(
        val reason: RejectReason,
    ) : Decoded
}

private class LegacyOffsetPair(
    val x: String,
    val y: String,
    val target: String,
)

private class Migration(
    val settings: Map<String, JsonElement>,
    val notes: Map<String, AdaptNote>,
)

object SettingsBackup {
    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = false
        }

    val registry: Map<String, PrefSpec<*>> by lazy {
        Prefs.javaClass.declaredFields
            .filter { PrefSpec::class.java.isAssignableFrom(it.type) }
            .mapNotNull { field ->
                runCatching {
                    field.isAccessible = true
                    field.get(Prefs) as PrefSpec<*>
                }.getOrNull()
            }.associateBy { it.key }
    }

    private val backupKeys: List<PrefSpec<*>> by lazy {
        registry.values
            .filter { it.key !in Prefs.excludedFromBackup }
            .sortedBy { it.key }
    }

    private val removedKeys = setOf("completion_pulse_enabled")

    private val renamedKeys = emptyMap<String, String>()

    private val legacyOffsetPairs =
        listOf(
            LegacyOffsetPair(
                "percent_text_offset_x",
                "percent_text_offset_y",
                "percent_text_offsets_by_rotation",
            ),
            LegacyOffsetPair(
                "filename_text_offset_x",
                "filename_text_offset_y",
                "filename_text_offsets_by_rotation",
            ),
            LegacyOffsetPair(
                "badge_offset_x",
                "badge_offset_y",
                "badge_offsets_by_rotation",
            ),
        )

    fun export(
        prefs: SharedPreferences,
        meta: BackupMeta,
    ): ExportResult {
        val values = backupKeys.associateWith { encodeCurrent(it, prefs) }
        val document =
            buildJsonObject {
                put("format", BACKUP_FORMAT)
                put("formatVersion", BACKUP_FORMAT_VERSION)
                put("exportedAt", meta.exportedAt)
                putJsonObject("app") {
                    put("versionName", meta.appVersionName)
                    put("versionCode", meta.appVersionCode)
                }
                putJsonObject("device") {
                    put("model", meta.deviceModel)
                    put("androidSdk", meta.androidSdk)
                }
                putJsonObject("settings") {
                    values.forEach { (spec, value) -> put(spec.key, value) }
                }
            }
        return ExportResult(
            json = json.encodeToString(JsonObject.serializer(), document),
            entries = values.map { (spec, value) -> ExportEntry(spec.key, value.display()) },
        )
    }

    fun parse(
        raw: String,
        prefs: SharedPreferences,
    ): ParsedBackup {
        val root =
            runCatching { json.parseToJsonElement(raw) }.getOrNull() as? JsonObject
                ?: return ParsedBackup.Unusable(FileProblem.NOT_JSON)

        if (root.string("format") != BACKUP_FORMAT) {
            return ParsedBackup.Unusable(FileProblem.NOT_A_BACKUP)
        }
        val version =
            root.int("formatVersion")?.takeIf { it >= 1 }
                ?: return ParsedBackup.Unusable(FileProblem.NOT_A_BACKUP)
        if (version > BACKUP_FORMAT_VERSION) {
            return ParsedBackup.Unusable(FileProblem.FROM_NEWER_APP)
        }
        val settings =
            root["settings"] as? JsonObject
                ?: return ParsedBackup.Unusable(FileProblem.NOT_A_BACKUP)

        val app = root["app"] as? JsonObject
        val device = root["device"] as? JsonObject
        val meta =
            BackupMeta(
                formatVersion = version,
                exportedAt = root.string("exportedAt").orEmpty(),
                appVersionName = app?.string("versionName").orEmpty(),
                appVersionCode = app?.int("versionCode") ?: 0,
                deviceModel = device?.string("model").orEmpty(),
                androidSdk = device?.int("androidSdk") ?: 0,
            )

        val entries = mutableListOf<RestoreEntry>()
        val writes = mutableListOf<PendingWrite>()
        val migration = migrate(settings, entries)

        for ((key, element) in migration.settings) {
            val note = migration.notes[key]
            if (key in Prefs.excludedFromBackup) {
                entries += RestoreEntry(key, RestoreOutcome.Excluded, next = element.display())
                continue
            }
            val spec = registry[key]
            if (spec == null) {
                entries += RestoreEntry(key, RestoreOutcome.Unknown, next = element.display())
                continue
            }
            val current = encodeCurrent(spec, prefs)
            when (val decoded = decode(spec, element)) {
                is Decoded.Err -> {
                    entries +=
                        RestoreEntry(
                            key,
                            RestoreOutcome.Rejected(decoded.reason),
                            previous = current.display(),
                            next = element.display(),
                        )
                }

                is Decoded.Ok -> {
                    if (decoded.normalized == current) {
                        entries +=
                            RestoreEntry(
                                key,
                                RestoreOutcome.Unchanged,
                                previous = current.display(),
                                next = current.display(),
                            )
                    } else {
                        writes += PendingWrite(spec, decoded.value)
                        val combined = listOfNotNull(note, decoded.note)
                        entries +=
                            RestoreEntry(
                                key,
                                combined
                                    .takeIf { it.isNotEmpty() }
                                    ?.let(RestoreOutcome::Migrated)
                                    ?: RestoreOutcome.Applied,
                                previous = current.display(),
                                next = decoded.normalized.display(),
                            )
                    }
                }
            }
        }
        return ParsedBackup.Usable(meta, entries.sortedBy { it.key }, writes)
    }

    private fun migrate(
        settings: JsonObject,
        entries: MutableList<RestoreEntry>,
    ): Migration {
        val notes = mutableMapOf<String, AdaptNote>()
        val effective = LinkedHashMap<String, JsonElement>()

        for ((key, element) in settings) {
            when {
                key in removedKeys -> {
                    entries += RestoreEntry(key, RestoreOutcome.Deprecated)
                }

                renamedKeys.containsKey(key) -> {
                    val target = renamedKeys.getValue(key)
                    if (!settings.containsKey(target)) {
                        effective[target] = element
                        notes[target] = AdaptNote.RenamedFromKey(key)
                    }
                }

                else -> {
                    effective[key] = element
                }
            }
        }

        for (pair in legacyOffsetPairs) {
            val rawX = settings[pair.x]
            val rawY = settings[pair.y]
            if (rawX == null && rawY == null) continue
            effective.remove(pair.x)
            effective.remove(pair.y)
            if (effective.containsKey(pair.target)) continue
            val x = rawX?.asNumberOrBool()?.floatOrNull
            val y = rawY?.asNumberOrBool()?.floatOrNull
            if ((rawX != null && x == null) || (rawY != null && y == null)) {
                entries +=
                    RestoreEntry(
                        pair.target,
                        RestoreOutcome.Rejected(
                            RejectReason.MalformedLegacyKeys(listOf(pair.x, pair.y)),
                        ),
                        next = listOfNotNull(rawX, rawY).joinToString(", ") { it.display() },
                    )
                continue
            }
            val base = OffsetPx(x ?: 0f, y ?: 0f)
            if (!base.x.isFinite() || !base.y.isFinite()) {
                entries +=
                    RestoreEntry(
                        pair.target,
                        RestoreOutcome.Rejected(
                            RejectReason.MalformedLegacyKeys(listOf(pair.x, pair.y)),
                        ),
                        next = listOfNotNull(rawX, rawY).joinToString(", ") { it.display() },
                    )
                continue
            }
            effective[pair.target] =
                JsonPrimitive(RotationOffsets(r0 = base, r90 = base).serialize())
            notes[pair.target] = AdaptNote.MigratedFromKeys(listOf(pair.x, pair.y))
        }
        return Migration(effective, notes)
    }

    private fun decode(
        spec: PrefSpec<*>,
        element: JsonElement,
    ): Decoded =
        when (spec) {
            is BoolPref -> {
                val value = element.asNumberOrBool()?.booleanOrNull
                if (value == null) {
                    Decoded.Err(RejectReason.NotABoolean)
                } else {
                    Decoded.Ok(value, JsonPrimitive(value))
                }
            }

            is IntPref -> {
                val value = element.asNumberOrBool()?.intOrNull
                when {
                    value == null -> {
                        Decoded.Err(RejectReason.NotAWholeNumber)
                    }

                    spec.range == null -> {
                        Decoded.Ok(value, JsonPrimitive(value))
                    }

                    value in spec.range -> {
                        Decoded.Ok(value, JsonPrimitive(value))
                    }

                    else -> {
                        val clamped = value.coerceIn(spec.range)
                        Decoded.Ok(
                            clamped,
                            JsonPrimitive(clamped),
                            AdaptNote.ClampedWhole(value, spec.range.first, spec.range.last),
                        )
                    }
                }
            }

            is FloatPref -> {
                val value = element.asNumberOrBool()?.floatOrNull
                when {
                    value == null || !value.isFinite() -> {
                        Decoded.Err(RejectReason.NotANumber)
                    }

                    spec.range == null -> {
                        Decoded.Ok(value, JsonPrimitive(value))
                    }

                    value in spec.range -> {
                        Decoded.Ok(value, JsonPrimitive(value))
                    }

                    else -> {
                        val clamped = value.coerceIn(spec.range)
                        Decoded.Ok(
                            clamped,
                            JsonPrimitive(clamped),
                            AdaptNote.ClampedNumber(
                                value,
                                spec.range.start,
                                spec.range.endInclusive,
                            ),
                        )
                    }
                }
            }

            is StringPref -> {
                val value = element.asText()
                when {
                    value == null -> {
                        Decoded.Err(RejectReason.NotText)
                    }

                    value.length > MAX_TEXT_LENGTH -> {
                        Decoded.Err(RejectReason.TooLong(MAX_TEXT_LENGTH))
                    }

                    spec.values != null && value !in spec.values -> {
                        Decoded.Err(RejectReason.NotAllowed(spec.values))
                    }

                    else -> {
                        Decoded.Ok(value, JsonPrimitive(value))
                    }
                }
            }

            is SetPref -> {
                val array = element as? JsonArray
                val texts = array?.map { it.asText() }
                when {
                    array == null -> {
                        Decoded.Err(RejectReason.NotAList)
                    }

                    array.size > MAX_SET_ENTRIES -> {
                        Decoded.Err(RejectReason.TooManyEntries(MAX_SET_ENTRIES))
                    }

                    texts!!.any { it == null } -> {
                        Decoded.Err(RejectReason.EntriesNotText)
                    }

                    texts.any { it!!.length > MAX_TEXT_LENGTH } -> {
                        Decoded.Err(RejectReason.TooLong(MAX_TEXT_LENGTH))
                    }

                    else -> {
                        val value = texts.filterNotNull().toSet()
                        val duplicates = texts.size - value.size
                        Decoded.Ok(
                            value,
                            encodeSet(value),
                            AdaptNote.CollapsedDuplicates(duplicates).takeIf { duplicates > 0 },
                        )
                    }
                }
            }

            is RotationOffsetsPref -> {
                decodeOffsets(element)
            }

            is RingOffsetsPref -> {
                decodeOffsets(element)
            }

            is FoldScalesPref -> {
                decodeFoldScales(element)
            }
        }

    private fun decodeOffsets(element: JsonElement): Decoded {
        val text =
            (element as? JsonPrimitive)?.takeIf { it.isString }?.content
                ?: return Decoded.Err(RejectReason.NotText)
        val slots = text.split("|")
        if (slots.size != 4 && slots.size != 8) return Decoded.Err(RejectReason.MalformedOffsets)
        val points =
            slots.map { slot ->
                val parts = slot.split(",")
                if (parts.size != 2) return Decoded.Err(RejectReason.MalformedOffsets)
                val x =
                    parts[0].trim().toFloatOrNull()
                        ?: return Decoded.Err(RejectReason.MalformedOffsets)
                val y =
                    parts[1].trim().toFloatOrNull()
                        ?: return Decoded.Err(RejectReason.MalformedOffsets)
                if (!x.isFinite() ||
                    !y.isFinite()
                ) {
                    return Decoded.Err(RejectReason.MalformedOffsets)
                }
                if (abs(x) > MAX_OFFSET_PX || abs(y) > MAX_OFFSET_PX) {
                    return Decoded.Err(RejectReason.OffsetOutOfRange(MAX_OFFSET_PX))
                }
                OffsetPx(x, y)
            }
        val value =
            if (points.size == 8) {
                RotationOffsets(
                    points[0],
                    points[1],
                    points[2],
                    points[3],
                    points[4],
                    points[5],
                    points[6],
                    points[7],
                )
            } else {
                RotationOffsets(points[0], points[1], points[2], points[3])
            }
        return Decoded.Ok(value, JsonPrimitive(value.serialize()))
    }

    private fun decodeFoldScales(element: JsonElement): Decoded {
        val text =
            (element as? JsonPrimitive)?.takeIf { it.isString }?.content
                ?: return Decoded.Err(RejectReason.NotText)
        val slots = text.split("|")
        if (slots.size != 1 && slots.size != 2) return Decoded.Err(RejectReason.MalformedOffsets)
        val points =
            slots.map { slot ->
                val parts = slot.split(",")
                if (parts.size != 2) return Decoded.Err(RejectReason.MalformedOffsets)
                val x =
                    parts[0].trim().toFloatOrNull()
                        ?: return Decoded.Err(RejectReason.MalformedOffsets)
                val y =
                    parts[1].trim().toFloatOrNull()
                        ?: return Decoded.Err(RejectReason.MalformedOffsets)
                if (!x.isFinite() || !y.isFinite()) {
                    return Decoded.Err(RejectReason.MalformedOffsets)
                }
                ScaleXy(x, y)
            }
        val value =
            if (points.size == 2) {
                FoldScales(closed = points[0], open = points[1])
            } else {
                FoldScales(closed = points[0], open = points[0])
            }
        return Decoded.Ok(value, JsonPrimitive(value.serialize()))
    }

    private fun encodeCurrent(
        spec: PrefSpec<*>,
        prefs: SharedPreferences,
    ): JsonElement {
        val value = runCatching { spec.read(prefs) }.getOrElse { spec.default }
        return when (spec) {
            is BoolPref -> {
                JsonPrimitive(value as Boolean)
            }

            is IntPref -> {
                JsonPrimitive(value as Int)
            }

            is FloatPref -> {
                JsonPrimitive((value as Float).takeIf { it.isFinite() } ?: spec.default)
            }

            is StringPref -> {
                val text = value as String
                JsonPrimitive(
                    text.takeIf { spec.values == null || it in spec.values } ?: spec.default,
                )
            }

            is SetPref -> {
                @Suppress("UNCHECKED_CAST")
                encodeSet(value as Set<String>)
            }

            is RotationOffsetsPref -> {
                JsonPrimitive((value as RotationOffsets).serialize())
            }

            is RingOffsetsPref -> {
                JsonPrimitive((value as RotationOffsets).serialize())
            }

            is FoldScalesPref -> {
                JsonPrimitive((value as FoldScales).serialize())
            }
        }
    }

    private fun encodeSet(value: Set<String>): JsonArray =
        buildJsonArray {
            value.sorted().forEach { add(JsonPrimitive(it)) }
        }

    private fun JsonElement.display(): String {
        val text =
            when (this) {
                is JsonArray -> joinToString(", ", "[", "]") { it.display() }
                is JsonPrimitive -> if (isString) content else toString()
                else -> toString()
            }
        return if (text.length <= MAX_DISPLAY_LENGTH) text else text.take(MAX_DISPLAY_LENGTH) + "…"
    }

    private fun JsonElement.asText(): String? =
        (this as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

    private fun JsonElement.asNumberOrBool(): JsonPrimitive? =
        (this as? JsonPrimitive)?.takeIf { !it.isString && it != JsonNull }

    private fun JsonObject.string(key: String): String? = this[key]?.asText()?.take(MAX_TEXT_LENGTH)

    private fun JsonObject.int(key: String): Int? = this[key]?.asNumberOrBool()?.intOrNull
}
