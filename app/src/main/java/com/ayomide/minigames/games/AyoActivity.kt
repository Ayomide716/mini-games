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
import com.ayomide.minigames.logic.Oware
import com.ayomide.minigames.ui.BaseScreen
import com.ayomide.minigames.ui.BoardView
import com.ayomide.minigames.ui.Stats
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Ayo board: top row is pits 11..6 (left to right), bottom row 0..5. */
class AyoBoardView(context: Context, private val onPit: (Int) -> Unit) : BoardView(context, aspect = 0.42f) {

    var game: Oware? = null
    var interactive: Set<Int> = emptySet()

    init {
        contentDescription = "Ayo board"
    }

    private fun pitAt(col: Int, row: Int) = if (row == 1) col else 11 - col

    override fun onDraw(canvas: Canvas) {
        val g = game ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        paint.style = Paint.Style.FILL
        paint.color = c(R.color.wood)
        canvas.drawRoundRect(0f, 0f, w, h, h * 0.12f, h * 0.12f, paint)
        val cw = w / 6
        val ch = h / 2
        val r = min(cw, ch) * 0.4f
        for (row in 0..1) for (col in 0 until 6) {
            val pit = pitAt(col, row)
            val cx = col * cw + cw / 2
            val cy = row * ch + ch / 2
            paint.style = Paint.Style.FILL
            paint.color = c(R.color.wood_dark)
            canvas.drawCircle(cx, cy, r, paint)
            if (pit in interactive) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = r * 0.12f
                paint.color = c(R.color.pit_glow)
                canvas.drawCircle(cx, cy, r * 1.05f, paint)
            }
            if (pit == g.lastMove) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = r * 0.07f
                paint.color = c(R.color.whot_cream)
                canvas.drawCircle(cx, cy, r * 0.85f, paint)
            }
            // Seeds as dots (up to 12), then the count.
            val n = g.pits[pit]
            paint.style = Paint.Style.FILL
            paint.color = c(R.color.seed)
            for (k in 0 until min(n, 12)) {
                val ring = if (k < 6) 0.3f else 0.58f
                val a = (k % 6) * Math.PI / 3 + if (k < 6) 0.0 else Math.PI / 6
                canvas.drawCircle(cx + (r * ring * cos(a)).toFloat(), cy + (r * ring * sin(a)).toFloat(), r * 0.12f, paint)
            }
            paint.color = c(R.color.whot_cream)
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = r * 0.55f
            val ty = if (row == 0) cy - r - r * 0.12f else cy + r + r * 0.5f
            canvas.drawText(n.toString(), cx, ty.coerceIn(paint.textSize, h - 4f), paint)
        }
    }

    override fun onTap(x: Float, y: Float) {
        val col = (x / (width / 6f)).toInt().coerceIn(0, 5)
        val row = (y / (height / 2f)).toInt().coerceIn(0, 1)
        val pit = pitAt(col, row)
        if (pit in interactive) {
            buzz(); onPit(pit)
        }
    }
}

class AyoActivity : BaseScreen() {

    override val screenTitle = "Ayo"

    private var game: Oware? = null
    private var vsCpu = true
    private var recorded = false
    private val handler = Handler(Looper.getMainLooper())
    private val cpuStep = Runnable { cpuTurn() }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(cpuStep)
    }

    override fun onResume() {
        super.onResume()
        scheduleCpu()
    }

    private fun name(p: Int) = if (p == 0) (if (vsCpu) "You" else "Player 1") else (if (vsCpu) "Computer" else "Player 2")

    override fun render() {
        val g = game
        val stats = Stats(this)
        if (g == null) {
            add(ui.card().apply {
                addView(ui.title("Ayo (Oware)"))
                addView(ui.small("Sow seeds anticlockwise from one of your pits. If your last seed makes 2 or 3 in an opponent's pit, you capture them (and the pits just before it with 2 or 3). First to 25 seeds wins."))
                addView(ui.button("Play vs computer") { start(true) })
                addView(ui.button("2 players (pass the phone)") { start(false) })
            })
            add(ui.small("Wins vs computer: ${stats.int("ayo_wins")} of ${stats.int("ayo_games")}"))
            return
        }
        add(ui.text("${name(1)}: ${g.store[1]} captured" + if (g.current == 1 && !g.over) "  ◀ turn" else "", 15f, R.color.text_primary, true))
        val board = AyoBoardView(this) { pit -> if (g.play(pit)) after() }.apply {
            game = g
            val humanTurn = !g.over && (g.current == 0 || !vsCpu)
            interactive = if (humanTurn) g.legalMoves().toSet() else emptySet()
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (8 * resources.displayMetrics.density).toInt()
                bottomMargin = topMargin
            }
        }
        add(board)
        add(ui.text("${name(0)}: ${g.store[0]} captured" + if (g.current == 0 && !g.over) "  ◀ turn" else "", 15f, R.color.text_primary, true))

        add(ui.card().apply {
            when (g.winner()) {
                0 -> addView(ui.text("🎉 ${name(0)} won ${g.store[0]}–${g.store[1]}!", 18f, R.color.success, true))
                1 -> addView(ui.text("${name(1)} won ${g.store[1]}–${g.store[0]}", 18f, R.color.danger, true))
                2 -> addView(ui.text("Draw, 24–24", 18f, R.color.text_primary, true))
                else -> {
                    val who = name(g.current)
                    addView(ui.text(if (vsCpu && g.current == 1) "Computer is thinking…" else "$who: tap a glowing pit", 15f, R.color.accent, true))
                    if (g.lastMove >= 0) addView(ui.small(if (g.lastCapture > 0) "Last move captured ${g.lastCapture} seeds!" else "No capture last move."))
                    if (g.legalMoves().size < g.pits.slice(g.current * 6 until g.current * 6 + 6).count { it > 0 })
                        addView(ui.small("Your opponent has no seeds, so you must give them some."))
                }
            }
        })
        if (g.over) {
            add(ui.button("Play again") { start(vsCpu) })
            add(ui.flatButton("Back to menu") { game = null; refresh() })
        }
        if (vsCpu) add(ui.small("You play the bottom row. Wins vs computer: ${stats.int("ayo_wins")} of ${stats.int("ayo_games")}"))
    }

    private fun start(cpu: Boolean) {
        vsCpu = cpu
        game = Oware()
        recorded = false
        refresh()
        scheduleCpu()
    }

    private fun after() {
        val g = game ?: return
        if (g.over && vsCpu && !recorded) {
            recorded = true
            val s = Stats(this)
            s.inc("ayo_games")
            if (g.winner() == 0) s.inc("ayo_wins")
        }
        refresh()
        scheduleCpu()
    }

    private fun scheduleCpu() {
        val g = game ?: return
        handler.removeCallbacks(cpuStep)
        if (vsCpu && !g.over && g.current == 1) handler.postDelayed(cpuStep, 900)
    }

    private fun cpuTurn() {
        val g = game ?: return
        if (!vsCpu || g.over || g.current != 1) return
        val m = g.bestMove()
        if (m >= 0) g.play(m)
        after()
    }
}
