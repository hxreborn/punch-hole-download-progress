package eu.hxreborn.phdp.backup

import eu.hxreborn.phdp.FakePrefs
import eu.hxreborn.phdp.prefs.Prefs
import eu.hxreborn.phdp.prefs.RotationOffsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupTest {
    private fun backup(
        settings: String,
        format: String = BACKUP_FORMAT,
        version: Int = BACKUP_FORMAT_VERSION,
    ) = """
        {
          "format": "$format",
          "formatVersion": $version,
          "exportedAt": "2026-08-14T10:00:00Z",
          "app": { "versionName": "2.6.0", "versionCode": 26000 },
          "device": { "model": "RMX2170", "androidSdk": 36 },
          "settings": { $settings }
        }
        """.trimIndent()

    private fun parse(
        raw: String,
        current: FakePrefs = FakePrefs(),
    ) = SettingsBackup.parse(raw, current)

    private fun usable(
        raw: String,
        current: FakePrefs = FakePrefs(),
    ): ParsedBackup.Usable {
        val parsed = parse(raw, current)
        assertTrue("expected a usable backup but got $parsed", parsed is ParsedBackup.Usable)
        return parsed as ParsedBackup.Usable
    }

    private fun parsedValue(
        parsed: ParsedBackup.Usable,
        key: String,
    ) = parsed.writes.first { it.spec.key == key }.value

    private fun outcomeOf(
        parsed: ParsedBackup.Usable,
        key: String,
    ) = parsed.entries.firstOrNull { it.key == key }?.outcome

    @Test
    fun `registry discovers every declared pref`() {
        assertTrue(SettingsBackup.registry.size > 100)
        assertNotNull(SettingsBackup.registry["stroke_width"])
        assertNotNull(SettingsBackup.registry["selected_packages"])
        assertNotNull(SettingsBackup.registry["ring_offsets_by_rotation"])
    }

    @Test
    fun `valid current backup round trips`() {
        val meta = BackupMeta(exportedAt = "now", appVersionName = "2.6.0", appVersionCode = 26000)
        val current = FakePrefs(mapOf("opacity" to 55, "clockwise" to false))
        val exported = SettingsBackup.export(current, meta).json

        val reimported = usable(exported, current)
        assertTrue(reimported.writes.isEmpty())
        assertEquals(RestoreOutcome.Unchanged, outcomeOf(reimported, "opacity"))

        val ontoDefaults = usable(exported, FakePrefs())
        assertEquals(RestoreOutcome.Applied, outcomeOf(ontoDefaults, "opacity"))
        assertTrue(ontoDefaults.writes.any { it.spec.key == "opacity" && it.value == 55 })
    }

    @Test
    fun `export excludes triggers and runtime state`() {
        val exported =
            SettingsBackup
                .export(
                    FakePrefs(mapOf("test_progress" to 42)),
                    BackupMeta(),
                ).json
        Prefs.excludedFromBackup.forEach {
            assertTrue("$it must not be exported", !exported.contains("\"$it\""))
        }
        assertTrue(exported.contains("\"stroke_width\""))
    }

    @Test
    fun `malformed file is rejected without touching settings`() {
        val parsed = parse("{ this is not json")
        assertEquals(ParsedBackup.Unusable(FileProblem.NOT_JSON), parsed)
    }

    @Test
    fun `truncated file is rejected`() {
        val exported = SettingsBackup.export(FakePrefs(mapOf("opacity" to 55)), BackupMeta()).json
        val parsed = parse(exported.substring(0, exported.length / 2))
        assertEquals(ParsedBackup.Unusable(FileProblem.NOT_JSON), parsed)
    }

    @Test
    fun `wrong file type is rejected`() {
        assertEquals(ParsedBackup.Unusable(FileProblem.NOT_JSON), parse("PNG\r\n\n"))
        assertEquals(
            ParsedBackup.Unusable(FileProblem.NOT_A_BACKUP),
            parse("""{"format":"some-other-app","formatVersion":1,"settings":{}}"""),
        )
        assertEquals(
            ParsedBackup.Unusable(FileProblem.NOT_A_BACKUP),
            parse(backup("").replace("\"settings\"", "\"stuff\"")),
        )
    }

    @Test
    fun `backup from a newer format is refused rather than guessed`() {
        val parsed = parse(backup("\"opacity\": 50", version = BACKUP_FORMAT_VERSION + 1))
        assertEquals(ParsedBackup.Unusable(FileProblem.FROM_NEWER_APP), parsed)
    }

    @Test
    fun `empty backup is valid and changes nothing`() {
        val parsed = usable(backup(""))
        assertTrue(parsed.entries.isEmpty())
        assertTrue(parsed.writes.isEmpty())
    }

    @Test
    fun `unknown settings from a newer version are reported not fatal`() {
        val parsed = usable(backup(""""teleport_mode": true, "opacity": 42"""))
        assertEquals(RestoreOutcome.Unknown, outcomeOf(parsed, "teleport_mode"))
        assertEquals(RestoreOutcome.Applied, outcomeOf(parsed, "opacity"))
        assertTrue(parsed.writes.none { it.spec.key == "teleport_mode" })
    }

    @Test
    fun `removed settings are reported as deprecated`() {
        val parsed = usable(backup(""""completion_pulse_enabled": true, "opacity": 42"""))
        assertEquals(RestoreOutcome.Deprecated, outcomeOf(parsed, "completion_pulse_enabled"))
        assertTrue(parsed.writes.none { it.spec.key == "completion_pulse_enabled" })
    }

    @Test
    fun `legacy offset keys migrate into the per rotation key`() {
        val parsed =
            usable(
                backup(
                    """
                    "percent_text_offset_x": 12.0,
                    "percent_text_offset_y": -4.0
                    """.trimIndent(),
                ),
            )
        val outcome = outcomeOf(parsed, "percent_text_offsets_by_rotation")
        assertTrue("expected a migration, got $outcome", outcome is RestoreOutcome.Migrated)
        assertEquals(
            listOf(
                AdaptNote.MigratedFromKeys(
                    listOf("percent_text_offset_x", "percent_text_offset_y"),
                ),
            ),
            (outcome as RestoreOutcome.Migrated).notes,
        )
        assertNull(outcomeOf(parsed, "percent_text_offset_x"))

        val write = parsed.writes.first { it.spec.key == "percent_text_offsets_by_rotation" }
        assertEquals(
            "12.0,-4.0|12.0,-4.0|0.0,0.0|0.0,0.0",
            (write.value as RotationOffsets).serialize(),
        )
    }

    @Test
    fun `invalid individual values are rejected one by one`() {
        val parsed =
            usable(
                backup(
                    """
                    "clockwise": "maybe",
                    "opacity": "loud",
                    "power_saver_mode": "hyperdrive",
                    "stroke_width": null,
                    "selected_packages": "com.example",
                    "percent_text_offsets_by_rotation": "1,2|3,4"
                    """.trimIndent(),
                ),
            )
        listOf(
            "clockwise",
            "opacity",
            "power_saver_mode",
            "stroke_width",
            "selected_packages",
            "percent_text_offsets_by_rotation",
        ).forEach { key ->
            assertTrue("$key should be rejected", outcomeOf(parsed, key) is RestoreOutcome.Rejected)
        }
        assertTrue(parsed.writes.isEmpty())
    }

    @Test
    fun `one invalid setting does not stop the valid ones`() {
        val parsed =
            usable(
                backup(
                    """
                    "opacity": "loud",
                    "clockwise": false,
                    "stroke_width": 3.5,
                    "power_saver_mode": "dim"
                    """.trimIndent(),
                ),
            )
        assertTrue(outcomeOf(parsed, "opacity") is RestoreOutcome.Rejected)
        assertEquals(RestoreOutcome.Applied, outcomeOf(parsed, "clockwise"))
        assertEquals(RestoreOutcome.Applied, outcomeOf(parsed, "stroke_width"))
        assertEquals(RestoreOutcome.Applied, outcomeOf(parsed, "power_saver_mode"))
        assertEquals(3, parsed.writes.size)
    }

    @Test
    fun `out of range values are clamped and reported`() {
        val parsed = usable(backup(""""opacity": 5000, "burn_in_hide_ms": 600000"""))
        val opacity = outcomeOf(parsed, "opacity")
        assertTrue(opacity is RestoreOutcome.Migrated)
        assertEquals(
            listOf(AdaptNote.ClampedWhole(5000, 1, 100)),
            (opacity as RestoreOutcome.Migrated).notes,
        )
        assertEquals(100, parsed.writes.first { it.spec.key == "opacity" }.value)
        assertEquals(120000, parsed.writes.first { it.spec.key == "burn_in_hide_ms" }.value)
    }

    @Test
    fun `duplicate entries in a set collapse`() {
        val parsed = usable(backup(""""selected_packages": ["com.b", "com.a", "com.a"]"""))
        val outcome = outcomeOf(parsed, "selected_packages")
        assertTrue("got $outcome", outcome is RestoreOutcome.Migrated)
        assertEquals(
            listOf(AdaptNote.CollapsedDuplicates(1)),
            (outcome as RestoreOutcome.Migrated).notes,
        )
        assertEquals(
            setOf("com.a", "com.b"),
            parsed.writes.first { it.spec.key == "selected_packages" }.value,
        )
    }

    @Test
    fun `a set holding a non text entry is rejected rather than salvaged`() {
        val parsed = usable(backup(""""selected_packages": ["com.b", 7]"""))
        assertEquals(
            RestoreOutcome.Rejected(RejectReason.EntriesNotText),
            outcomeOf(parsed, "selected_packages"),
        )
        assertTrue(parsed.writes.isEmpty())
    }

    @Test
    fun `quoted scalars are not accepted for typed prefs`() {
        val parsed =
            usable(backup(""""clockwise": "true", "opacity": "42", "stroke_width": "3.5""""))
        assertEquals(
            RestoreOutcome.Rejected(RejectReason.NotABoolean),
            outcomeOf(parsed, "clockwise"),
        )
        assertEquals(
            RestoreOutcome.Rejected(RejectReason.NotAWholeNumber),
            outcomeOf(parsed, "opacity"),
        )
        assertEquals(
            RestoreOutcome.Rejected(RejectReason.NotANumber),
            outcomeOf(parsed, "stroke_width"),
        )
        assertTrue(parsed.writes.isEmpty())
    }

    @Test
    fun `nonpositive format versions are refused`() {
        assertEquals(
            ParsedBackup.Unusable(FileProblem.NOT_A_BACKUP),
            parse(backup("\"opacity\": 50", version = 0)),
        )
        assertEquals(
            ParsedBackup.Unusable(FileProblem.NOT_A_BACKUP),
            parse(backup("\"opacity\": 50", version = -1)),
        )
    }

    @Test
    fun `oversized text and sets are rejected`() {
        val long = "x".repeat(5000)
        val parsed = usable(backup(""""preview_filename_text": "$long""""))
        assertTrue(outcomeOf(parsed, "preview_filename_text") is RestoreOutcome.Rejected)
        assertTrue(parsed.writes.isEmpty())
    }

    @Test
    fun `a malformed legacy offset is rejected instead of zero filled`() {
        val parsed =
            usable(
                backup(
                    """
                    "percent_text_offset_x": "twelve",
                    "percent_text_offset_y": -4.0
                    """.trimIndent(),
                ),
            )
        val outcome = outcomeOf(parsed, "percent_text_offsets_by_rotation")
        assertTrue("got $outcome", outcome is RestoreOutcome.Rejected)
        assertTrue(parsed.writes.isEmpty())
    }

    @Test
    fun `export preserves calibration derived from legacy ring offsets`() {
        val legacy = FakePrefs(mapOf("ring_offset_x" to 12f, "ring_offset_y" to -4f))
        val exported = SettingsBackup.export(legacy, BackupMeta()).json
        val restored = usable(exported, FakePrefs())
        assertEquals(
            Prefs.ringOffsets.read(legacy).serialize(),
            (parsedValue(restored, "ring_offsets_by_rotation") as RotationOffsets).serialize(),
        )
    }

    @Test
    fun `duplicate json keys resolve to the last value`() {
        val parsed = usable(backup(""""opacity": 10, "opacity": 20"""))
        assertEquals(20, parsed.writes.first { it.spec.key == "opacity" }.value)
    }

    @Test
    fun `settings absent from the backup keep their current value`() {
        val current = FakePrefs(mapOf("opacity" to 33, "stroke_width" to 7f))
        val parsed = usable(backup(""""clockwise": false"""), current)
        assertTrue(parsed.writes.none { it.spec.key == "opacity" })
        assertTrue(parsed.entries.none { it.key == "stroke_width" })
    }

    @Test
    fun `excluded keys present in a file are reported and never applied`() {
        val parsed = usable(backup(""""test_progress": 80, "persistent_preview": true"""))
        assertEquals(RestoreOutcome.Excluded, outcomeOf(parsed, "test_progress"))
        assertEquals(RestoreOutcome.Excluded, outcomeOf(parsed, "persistent_preview"))
        assertTrue(parsed.writes.isEmpty())
    }

    @Test
    fun `import into a fresh installation applies every value`() {
        val parsed = usable(backup(""""opacity": 42, "clockwise": false, "path_mode": true"""))
        assertEquals(3, parsed.writes.size)
        assertEquals(3, RestoreReport(parsed.meta, parsed.entries).restoredCount)
    }

    @Test
    fun `repeated import of the same backup is idempotent`() {
        val exported = SettingsBackup.export(FakePrefs(mapOf("opacity" to 42)), BackupMeta()).json
        val first = usable(exported, FakePrefs())
        val applied = FakePrefs(first.writes.associate { it.spec.key to it.value })

        val second = usable(exported, applied)
        assertTrue("second import should change nothing", second.writes.isEmpty())
        assertTrue(second.entries.all { it.outcome == RestoreOutcome.Unchanged })
    }

    @Test
    fun `a non finite stored float still produces a readable backup`() {
        val current =
            FakePrefs(
                mapOf(
                    "stroke_width" to Float.NaN,
                    "glow_radius" to Float.POSITIVE_INFINITY,
                ),
            )
        val exported = SettingsBackup.export(current, BackupMeta()).json
        val parsed = usable(exported, FakePrefs())
        assertTrue(parsed.entries.none { it.outcome is RestoreOutcome.Rejected })
        assertEquals(RestoreOutcome.Unchanged, outcomeOf(parsed, "stroke_width"))
    }

    @Test
    fun `non finite numbers in a file are rejected`() {
        val parsed = usable(backup(""""stroke_width": 1e999, "glow_radius": 5.0"""))
        assertTrue(outcomeOf(parsed, "stroke_width") is RestoreOutcome.Rejected)
        assertEquals(RestoreOutcome.Applied, outcomeOf(parsed, "glow_radius"))
    }

    @Test
    fun `deeply nested json is rejected without blowing the stack`() {
        val deep =
            """{"format":"$BACKUP_FORMAT","formatVersion":1,"settings":""" +
                "[".repeat(20000) + "]".repeat(20000) + "}"
        assertEquals(ParsedBackup.Unusable(FileProblem.NOT_JSON), parse(deep))
    }

    @Test
    fun `numbers too large for their type are rejected`() {
        val parsed = usable(backup(""""opacity": 99999999999999, "finish_hold_ms": 2000"""))
        assertTrue(outcomeOf(parsed, "opacity") is RestoreOutcome.Rejected)
        assertEquals(RestoreOutcome.Applied, outcomeOf(parsed, "finish_hold_ms"))
    }

    @Test
    fun `a legacy pair does not override the key it migrates into`() {
        val parsed =
            usable(
                backup(
                    """
                    "percent_text_offset_x": 12.0,
                    "percent_text_offset_y": -4.0,
                    "percent_text_offsets_by_rotation": "1.0,2.0|3.0,4.0|5.0,6.0|7.0,8.0"
                    """.trimIndent(),
                ),
            )
        val write = parsed.writes.first { it.spec.key == "percent_text_offsets_by_rotation" }
        assertEquals(
            "1.0,2.0|3.0,4.0|5.0,6.0|7.0,8.0",
            (write.value as RotationOffsets).serialize(),
        )
    }

    @Test
    fun `binary junk is rejected as not json`() {
        val junk = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        assertEquals(ParsedBackup.Unusable(FileProblem.NOT_JSON), parse(junk.decodeToString()))
    }

    @Test
    fun `metadata survives the round trip`() {
        val meta =
            BackupMeta(
                exportedAt = "2026-08-14T10:00:00Z",
                appVersionName = "2.6.0",
                appVersionCode = 26000,
                deviceModel = "RMX2170",
                androidSdk = 36,
            )
        val parsed = usable(SettingsBackup.export(FakePrefs(), meta).json)
        assertEquals(meta, parsed.meta)
    }

    @Test
    fun `report groups every outcome for the summary`() {
        val parsed =
            usable(
                backup(
                    """
                    "opacity": 42,
                    "clockwise": "maybe",
                    "completion_pulse_enabled": true,
                    "teleport_mode": 1,
                    "test_progress": 5,
                    "badge_offset_x": 3.0,
                    "stroke_width": 2.0
                    """.trimIndent(),
                ),
            )
        val report = RestoreReport(parsed.meta, parsed.entries)
        assertEquals(1, report.applied.size)
        assertEquals(1, report.rejected.size)
        assertEquals(1, report.deprecated.size)
        assertEquals(1, report.unknown.size)
        assertEquals(1, report.excluded.size)
        assertEquals(1, report.migrated.size)
        assertEquals(1, report.unchanged.size)
        assertTrue(report.failed.isEmpty())
        assertEquals(Prefs.excludedFromBackup, report.excludedByPolicy)
    }
}
