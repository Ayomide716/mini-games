package com.ayomide.minigames.ui

import android.content.Context
import android.graphics.Paint
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

/** Square canvas view that reports taps as (x, y) in pixels. */
abstract class BoardView(context: Context, private val aspect: Float = 1f) : View(context) {

    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected fun c(id: Int) = context.getColor(id)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val hSpec = MeasureSpec.getSize(heightMeasureSpec)
        var h = (w * aspect).toInt()
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED && hSpec > 0) h = min(h, hSpec)
        setMeasuredDimension(w, h)
    }

    abstract fun onTap(x: Float, y: Float)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                performClick()
                onTap(event.x, event.y)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun buzz() = performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
}
