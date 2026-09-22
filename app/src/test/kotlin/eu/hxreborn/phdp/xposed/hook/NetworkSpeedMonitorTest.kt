package eu.hxreborn.phdp.xposed.hook

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class NetworkSpeedMonitorTest {
    @Test
    fun `scales to bit units with one decimal below a hundred`() {
        Locale.setDefault(Locale.US)
        assertEquals("0.0 kbps", NetworkSpeedMonitor.format(0, "bits"))
        assertEquals("8.0 kbps", NetworkSpeedMonitor.format(1_000, "bits"))
        assertEquals("2.4 Mbps", NetworkSpeedMonitor.format(300_000, "bits"))
        assertEquals("234 Mbps", NetworkSpeedMonitor.format(29_300_000, "bits"))
        assertEquals("1.2 Gbps", NetworkSpeedMonitor.format(150_000_000, "bits"))
    }

    @Test
    fun `scales to byte units`() {
        Locale.setDefault(Locale.US)
        assertEquals("1.0 kB/s", NetworkSpeedMonitor.format(1_000, "bytes"))
        assertEquals("2.9 MB/s", NetworkSpeedMonitor.format(2_900_000, "bytes"))
        assertEquals("1.5 GB/s", NetworkSpeedMonitor.format(1_500_000_000, "bytes"))
    }
}
