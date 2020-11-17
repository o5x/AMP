package io.github.jeffshee.visualizer.painters.fft

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import io.github.jeffshee.visualizer.painters.Painter
import io.github.jeffshee.visualizer.utils.VisualizerHelper
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction

class FftAnalog(
    override var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE;style = Paint.Style.STROKE;strokeWidth = 2f
    },
    //
    var startHz: Int = 0,
    var endHz: Int = 2000,
    //
    var num: Int = 128,
    var interpolator: String = "li",
    //
    var mirror: Boolean = false,
    var power: Boolean = false,
    //
    var ampR: Float = 1f
) : Painter() {

    private var points = Array(0) { GravityModel() }
    private val path = Path()
    private var skipFrame = false
    lateinit var fft: DoubleArray
    lateinit var psf: PolynomialSplineFunction

    override fun calc(helper: VisualizerHelper) {
        fft = helper.getFftMagnitudeRange(startHz, endHz)

        if (isQuiet(fft)) {
            skipFrame = true
            return
        } else skipFrame = false

        if (power) fft = getPowerFft(fft)
        if (mirror) fft = getMirrorFft(fft)

        if (points.size != fft.size) points =
            Array(fft.size) { GravityModel(0f) }
        points.forEachIndexed { index, bar -> bar.update(fft[index].toFloat() * ampR) }

        psf = interpolateFft(points, num, interpolator)
    }

    override fun draw(canvas: Canvas, helper: VisualizerHelper) {
        if (skipFrame) return

        val width = canvas.width.toFloat()
        val gapWidth = width / num

        drawHelper(canvas, "a", 0f, .5f) {
            for (i in 0 until num) {
                if (i % 2 == 0)
                    if (i == 0)
                        path.moveTo(gapWidth * i, -psf.value(i.toDouble()).toFloat())
                    else
                        path.lineTo(gapWidth * i, -psf.value(i.toDouble()).toFloat())
                else
                    path.lineTo(gapWidth * i, psf.value(i.toDouble()).toFloat())
            }
            canvas.drawPath(path, paint)
        }
        path.reset()
    }
}