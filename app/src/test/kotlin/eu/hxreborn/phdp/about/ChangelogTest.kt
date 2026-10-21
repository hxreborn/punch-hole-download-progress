package eu.hxreborn.phdp.about

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `strips a leading dash, star or dot bullet and keeps the line as written`() {
        listOf("- ", "* ", "• ", "").forEach { bullet ->
            assertEquals(
                bullet,
                listOf("Added a status card"),
                parseEntries("${bullet}Added a status card\n"),
            )
        }
    }

    @Test
    fun `reads release dates and skips malformed lines`() {
        assertEquals(
            mapOf("2.10.0" to LocalDate.of(2026, 10, 16), "2.9.0" to LocalDate.of(2026, 10, 1)),
            parseReleaseDates("v2.10.0 2026-10-16\nv2.9.0 2026-10-01\nbroken\nv2.8.0 notadate\n\n"),
        )
    }

    @Test
    fun `leaves a version without a release line undated`() {
        assertNull(parseReleaseDates("v2.10.0 2026-10-16\n")["2.11.0"])
    }

    @Test
    fun `reads plain and bulleted notes for an undated version`() {
        val dates = parseReleaseDates("v2.10.0 2026-10-16\n")
        val name = versionNameOf(211000)
        val release =
            Release(
                211000,
                name,
                parseEntries("- Added a status card\nFixed tab labels\n"),
                dates[name],
            )
        assertEquals(listOf("Added a status card", "Fixed tab labels"), release.entries)
        assertNull(release.releasedOn)
    }
}
