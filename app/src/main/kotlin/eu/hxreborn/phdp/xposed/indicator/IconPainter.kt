package eu.hxreborn.phdp.xposed.indicator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.ColorInt
import androidx.core.graphics.createBitmap
import eu.hxreborn.phdp.prefs.PrefsManager
import eu.hxreborn.phdp.util.log

class IconPainter(
    private val context: Context,
    private val density: Float,
) {
    private data class CachedIcon(
        val packageName: String,
        val sizePx: Int,
        val bitmap: Bitmap,
        val monoBitmap: Bitmap?,
    ) {
        fun recycle() {
            bitmap.recycle()
            monoBitmap?.recycle()
        }
    }

    private val iconPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            setShadowLayer(
                IndicatorView.LABEL_SHADOW_RADIUS_DP * density,
                0f,
                IndicatorView.LABEL_SHADOW_DY_DP * density,
                IndicatorView.LABEL_SHADOW_COLOR,
            )
        }

    private var cache: CachedIcon? = null

    @ColorInt
    private var iconTintColor: Int = 0
    private var alphaPercent: Int = 100
    private var monoColorFilter = PorterDuffColorFilter(iconTintColor, PorterDuff.Mode.SRC_IN)

    fun updateColors(
        @ColorInt color: Int,
        opacity: Int,
    ) {
        iconTintColor = color
        alphaPercent = opacity.coerceIn(0, 100)
        monoColorFilter = PorterDuffColorFilter(iconTintColor, PorterDuff.Mode.SRC_IN)
    }

    fun draw(
        canvas: Canvas,
        x: Float,
        y: Float,
        packageName: String,
    ) {
        val sizePx = (PrefsManager.appIconSize * density).toInt()
        val cached = getOrLoadCache(packageName, sizePx) ?: return

        val monoBitmap = cached.monoBitmap
        val useMonochrome = PrefsManager.appIconMonochrome && monoBitmap != null
        val bitmap = if (useMonochrome) monoBitmap else cached.bitmap

        iconPaint.colorFilter = if (useMonochrome) monoColorFilter else null
        iconPaint.alpha = alphaPercent * 255 / 100
        canvas.drawBitmap(bitmap, x, y, iconPaint)
    }

    fun invalidateCache() {
        cache?.recycle()
        cache = null
    }

    fun recycle() = invalidateCache()

    private fun getOrLoadCache(
        packageName: String,
        sizePx: Int,
    ): CachedIcon? {
        cache?.takeIf { it.packageName == packageName && it.sizePx == sizePx }?.let { return it }

        val drawable =
            runCatching { context.packageManager.getApplicationIcon(packageName) }
                .onFailure { log("Failed to load icon for $packageName", it) }
                .getOrNull() ?: return null

        val newCache =
            CachedIcon(
                packageName = packageName,
                sizePx = sizePx,
                bitmap = drawable.toBitmap(sizePx),
                monoBitmap = drawable.loadNativeMonoBitmap(sizePx),
            )

        cache?.recycle()
        cache = newCache
        return newCache
    }

    private fun Drawable.toBitmap(sizePx: Int): Bitmap =
        createBitmap(sizePx, sizePx).also {
            setBounds(0, 0, sizePx, sizePx)
            draw(Canvas(it))
        }

    private fun Drawable.loadNativeMonoBitmap(sizePx: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val mono = (this as? AdaptiveIconDrawable)?.monochrome ?: return null
        val insetPx = (sizePx * AdaptiveIconDrawable.getExtraInsetFraction()).toInt()
        return createBitmap(sizePx, sizePx).also {
            mono.setBounds(-insetPx, -insetPx, sizePx + insetPx, sizePx + insetPx)
            mono.draw(Canvas(it))
        }
    }
}
