package eu.hxreborn.phdp.about

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ChangelogTest {
    @Test
    fun `decodes both version code formulas and legacy codes`() {
        assertEquals("1.0.0", versionNameOf(10129))
        assertEquals("2.0.0", versionNameOf(20228))
        assertEquals("2.3.2", versionNameOf(23002))
        assertEquals("2.9.0", versionNameOf(29000))
        assertEquals("2.10.0", versionNameOf(210000))
        assertEquals("3.0.1", versionNameOf(300001))
    }

    @Test
    fun `splits bullet and plain lines into entries`() {
        assertEquals(
            listOf("Added a thing", "Fixed another", "Plain line"),
            parseEntries("- Added a thing\n\n- Fixed another\nPlain line\n"),
        )
    }

    @Test
    fun `picks the badge kind from the leading verb and keeps the line`() {
        assertEquals(Note(Note.Kind.NEW, "Added an About screen"), noteOf("Added an About screen"))
        assertEquals(Note.Kind.FIXED, noteOf("Fixed tab labels wrapping").kind)
        assertEquals(Note.Kind.REMOVED, noteOf("Removed the custom preset chip").kind)
        assertEquals(Note.Kind.UPDATED, noteOf("Updated core libraries").kind)
        assertEquals(Note.Kind.CHANGED, noteOf("Renamed Reload module to Hot reload").kind)
        assertEquals(Note.Kind.CHANGED, noteOf("Smoother ring drawing").kind)
        assertEquals(Note.Kind.CHANGED, noteOf("Addedness is not a verb").kind)
    }

    @Test
    fun `reads tag dates and skips malformed lines`() {
        assertEquals(
            mapOf("2.10.0" to LocalDate.of(2026, 10, 16), "2.9.0" to LocalDate.of(2026, 10, 1)),
            parseReleaseDates("v2.10.0 2026-10-16\nv2.9.0 2026-10-01\nbroken\nv2.8.0 notadate\n\n"),
        )
    }
}
