package eu.hxreborn.phdp.about

import org.junit.Assert.assertEquals
import org.junit.Test

class ContributorsTest {
    @Test
    fun `reads login, line counts, url and avatar path`() {
        val entries =
            parseContributors(
                """[{"login":"a","owner":true,"added":9637,"removed":1964,"url":"https://github.com/a","avatar":"avatars/a.webp"}]""",
            )
        assertEquals(
            listOf(
                ContributorEntry("a", true, 9637, 1964, "https://github.com/a", "avatars/a.webp"),
            ),
            entries,
        )
    }

    @Test
    fun `blocks follow only the contributor's own add and remove ratio`() {
        assertEquals(3 to 1, greenBlocks(50122, 24473) to redBlocks(50122, 24473))
        assertEquals(4 to 0, greenBlocks(2957, 633) to redBlocks(2957, 633))
        assertEquals(4 to 0, greenBlocks(286, 1) to redBlocks(286, 1))
        assertEquals(5 to 0, greenBlocks(10, 0) to redBlocks(10, 0))
        assertEquals(0 to 5, greenBlocks(0, 7) to redBlocks(0, 7))
        assertEquals(2 to 2, greenBlocks(1, 1) to redBlocks(1, 1))
        assertEquals(0 to 0, greenBlocks(0, 0) to redBlocks(0, 0))
    }
}
