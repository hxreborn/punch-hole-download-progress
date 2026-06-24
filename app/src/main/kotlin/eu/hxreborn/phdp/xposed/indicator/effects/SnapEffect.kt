package eu.hxreborn.phdp.xposed.indicator.effects

import android.graphics.Canvas
import android.view.View

class SnapEffect : CompletionEffect {
    override fun start(
        view: View,
        params: EffectParams,
        onEnd: () -> Unit,
    ) = onEnd()

    override fun draw(
        canvas: Canvas,
        ctx: FinishDrawContext,
    ) {
    }

    override fun cancel() {}
}
