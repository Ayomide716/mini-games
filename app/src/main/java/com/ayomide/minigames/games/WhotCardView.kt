package com.ayomide.minigames.games

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import com.ayomide.minigames.R
import com.ayomide.minigames.logic.Suit
import com.ayomide.minigames.logic.WCard
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Draws a Whot card face (maroon on cream) or the card back. */
class WhotCardView(context: Context, var card: WCard?, var faceDown: Boolean = false) : View(context) {

    var dimmed = false
        set(v) { field = v; alpha = if (v) 0.45f else 1f }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    init {
        contentDescription = card?.toString() ?: "Card"
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = w * 0.1f
        rect.set(2f, 2f, w - 2f, h - 2f)
        val maroon = context.getColor(R.color.whot_maroon)

        if (faceDown || card == null) {
            paint.style = Paint.Style.FILL
            paint.color = maroon
            canvas.drawRoundRect(rect, r, r, paint)
            paint.color = context.getColor(R.color.whot_cream)
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = w * 0.26f
            canvas.save()
            canvas.rotate(-30f, w / 2, h / 2)
            canvas.drawText("WHOT", w / 2, h / 2 + paint.textSize / 3, paint)
            canvas.restore()
            return
        }
        val c = card!!
        paint.style = Paint.Style.FILL
        paint.color = context.getColor(R.color.whot_cream)
        canvas.drawRoundRect(rect, r, r, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.025f
        paint.color = maroon
        canvas.drawRoundRect(rect, r, r, paint)

        // Corner numbers
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = w * 0.2f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(c.number.toString(), w * 0.1f, w * 0.26f, paint)
        canvas.save()
        canvas.rotate(180f, w / 2, h / 2)
        canvas.drawText(c.number.toString(), w * 0.1f, w * 0.26f, paint)
        canvas.restore()

        // Centre symbol
        val cx = w / 2
        val cy = h / 2
        val s = min(w, h) * 0.26f
        drawSuit(canvas, c.suit, cx, cy, s, paint)
    }

    companion object {
        fun drawSuit(canvas: Canvas, suit: Suit, cx: Float, cy: Float, s: Float, paint: Paint) {
            paint.style = Paint.Style.FILL
            val path = Path()
            when (suit) {
                Suit.CIRCLE -> canvas.drawCircle(cx, cy, s, paint)
                Suit.SQUARE -> canvas.drawRect(cx - s * 0.9f, cy - s * 0.9f, cx + s * 0.9f, cy + s * 0.9f, paint)
                Suit.TRIANGLE -> {
                    path.moveTo(cx, cy - s); path.lineTo(cx + s, cy + s * 0.8f); path.lineTo(cx - s, cy + s * 0.8f); path.close()
                    canvas.drawPath(path, paint)
                }
                Suit.CROSS -> {
                    val t = s * 0.35f
                    canvas.drawRect(cx - t, cy - s, cx + t, cy + s, paint)
                    canvas.drawRect(cx - s, cy - t, cx + s, cy + t, paint)
                }
                Suit.STAR -> {
                    for (i in 0 until 10) {
                        val rad = if (i % 2 == 0) s else s * 0.45f
                        val a = Math.PI / 2 + i * Math.PI / 5
                        val x = cx + (rad * cos(a)).toFloat()
                        val y = cy - (rad * sin(a)).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    canvas.drawPath(path, paint)
                }
                Suit.WHOT -> {
                    paint.textAlign = Paint.Align.CENTER
                    paint.typeface = Typeface.DEFAULT_BOLD
                    val old = paint.textSize
                    paint.textSize = s * 0.9f
                    canvas.drawText("WHOT", cx, cy + paint.textSize / 3, paint)
                    paint.textSize = old
                }
            }
        }
    }
}
