package com.ayomide.minigames.games

import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Toast
import com.ayomide.minigames.R
import com.ayomide.minigames.logic.Suit
import com.ayomide.minigames.logic.WCard
import com.ayomide.minigames.logic.Whot
import com.ayomide.minigames.ui.BaseScreen
import com.ayomide.minigames.ui.Stats
import com.ayomide.minigames.ui.dp

class WhotActivity : BaseScreen() {

    override val screenTitle = "Whot!"

    private var game: Whot? = null
    private val log = ArrayDeque<String>()
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

    override fun render() {
        val g = game
        if (g == null) {
            renderMenu(); return
        }
        val stats = Stats(this)

        // Opponents
        add(ui.row().apply {
            for (p in 1 until g.players) {
                addView(ui.card().apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = dp(4); marginStart = dp(4); topMargin = dp(8)
                    }
                    gravity = Gravity.CENTER_HORIZONTAL
                    val turn = g.current == p && g.winner < 0
                    addView(ui.text((if (turn) "▶ " else "") + g.name(p), 14f, if (turn) R.color.accent else R.color.text_primary, true).apply { gravity = Gravity.CENTER })
                    val n = g.hands[p].size
                    addView(ui.small(if (n == 1) "LAST CARD!" else "$n cards").apply { gravity = Gravity.CENTER })
                })
            }
        })

        // Table: market + top card
        add(ui.row().apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(8))
            addView(ui.column().apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(24) }
                gravity = Gravity.CENTER_HORIZONTAL
                addView(WhotCardView(this@WhotActivity, null, faceDown = true).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(80), dp(112))
                    isClickable = true
                    setOnClickListener { if (myTurn()) { g.goMarket(); after() } }
                })
                addView(ui.small("Market (${g.market.size})").apply { gravity = Gravity.CENTER })
            })
            addView(ui.column().apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.CENTER_HORIZONTAL
                addView(WhotCardView(this@WhotActivity, g.top).apply { layoutParams = LinearLayout.LayoutParams(dp(80), dp(112)) })
                val called = g.calledSuit
                addView(ui.text(if (called != null) "Asked for: ${called.symbol} ${called.label}" else "Top card", 13f,
                    if (called != null) R.color.accent else R.color.text_secondary, called != null).apply { gravity = Gravity.CENTER })
            })
        })

        // Status / log
        add(ui.card().apply {
            when {
                g.winner == 0 -> addView(ui.text("🎉 You won!", 20f, R.color.success, true))
                g.winner > 0 -> addView(ui.text("${g.name(g.winner)} won this round", 18f, R.color.danger, true))
                myTurn() -> addView(ui.text("Your turn: play a matching card or tap the market", 15f, R.color.accent, true))
                else -> addView(ui.text("${g.name(g.current)} is thinking…", 15f, R.color.text_secondary, true))
            }
            for (e in log) addView(ui.small("• $e"))
        })

        if (g.winner >= 0) {
            add(ui.button("Play again") { newGame(g.players - 1) })
            add(ui.flatButton("Change number of players") { game = null; refresh() })
        }

        // Your hand
        add(ui.section("Your cards (${g.hands[0].size})"))
        val strip = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(8)) }
        for (card in g.hands[0].sortedWith(compareBy<WCard>({ it.suit.ordinal }, { it.number }))) {
            strip.addView(WhotCardView(this, card).apply {
                layoutParams = LinearLayout.LayoutParams(dp(72), dp(100)).apply { marginEnd = dp(6) }
                dimmed = myTurn() && !g.canPlay(card)
                isClickable = true
                setOnClickListener { tapCard(card) }
            })
        }
        add(HorizontalScrollView(this).apply { addView(strip); isHorizontalScrollBarEnabled = false })

        add(ui.small("Wins: ${stats.int("whot_wins")} of ${stats.int("whot_games")} games. Specials: 1 hold on · 2 pick two · 5 pick three · 8 suspension · 14 general market · 20 Whot (ask for a suit)."))
    }

    private fun renderMenu() {
        add(ui.card().apply {
            addView(ui.title("Naija Whot"))
            addView(ui.small("Match the top card by shape or number. First to finish their cards wins."))
            addView(ui.button("Play vs 1 computer") { newGame(1) })
            addView(ui.button("Play vs 2 computers") { newGame(2) })
            addView(ui.button("Play vs 3 computers") { newGame(3) })
        })
        val s = Stats(this)
        add(ui.small("Wins: ${s.int("whot_wins")} of ${s.int("whot_games")} games"))
    }

    private fun newGame(cpus: Int) {
        handler.removeCallbacks(cpuStep)
        game = Whot(cpus + 1)
        log.clear()
        refresh()
        scheduleCpu()
    }

    private fun myTurn() = game?.let { it.winner < 0 && it.current == 0 } == true

    private fun tapCard(card: WCard) {
        val g = game ?: return
        if (!myTurn()) return
        if (!g.canPlay(card)) {
            val called = g.calledSuit
            Toast.makeText(this,
                if (called != null) "Play a ${called.label} or a Whot" else "Match ${g.top.suit.label} or ${g.top.number}",
                Toast.LENGTH_SHORT).show()
            return
        }
        if (card.suit == Suit.WHOT) {
            val suits = Suit.values().filter { it != Suit.WHOT }
            AlertDialog.Builder(this)
                .setTitle("Ask for which shape?")
                .setItems(suits.map { "${it.symbol}  ${it.label}" }.toTypedArray()) { _, i ->
                    if (g.play(card, suits[i])) after()
                }
                .show()
        } else if (g.play(card)) {
            after()
        }
    }

    private fun after() {
        val g = game ?: return
        if (g.lastEvent.isNotBlank()) {
            log.addLast(g.lastEvent)
            while (log.size > 4) log.removeFirst()
        }
        if (g.winner >= 0) {
            val s = Stats(this)
            s.inc("whot_games")
            if (g.winner == 0) s.inc("whot_wins")
        }
        refresh()
        scheduleCpu()
    }

    private fun scheduleCpu() {
        val g = game ?: return
        handler.removeCallbacks(cpuStep)
        if (g.winner < 0 && g.current != 0) handler.postDelayed(cpuStep, 1000)
    }

    private fun cpuTurn() {
        val g = game ?: return
        if (g.winner >= 0 || g.current == 0) return
        val choice = g.cpuChoice()
        if (choice == null) g.goMarket() else g.play(choice.first, choice.second)
        after()
    }
}
