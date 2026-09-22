package eu.hxreborn.phdp.xposed.hook

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import eu.hxreborn.phdp.util.logDebug
import java.lang.reflect.Method
import java.util.Locale

object NetworkSpeedMonitor {
    private const val UNIT_BITS = "bits"

    private const val SAMPLE_INTERVAL_MS = 1000L
    private const val MILLIS_PER_SECOND = 1000.0
    private const val SMOOTHING = 0.5
    private const val BITS_PER_BYTE = 8.0
    private const val KILO = 1_000.0
    private const val MEGA = 1_000_000.0
    private const val GIGA = 1_000_000_000.0
    private const val DECIMAL_LIMIT = 100.0
    private const val PREVIEW_BYTES_PER_SECOND = 1_550_000L

    @Volatile
    private var samplerThread: HandlerThread? = null

    @Volatile
    private var handler: Handler? = null

    @Volatile
    private var running = false

    @Volatile
    private var connectivityManager: ConnectivityManager? = null

    @Volatile
    private var underlyingNetworksMethod: Method? = null

    private var lastInterface: String? = null
    private var lastRxBytes = TrafficStats.UNSUPPORTED.toLong()
    private var lastSampleMs = 0L
    private var smoothedBytesPerSecond = -1.0

    var onSpeedChanged: ((String?) -> Unit)? = null

    private val sampler =
        object : Runnable {
            override fun run() {
                if (!running) return
                sample()
                if (running) handler?.postDelayed(this, SAMPLE_INTERVAL_MS)
            }
        }

    fun attach(context: Context) {
        connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    }

    fun start() {
        if (running) return
        running = true
        val target = handler ?: createHandler()
        target.post { captureBaseline() }
        target.postDelayed(sampler, SAMPLE_INTERVAL_MS)
        logDebug { "started speed-monitor" }
    }

    fun stop() {
        if (!running) return
        running = false
        handler?.removeCallbacks(sampler)
        handler?.post { onSpeedChanged?.invoke(null) }
        logDebug { "stopped speed-monitor" }
    }

    fun shutdown() {
        stop()
        samplerThread?.quitSafely()
        samplerThread = null
        handler = null
        connectivityManager = null
        onSpeedChanged = null
        logDebug { "shut down speed-monitor" }
    }

    private fun createHandler(): Handler {
        val thread = HandlerThread("phdp-speed").apply { start() }
        val created = Handler(thread.looper)
        samplerThread = thread
        handler = created
        return created
    }

    private fun captureBaseline() {
        lastInterface = transportInterface()
        lastRxBytes = readRxBytes(lastInterface)
        lastSampleMs = SystemClock.elapsedRealtime()
        smoothedBytesPerSecond = -1.0
    }

    private fun sample() {
        if (!IndicatorState.speedTextEnabled) return
        val iface = transportInterface()
        val rxBytes = readRxBytes(iface)
        val now = SystemClock.elapsedRealtime()
        val previousBytes = lastRxBytes
        val previousInterface = lastInterface
        val elapsed = now - lastSampleMs
        lastInterface = iface
        lastRxBytes = rxBytes
        lastSampleMs = now

        val usable =
            previousBytes >= 0 &&
                rxBytes >= previousBytes &&
                iface == previousInterface &&
                elapsed > 0
        if (!usable) {
            smoothedBytesPerSecond = -1.0
            logDebug { "speed iface=$iface unusable rx=$rxBytes elapsed=$elapsed" }
            onSpeedChanged?.invoke(null)
            return
        }

        val bytesPerSecond = (rxBytes - previousBytes) * MILLIS_PER_SECOND / elapsed
        smoothedBytesPerSecond =
            if (smoothedBytesPerSecond < 0) {
                bytesPerSecond
            } else {
                SMOOTHING * bytesPerSecond + (1 - SMOOTHING) * smoothedBytesPerSecond
            }
        logDebug { "speed iface=$iface bytesPerSecond=$bytesPerSecond elapsed=$elapsed" }
        onSpeedChanged?.invoke(
            format(smoothedBytesPerSecond.toLong(), IndicatorState.speedTextUnit),
        )
    }

    private fun readRxBytes(iface: String?): Long =
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> TrafficStats.getTotalRxBytes()
            iface != null -> TrafficStats.getRxBytes(iface)
            else -> TrafficStats.UNSUPPORTED.toLong()
        }

    @SuppressLint("MissingPermission")
    private fun transportInterface(): String? =
        connectivityManager?.let { manager ->
            runCatching {
                val active = manager.activeNetwork
                val network = underlyingNetwork(manager, active) ?: active
                manager.getLinkProperties(network)?.interfaceName
            }.getOrNull()
        }

    @SuppressLint("MissingPermission")
    private fun underlyingNetwork(
        manager: ConnectivityManager,
        network: Network?,
    ): Network? {
        val capabilities = manager.getNetworkCapabilities(network) ?: return null
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return null
        return runCatching {
            val method =
                underlyingNetworksMethod ?: NetworkCapabilities::class.java
                    .getMethod("getUnderlyingNetworks")
                    .also { underlyingNetworksMethod = it }
            (method.invoke(capabilities) as? List<*>)?.firstOrNull() as? Network
        }.getOrNull()
    }

    fun previewText(unit: String): String = format(PREVIEW_BYTES_PER_SECOND, unit)

    fun format(
        bytesPerSecond: Long,
        unit: String,
    ): String {
        val bits = unit == UNIT_BITS
        val value = if (bits) bytesPerSecond * BITS_PER_BYTE else bytesPerSecond.toDouble()
        val suffix = if (bits) "bps" else "B/s"
        val (scaled, prefix) =
            when {
                value >= GIGA -> value / GIGA to "G"
                value >= MEGA -> value / MEGA to "M"
                else -> value / KILO to "k"
            }
        val pattern = if (scaled < DECIMAL_LIMIT) "%.1f %s%s" else "%.0f %s%s"
        return String.format(Locale.getDefault(), pattern, scaled, prefix, suffix)
    }
}
