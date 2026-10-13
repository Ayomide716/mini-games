package com.ayomide.minigames.games

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.ayomide.minigames.R
import com.ayomide.minigames.logic.Sudoku
import com.ayomide.minigames.ui.BaseScreen
import com.ayomide.minigames.ui.Stats
import com.ayomide.minigames.ui.dp
import com.ayomide.minigames.ui.mmss

class SudokuActivity : BaseScreen() {

    override val screenTitle = "Sudoku"

    private lateinit var stats: Stats
    private var level = Sudoku.Level.EASY
    private var givens: IntArray? = null
    private var solution = IntArray(81)
    private var values = IntArray(81)
    private var notes = IntArray(81)
    private var elapsed = 0L // seconds, before the current resume
    private var resumedAt = 0L
    private var hints = 0
    private var notesMode = false
    private var generating = false

    private var board: SudokuView? = null
    private var timerText: TextView? = null
    private var notesButton: Button? = null
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            timerText?.text = "⏱ ${mmss(seconds())}"
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        stats = Stats(this)
        load()
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        resumedAt = System.currentTimeMillis()
        super.onResume()
        handler.post(tick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
        elapsed = seconds()
        save()
    }

    private fun seconds(): Long =
        if (givens == null || resumedAt == 0L) elapsed else elapsed + (System.currentTimeMillis() - resumedAt) / 1000

    // ---- persistence -------------------------------------------------------

    private fun enc(a: IntArray) = a.joinToString(",")
    private fun dec(s: String?): IntArray? = s?.split(",")?.mapNotNull { it.toIntOrNull() }?.toIntArray()?.takeIf { it.size == 81 }

    private fun load() {
        val g = dec(stats.str("sd_givens")) ?: return
        givens = g
        solution = dec(stats.str("sd_solution")) ?: return clearSaved()
        values = dec(stats.str("sd_values")) ?: g.copyOf()
        notes = dec(stats.str("sd_notes")) ?: IntArray(81)
        level = Sudoku.Level.values().getOrElse(stats.int("sd_level")) { Sudoku.Level.EASY }
        elapsed = stats.long("sd_elapsed")
        hints = stats.int("sd_hints")
    }

    private fun save() {
        val g = givens ?: return
        stats.setStr("sd_givens", enc(g))
        stats.setStr("sd_solution", enc(solution))
        stats.setStr("sd_values", enc(values))
        stats.setStr("sd_notes", enc(notes))
        stats.setInt("sd_level", level.ordinal)
        stats.setLong("sd_elapsed", elapsed)
        stats.setInt("sd_hints", hints)
    }

    private fun clearSaved() {
        givens = null
        stats.setStr("sd_givens", null)
    }

    // ---- UI ----------------------------------------------------------------

    override fun render() {
        board = null; timerText = null
        if (generating) {
            add(ui.empty("Creating a ${level.label.lowercase()} puzzle…"))
            return
        }
        if (givens == null) {
            renderMenu(); return
        }
        renderGame()
    }

    private fun renderMenu() {
        add(ui.card().apply {
            addView(ui.title("New puzzle"))
            addView(ui.small("Fill every row, column and 3×3 box with 1–9. Each puzzle has exactly one solution."))
            for (l in Sudoku.Level.values()) {
                val best = stats.long("sd_best_${l.name}")
                addView(ui.button(l.label + if (best > 0) "   ·   best ${mmss(best)}" else "") { start(l) })
            }
        })
        add(ui.small("Solved: ${stats.int("sd_solved")}"))
    }

    private fun start(l: Sudoku.Level) {
        level = l
        generating = true
        refresh()
        Thread {
            val p = Sudoku.generate(l)
            handler.post {
                givens = p.givens
                solution = p.solution
                values = p.givens.copyOf()
                notes = IntArray(81)
                elapsed = 0; hints = 0; resumedAt = System.currentTimeMillis()
                generating = false
                notesMode = false
                save()
                refresh()
            }
        }.start()
    }

    private fun renderGame() {
        val g = givens!!
        add(ui.row().apply {
            addView(ui.title("${level.label}${if (hints > 0) " · $hints hint(s)" else ""}").apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            val t = ui.text("⏱ ${mmss(seconds())}", 16f, R.color.text_primary, true).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            timerText = t
            addView(t)
        })
        val v = SudokuView(this) { idx -> board?.selected = idx; board?.invalidate() }.apply {
            givens = g; values = this@SudokuActivity.values; notes = this@SudokuActivity.notes
            conflicts = Sudoku.conflicts(this@SudokuActivity.values)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
        }
        board = v
        add(v)

        for (rowDigits in listOf(1..5, 6..9)) {
            add(ui.row().apply {
                for (d in rowDigits) addView(padButton(d.toString()) { input(d) })
                if (rowDigits.first == 6) addView(padButton("⌫") { input(0) })
            })
        }
        add(ui.row().apply {
            val nb = ui.button(if (notesMode) "✏ Notes: ON" else "✏ Notes: off") {
                notesMode = !notesMode
                notesButton?.text = if (notesMode) "✏ Notes: ON" else "✏ Notes: off"
            }.apply { layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
            notesButton = nb
            addView(nb)
            addView(ui.button("💡 Hint") { hint() }.apply { layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) })
        })
        add(ui.flatButton("New puzzle") {
            ui.confirm("Start a new puzzle?", "Your current puzzle will be lost.", "New puzzle") {
                clearSaved(); refresh()
            }
        })
        add(ui.small("Tap a square, then a number. Red numbers clash with another in the same row, column or box."))
    }

    private fun padButton(label: String, onClick: () -> Unit) = ui.button(label, onClick).apply {
        textSize = 20f
        minWidth = 0
        setPadding(0, dp(10), 0, dp(10))
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        gravity = Gravity.CENTER
    }

    private fun input(d: Int) {
        val v = board ?: return
        val i = v.selected
        val g = givens ?: return
        if (i < 0 || g[i] != 0) return
        if (notesMode && d != 0) {
            if (values[i] == 0) notes[i] = notes[i] xor (1 shl d)
        } else {
            values[i] = if (values[i] == d) 0 else d
            if (d == 0) notes[i] = 0
            if (values[i] != 0) clearNotesFor(i, values[i])
        }
        afterChange()
    }

    /** Removes pencil marks of [d] from the cell's row, column and box. */
    private fun clearNotesFor(i: Int, d: Int) {
        val r = i / 9; val c = i % 9
        for (k in 0 until 81) {
            val kr = k / 9; val kc = k % 9
            if (kr == r || kc == c || (kr / 3 == r / 3 && kc / 3 == c / 3)) notes[k] = notes[k] and (1 shl d).inv()
        }
    }

    private fun hint() {
        val v = board ?: return
        val g = givens ?: return
        var i = v.selected
        if (i < 0 || g[i] != 0 || values[i] == solution[i]) {
            i = (0 until 81).firstOrNull { values[it] != solution[it] } ?: return
            v.selected = i
        }
        values[i] = solution[i]
        notes[i] = 0
        clearNotesFor(i, values[i])
        hints++
        afterChange()
    }

    private fun afterChange() {
        val v = board ?: return
        v.conflicts = Sudoku.conflicts(values)
        v.invalidate()
        if (Sudoku.isSolved(values)) finishPuzzle()
    }

    private fun finishPuzzle() {
        val secs = seconds()
        stats.inc("sd_solved")
        val record = hints == 0 && stats.setBestLow("sd_best_${level.name}", secs.coerceAtLeast(1))
        clearSaved()
        android.app.AlertDialog.Builder(this)
            .setTitle("🎉 Solved!")
            .setMessage("${level.label} in ${mmss(secs)}" + (if (hints > 0) " with $hints hint(s)." else ".") + if (record) "\nNew best time!" else "")
            .setPositiveButton("Play again") { _, _ -> refresh() }
            .setOnDismissListener { refresh() }
            .show()
    }
}
