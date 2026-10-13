package com.ayomide.minigames.games

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.ayomide.minigames.R
import com.ayomide.minigames.ui.BoardView

class SudokuView(context: Context, private val onSelect: (Int) -> Unit) : BoardView(context) {

    var givens = IntArray(81)
    var values = IntArray(81)
    var notes = IntArray(81) // bit v set = pencil mark v
    var conflicts: Set<Int> = emptySet()
    var selected = -1

    init {
        contentDescription = "Sudoku board"
    }

    override fun onDraw(canvas: Canvas) {
        val cell = width / 9f
        paint.style = Paint.Style.FILL
        paint.color = c(R.color.card)
        canvas.drawRect(0f, 0f, cell * 9, cell * 9, paint)

        if (selected >= 0) {
            val sr = selected / 9
            val sc = selected % 9
            val sv = values[selected]
            for (i in 0 until 81) {
                val r = i / 9
                val col = i % 9
                val related = r == sr || col == sc || (r / 3 == sr / 3 && col / 3 == sc / 3)
                val same = sv != 0 && values[i] == sv
                if (i == selected) paint.color = c(R.color.select)
                else if (same) paint.color = c(R.color.same)
                else if (related) paint.color = c(R.color.related)
                else continue
                canvas.drawRect(col * cell, r * cell, (col + 1) * cell, (r + 1) * cell, paint)
            }
        }

        paint.textAlign = Paint.Align.CENTER
        for (i in 0 until 81) {
            val r = i / 9
            val col = i % 9
            val v = values[i]
            val cx = col * cell + cell / 2
            if (v != 0) {
                paint.textSize = cell * 0.62f
                paint.typeface = if (givens[i] != 0) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                paint.color = when {
                    i in conflicts && givens[i] == 0 -> c(R.color.danger)
                    givens[i] != 0 -> c(R.color.text_primary)
                    else -> c(R.color.accent)
                }
                canvas.drawText(v.toString(), cx, r * cell + cell / 2 - (paint.descent() + paint.ascent()) / 2, paint)
            } else if (notes[i] != 0) {
                paint.textSize = cell * 0.27f
                paint.typeface = Typeface.DEFAULT
                paint.color = c(R.color.text_secondary)
                val sub = cell / 3
                for (n in 1..9) {
                    if (notes[i] and (1 shl n) == 0) continue
                    val nx = col * cell + ((n - 1) % 3) * sub + sub / 2
                    val ny = r * cell + ((n - 1) / 3) * sub + sub / 2 - (paint.descent() + paint.ascent()) / 2
                    canvas.drawText(n.toString(), nx, ny, paint)
                }
            }
        }

        paint.color = c(R.color.grid)
        for (k in 0..9) {
            paint.strokeWidth = if (k % 3 == 0) cell * 0.06f else cell * 0.02f
            canvas.drawLine(k * cell, 0f, k * cell, cell * 9, paint)
            canvas.drawLine(0f, k * cell, cell * 9, k * cell, paint)
        }
    }

    override fun onTap(x: Float, y: Float) {
        val cell = width / 9f
        val col = (x / cell).toInt()
        val r = (y / cell).toInt()
        if (col in 0..8 && r in 0..8) {
            buzz()
            onSelect(r * 9 + col)
        }
    }
}
