package eu.hxreborn.phdp.prefs

import eu.hxreborn.phdp.FakePrefs
import eu.hxreborn.phdp.backup.BackupMeta
import eu.hxreborn.phdp.backup.RestoreOutcome
import eu.hxreborn.phdp.backup.SettingsBackup
import eu.hxreborn.phdp.ui.MAX_BACKUP_BYTES
import eu.hxreborn.phdp.ui.readAtMost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class PrefsRepositoryImportTest {
    private fun backupOf(vararg settings: Pair<String, Any>): String {
        val body =
            settings.joinToString(",") { (key, value) ->
                val encoded = if (value is String) "\"$value\"" else value.toString()
                "\"$key\": $encoded"
            }
        return """
            {
              "format": "phdp-settings",
              "formatVersion": 1,
              "exportedAt": "2026-08-14T10:00:00Z",
              "app": { "versionName": "2.6.0", "versionCode": 26000 },
              "device": { "model": "RMX2170", "androidSdk": 36 },
              "settings": { $body }
            }
            """.trimIndent()
    }

    @Test
    fun `import writes valid settings to local and remote`() {
        val local = FakePrefs(mapOf("opacity" to 90))
        val remote = FakePrefs()
        val repository = PrefsRepositoryImpl(local) { remote }

        val result = repository.importSettings(backupOf("opacity" to 42, "clockwise" to false))

        assertTrue(result is ImportResult.Restored)
        assertEquals(42, local.values["opacity"])
        assertEquals(false, local.values["clockwise"])
        assertEquals(42, remote.values["opacity"])
    }

    @Test
    fun `a write that throws is reported without losing the others`() {
        val local = FakePrefs(mapOf("opacity" to 90), poisonedKey = "opacity")
        val repository = PrefsRepositoryImpl(local) { null }

        val result =
            repository.importSettings(
                backupOf("opacity" to 42, "clockwise" to false),
            ) as ImportResult.Restored

        val opacity =
            result.report.entries
                .first { it.key == "opacity" }
                .outcome
        assertTrue("got $opacity", opacity is RestoreOutcome.Failed)
        assertEquals(
            RestoreOutcome.Applied,
            result.report.entries
                .first { it.key == "clockwise" }
                .outcome,
        )
        assertEquals(90, local.values["opacity"])
        assertEquals(false, local.values["clockwise"])
    }

    @Test
    fun `a failed commit rolls back and reports every write as failed`() {
        val local = FakePrefs(mapOf("opacity" to 90, "clockwise" to true), commitSucceeds = false)
        val repository = PrefsRepositoryImpl(local) { null }

        val result =
            repository.importSettings(
                backupOf("opacity" to 42, "clockwise" to false),
            ) as ImportResult.Restored

        assertEquals(2, result.report.failed.size)
        assertTrue(result.report.applied.isEmpty())
        assertEquals(90, local.values["opacity"])
        assertEquals(true, local.values["clockwise"])
    }

    @Test
    fun `an unreadable file leaves settings untouched`() {
        val local = FakePrefs(mapOf("opacity" to 90))
        val repository = PrefsRepositoryImpl(local) { null }

        val result = repository.importSettings("not a backup at all")

        assertTrue(result is ImportResult.Unreadable)
        assertEquals(90, local.values["opacity"])
    }

    @Test
    fun `export then import on a second install reproduces the settings`() {
        val source =
            FakePrefs(
                mapOf(
                    "opacity" to 42,
                    "path_mode" to true,
                    "power_saver_mode" to "dim",
                ),
            )
        val exported = PrefsRepositoryImpl(source) { null }.exportSettings(BackupMeta()).json

        val target = FakePrefs()
        PrefsRepositoryImpl(target) { null }.importSettings(exported)

        assertEquals(42, target.values["opacity"])
        assertEquals(true, target.values["path_mode"])
        assertEquals("dim", target.values["power_saver_mode"])
        Prefs.excludedFromBackup.forEach { assertTrue(!target.values.containsKey(it)) }
    }

    @Test
    fun `oversized files are refused before they are parsed`() {
        assertEquals(4, ByteArrayInputStream(ByteArray(4)).readAtMost(4)?.size)
        assertNull(ByteArrayInputStream(ByteArray(5)).readAtMost(4))
        assertEquals(0, ByteArrayInputStream(ByteArray(0)).readAtMost(4)?.size)
        assertNull(
            ByteArrayInputStream(ByteArray(MAX_BACKUP_BYTES + 1)).readAtMost(MAX_BACKUP_BYTES),
        )
    }

    @Test
    fun `registry keys all round trip through export`() {
        val exported = PrefsRepositoryImpl(FakePrefs()) { null }.exportSettings(BackupMeta()).json
        val parsed = SettingsBackup.parse(exported, FakePrefs())
        val usable = parsed as eu.hxreborn.phdp.backup.ParsedBackup.Usable
        assertTrue(usable.entries.none { it.outcome is RestoreOutcome.Unknown })
        assertTrue(usable.entries.none { it.outcome is RestoreOutcome.Rejected })
        assertEquals(
            SettingsBackup.registry.size - Prefs.excludedFromBackup.size,
            usable.entries.size,
        )
    }
}
