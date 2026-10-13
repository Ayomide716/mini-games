package com.ayomide.minigames.games

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.LinearLayout
import com.ayomide.minigames.R
import com.ayomide.minigames.logic.MemoryGame
import com.ayomide.minigames.ui.BaseScreen
import com.ayomide.minigames.ui.BoardView
import com.ayomide.minigames.ui.Stats
import com.ayomide.minigames.ui.mmss

class MemoryView(context: Context, private val cols: Int, private val rows: Int, private val onCard: (Int) -> Unit) :
    BoardView(context, aspect = rows.toFloat() / cols) {

    var game: MemoryGame? = null

    init {
        contentDescription = "Memory cards"
    }

    override fun onDraw(canvas: Canvas) {
        val g = game ?: return
        val cw = width.toFloat() / cols
        val ch = height.toFloat() / rows
        val gap = cw * 0.06f
        for (i in g.cards.indices) {
            val x = (i % cols) * cw
            val y = (i / cols) * ch
            val up = g.faceUp[i] || g.matched[i]
            paint.style = Paint.Style.FILL
            paint.color = if (up) c(R.color.card) else c(R.color.accent)
            paint.alpha = if (g.matched[i]) 140 else 255
            canvas.drawRoundRect(x + gap, y + gap, x + cw - gap, y + ch - gap, cw * 0.14f, cw * 0.14f, paint)
            paint.alpha = 255
            paint.textAlign = Paint.Align.CENTER
            if (up) {
                paint.textSize = minOf(cw, ch) * 0.5f
                canvas.drawText(g.cards[i], x + cw / 2, y + ch / 2 - (paint.descent() + paint.ascent()) / 2, paint)
            } else {
                paint.color = c(R.color.on_accent)
                paint.typeface = Typeface.DEFAULT_BOLD
                paint.textSize = minOf(cw, ch) * 0.4f
                canvas.drawText("?", x + cw / 2, y + ch / 2 - (paint.descent() + paint.ascent()) / 2, paint)
            }
        }
    }

    override fun onTap(x: Float, y: Float) {
        val col = (x / (width.toFloat() / cols)).toInt().coerceIn(0, cols - 1)
        val row = (y / (height.toFloat() / rows)).toInt().coerceIn(0, rows - 1)
        val i = row * cols + col
        if (game?.cards?.indices?.contains(i) == true) onCard(i)
    }
}

class MemoryActivity : BaseScreen() {

    override val screenTitle = "Memory Match"

    private class Size(val label: String, val pairs: Int, val cols: Int)
    private val sizes = listOf(Size("Easy · 16 cards", 8, 4), Size("Hard · 24 cards", 12, 4))

    private var size: Size? = null
    private var game: MemoryGame? = null
    private var startedAt = 0L
    private var finishedSecs = -1L
    private var view: MemoryView? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onPause() {
        super.onPause()
        // Don't leave a mismatched pair waiting if we leave the screen.
        handler.removeCallbacksAndMessages(null)
        game?.hideMismatch()
    }

    override fun render() {
        val s = size
        val g = game
        val stats = Stats(this)
        if (s == null || g == null) {
            add(ui.card().apply {
                addView(ui.title("Memory Match"))
                addView(ui.small("Flip two cards at a time and find all the pairs in as few moves as you can."))
                for (z in sizes) {
                    val best = stats.long("mem_best_${z.pairs}")
                    addView(ui.button(z.label + if (best > 0) "   ·   best $best moves" else "") { start(z) })
                }
            })
            return
        }
        val rows = s.pairs * 2 / s.cols
        val done = g.isComplete
        add(ui.text(if (done) "🎉 All pairs found in ${g.moves} moves (${mmss(finishedSecs)})" else "Moves: ${g.moves}", 17f,
            if (done) R.color.success else R.color.text_primary, true))
        val v = MemoryView(this, s.cols, rows) { i -> tap(i) }.apply {
            game = g
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (8 * resources.displayMetrics.density).toInt()
            }
        }
        view = v
        add(v)
        if (done) {
            add(ui.button("Play again") { start(s) })
            add(ui.flatButton("Change size") { size = null; game = null; refresh() })
        } else {
            add(ui.flatButton("Restart") { start(s) })
        }
        val best = stats.long("mem_best_${s.pairs}")
        if (best > 0) add(ui.small("Best: $best moves"))
    }

    private fun start(z: Size) {
        handler.removeCallbacksAndMessages(null)
        size = z
        game = MemoryGame(z.pairs)
        startedAt = System.currentTimeMillis()
        finishedSecs = -1
        refresh()
    }

    private fun tap(i: Int) {
        val g = game ?: return
        when (g.flip(i)) {
            MemoryGame.Flip.IGNORED -> return
            MemoryGame.Flip.MISMATCH -> handler.postDelayed({ g.hideMismatch(); view?.invalidate() }, 800)
            MemoryGame.Flip.MATCH -> if (g.isComplete) {
                finishedSecs = (System.currentTimeMillis() - startedAt) / 1000
                Stats(this).setBestLow("mem_best_${size!!.pairs}", g.moves.toLong())
                refresh(); return
            }
            MemoryGame.Flip.FIRST -> Unit
        }
        view?.buzz()
        refresh()
    }
}
