package info.cosmonet.flusiva

import android.graphics.*
import android.graphics.drawable.Drawable

/** Small code-native icons. Touch targets are set by the containing views. */
class MusicIcon(private val name: String, private val color: Int) : Drawable() {
    private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = this@MusicIcon.color; strokeWidth = 1.8f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    override fun draw(canvas: Canvas) {
        val save = canvas.save(); canvas.translate(bounds.left.toFloat(), bounds.top.toFloat()); canvas.scale(bounds.width() / 24f, bounds.height() / 24f)
        fun line(x: Float, y: Float, x2: Float, y2: Float) { pen.style = Paint.Style.STROKE; canvas.drawLine(x, y, x2, y2, pen) }
        fun triangle(points: FloatArray) { pen.style = Paint.Style.FILL; canvas.drawPath(Path().apply { moveTo(points[0], points[1]); lineTo(points[2], points[3]); lineTo(points[4], points[5]); close() }, pen) }
        when (name) {
            "play" -> triangle(floatArrayOf(8f, 5f, 19f, 12f, 8f, 19f))
            "pause" -> { pen.style = Paint.Style.FILL; canvas.drawRoundRect(6f, 5f, 10f, 19f, 1f, 1f, pen); canvas.drawRoundRect(14f, 5f, 18f, 19f, 1f, 1f, pen) }
            "previous" -> { line(6f, 5f, 6f, 19f); triangle(floatArrayOf(18f, 5f, 8f, 12f, 18f, 19f)) }
            "next" -> { line(18f, 5f, 18f, 19f); triangle(floatArrayOf(6f, 5f, 16f, 12f, 6f, 19f)) }
            "shuffle" -> { line(4f, 6f, 7f, 6f); line(7f, 6f, 17f, 18f); line(17f, 18f, 21f, 18f); line(18f, 15f, 21f, 18f); line(21f, 18f, 18f, 21f); line(4f, 18f, 7f, 18f); line(7f, 18f, 11f, 13f); line(14f, 9f, 17f, 6f); line(17f, 6f, 21f, 6f); line(18f, 3f, 21f, 6f); line(21f, 6f, 18f, 9f) }
            "repeat" -> { line(5f, 8f, 19f, 8f); line(19f, 8f, 16f, 5f); line(19f, 8f, 16f, 11f); line(19f, 16f, 5f, 16f); line(5f, 16f, 8f, 13f); line(5f, 16f, 8f, 19f) }
            "timer" -> { pen.style = Paint.Style.STROKE; canvas.drawCircle(12f, 13f, 7f, pen); line(12f, 13f, 12f, 9f); line(10f, 2f, 14f, 2f); line(12f, 2f, 12f, 5f) }
            else -> { line(15f, 4f, 15f, 16f); line(15f, 4f, 20f, 6f); pen.style = Paint.Style.FILL; canvas.drawOval(6f, 14f, 15f, 20f, pen) }
        }
        canvas.restoreToCount(save)
    }
    override fun setAlpha(alpha: Int) { pen.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(filter: ColorFilter?) { pen.colorFilter = filter; invalidateSelf() }
    @Deprecated("Deprecated in Java") override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
