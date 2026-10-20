package eu.hxreborn.phdp.about

import org.junit.Assert.assertEquals
import org.junit.Test

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
}
