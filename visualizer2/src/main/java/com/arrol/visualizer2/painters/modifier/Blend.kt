package io.github.jeffshee.visualizer.painters.modifier

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import io.github.jeffshee.visualizer.painters.Painter
import io.github.jeffshee.visualizer.utils.VisualizerHelper

class Blend(val src: Painter, val dst: Painter) : Painter() {
    override var paint = Paint()

    override fun calc(helper: VisualizerHelper) {
        src.calc(helper)
        dst.calc(helper)
    }

    override fun draw(canvas: Canvas, helper: VisualizerHelper) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.saveLayer(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)
        } else {
            @Suppress("DEPRECATION")
            canvas.saveLayer(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint, Canvas.ALL_SAVE_FLAG)
        }
        dst.apply { paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN) }
        src.draw(canvas, helper)
        dst.draw(canvas, helper)
        canvas.restore()
    }
}