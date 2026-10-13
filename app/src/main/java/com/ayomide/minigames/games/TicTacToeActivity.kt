package com.ayomide.minigames.games

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.LinearLayout
import com.ayomide.minigames.R
import com.ayomide.minigames.logic.TicTacToe
import com.ayomide.minigames.ui.BaseScreen
import com.ayomide.minigames.ui.BoardView
import com.ayomide.minigames.ui.Stats

class TicTacToeView(context: Context, private val onCell: (Int) -> Unit) : BoardView(context) {

    var board = IntArray(9)
    var active = true

    init {
        contentDescription = "Tic-tac-toe board"
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val cell = w / 3
        paint.style = Paint.Style.FILL
        paint.color = c(R.color.card)
        canvas.drawRoundRect(0f, 0f, w, w, cell * 0.15f, cell * 0.15f, paint)
        paint.color = c(R.color.grid)
        paint.strokeWidth = cell * 0.04f
        paint.strokeCap = Paint.Cap.ROUND
        for (k in 1..2) {
            canvas.drawLine(k * cell, cell * 0.15f, k * cell, w - cell * 0.15f, paint)
            canvas.drawLine(cell * 0.15f, k * cell, w - cell * 0.15f, k * cell, paint)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = cell * 0.1f
        val pad = cell * 0.24f
        for (i in 0 until 9) {
            val x = (i % 3) * cell
            val y = (i / 3) * cell
            when (board[i]) {
                1 -> {
                    paint.color = c(R.color.accent)
                    canvas.drawLine(x + pad, y + pad, x + cell - pad, y + cell - pad, paint)
                    canvas.drawLine(x + cell - pad, y + pad, x + pad, y + cell - pad, paint)
                }
                2 -> {
                    paint.color = c(R.color.o_color)
                    canvas.drawCircle(x + cell / 2, y + cell / 2, cell / 2 - pad, paint)
                }
            }
        }
        TicTacToe.winningLine(board)?.let { l ->
            paint.color = c(R.color.success)
            paint.strokeWidth = cell * 0.08f
            fun center(i: Int) = Pair((i % 3) * cell + cell / 2, (i / 3) * cell + cell / 2)
            val (ax, ay) = center(l[0]); val (bx, by) = center(l[2])
            canvas.drawLine(ax, ay, bx, by, paint)
        }
    }

    override fun onTap(x: Float, y: Float) {
        if (!active) return
        val cell = width / 3f
        val i = (y / cell).toInt().coerceIn(0, 2) * 3 + (x / cell).toInt().coerceIn(0, 2)
        if (board[i] == 0) {
            buzz(); onCell(i)
        }
    }
}

class TicTacToeActivity : BaseScreen() {

    override val screenTitle = "Tic-Tac-Toe"

    private val modes = listOf("vs Computer · Easy", "vs Computer · Medium", "vs Computer · Hard", "2 players")
    private var mode = 1
    private var game = TicTacToe()
    /** In CPU modes the human alternates between X (first) and O each round. */
    private var humanIsX = true
    private var recorded = false
    private val handler = Handler(Looper.getMainLooper())
    private val cpuStep = Runnable { cpuTurn() }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        mode = Stats(this).int("ttt_mode", 1).coerceIn(0, 3)
        super.onCreate(savedInstanceState)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(cpuStep)
    }

    override fun onResume() {
        super.onResume()
        scheduleCpu()
    }

    private val vsCpu get() = mode < 3
    private val human get() = if (humanIsX) 1 else 2

    override fun render() {
        val stats = Stats(this)
        add(ui.spinner(modes, mode).apply {
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: android.widget.AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                    if (pos != mode) {
                        mode = pos; stats.setInt("ttt_mode", pos); newRound(resetSide = true)
                    }
                }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) = Unit
            }
        })
        val r = game.result()
        val status = when {
            r == 3 -> "It's a draw"
            r != 0 && !vsCpu -> "${if (r == 1) "X" else "O"} wins!"
            r != 0 && r == human -> "🎉 You win!"
            r != 0 -> "Computer wins"
            !vsCpu -> "${if (game.turn == 1) "X" else "O"} to play"
            game.turn == human -> "Your turn (${if (humanIsX) "X" else "O"})"
            else -> "Computer is thinking…"
        }
        add(ui.text(status, 18f, if (r != 0 && vsCpu && r == human) R.color.success else R.color.text_primary, true).apply {
            gravity = android.view.Gravity.CENTER; setPadding(0, 24, 0, 8)
        })
        add(TicTacToeView(this) { i -> if (game.play(i)) after() }.apply {
            board = game.board
            active = r == 0 && (!vsCpu || game.turn == human)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })
        if (r != 0) add(ui.button("Next round") { newRound(resetSide = false) })
        if (vsCpu) {
            val k = "ttt_${mode}_"
            add(ui.card().apply {
                addView(ui.title(modes[mode]))
                addView(ui.primary("You ${stats.int(k + "w")} · Draws ${stats.int(k + "d")} · Computer ${stats.int(k + "l")}"))
                if (mode == 2) addView(ui.small("Hard plays perfectly. A draw is a good result!"))
            })
        }
    }

    private fun newRound(resetSide: Boolean) {
        handler.removeCallbacks(cpuStep)
        humanIsX = if (resetSide) true else !humanIsX
        game = TicTacToe()
        recorded = false
        refresh()
        scheduleCpu()
    }

    private fun after() {
        val r = game.result()
        if (r != 0 && vsCpu && !recorded) {
            recorded = true
            val k = "ttt_${mode}_"
            Stats(this).inc(k + when (r) { 3 -> "d"; human -> "w"; else -> "l" })
        }
        refresh()
        scheduleCpu()
    }

    private fun scheduleCpu() {
        handler.removeCallbacks(cpuStep)
        if (vsCpu && game.result() == 0 && game.turn != human) handler.postDelayed(cpuStep, 500)
    }

    private fun cpuTurn() {
        if (!vsCpu || game.result() != 0 || game.turn == human) return
        game.play(TicTacToe.cpuMove(game.board, game.turn, mode))
        after()
    }
}
