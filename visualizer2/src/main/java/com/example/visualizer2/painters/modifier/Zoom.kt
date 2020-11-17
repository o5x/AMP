package io.github.jeffshee.visualizer.painters.modifier

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import io.github.jeffshee.visualizer.painters.Painter
import io.github.jeffshee.visualizer.utils.VisualizerHelper

class Zoom(
    vararg val painters: Painter,
    //
    var pxR: Float = .5f,
    var pyR: Float = .5f,
    //
    var anim: ValueAnimator = ValueAnimator.ofFloat(.9f, 1.1f).apply {
        duration = 8000;repeatCount = ValueAnimator.INFINITE;repeatMode = ValueAnimator.REVERSE
    }
) : Painter() {

    override var paint = Paint()

    init {
        anim.start()
    }

    override fun calc(helper: VisualizerHelper) {
        painters.forEach { painter ->
            painter.calc(helper)
        }
    }

    override fun draw(canvas: Canvas, helper: VisualizerHelper) {
        canvas.save()
        canvas.scale(
            anim.animatedValue as Float, anim.animatedValue as Float,
            pxR * canvas.width, pyR * canvas.height
        )
        painters.forEach { painter ->
            painter.paint.apply { colorFilter = paint.colorFilter;xfermode = paint.xfermode }
            painter.draw(canvas, helper)
        }
        canvas.restore()
    }
}