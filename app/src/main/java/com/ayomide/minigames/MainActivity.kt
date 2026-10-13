package com.ayomide.minigames

import android.content.Intent
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import com.ayomide.minigames.games.AyoActivity
import com.ayomide.minigames.games.MemoryActivity
import com.ayomide.minigames.games.SudokuActivity
import com.ayomide.minigames.games.TicTacToeActivity
import com.ayomide.minigames.games.WhotActivity
import com.ayomide.minigames.ui.BaseScreen
import com.ayomide.minigames.ui.Stats
import com.ayomide.minigames.ui.dp
import com.ayomide.minigames.ui.mmss

class MainActivity : BaseScreen() {

    override val screenTitle = "Mini Games"
    override val showBack = false

    private class Game(val icon: String, val name: String, val blurb: String, val stat: String, val target: Class<*>)

    override fun render() {
        val s = Stats(this)
        val sudokuBest = s.long("sd_best_EASY")
        val ttt = (0..2).sumOf { s.int("ttt_${it}_w") }
        val memBest = s.long("mem_best_8")
        val games = listOf(
            Game("🔢", "Sudoku", "Easy, medium and hard puzzles",
                if (s.str("sd_givens") != null) "Puzzle in progress" else if (sudokuBest > 0) "Best easy: ${mmss(sudokuBest)}" else "Solved: ${s.int("sd_solved")}",
                SudokuActivity::class.java),
            Game("🃏", "Whot!", "Naija Whot vs 1–3 computers", "Wins: ${s.int("whot_wins")}", WhotActivity::class.java),
            Game("🟤", "Ayo", "Oware vs computer or a friend", "Wins: ${s.int("ayo_wins")}", AyoActivity::class.java),
            Game("❌", "Tic-Tac-Toe", "Easy, medium, hard or 2 players", "Wins: $ttt", TicTacToeActivity::class.java),
            Game("🧠", "Memory Match", "Find the pairs", if (memBest > 0) "Best: $memBest moves" else "Not played yet", MemoryActivity::class.java),
        )
        add(ui.small("Offline · no ads · no internet"))
        for (g in games) {
            add(ui.card { startActivity(Intent(this, g.target)) }.apply {
                addView(ui.row().apply {
                    addView(ui.text(g.icon, 34f).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(60), ViewGroup.LayoutParams.WRAP_CONTENT)
                        gravity = Gravity.CENTER
                    })
                    addView(ui.column().apply {
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        addView(ui.title(g.name))
                        addView(ui.small(g.blurb))
                        addView(ui.text(g.stat, 13f, R.color.accent, true))
                    })
                    addView(ui.text("›", 26f).apply {
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    })
                })
            })
        }
    }
}
