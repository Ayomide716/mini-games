package com.ayomide.minigames.logic

/**
 * Ayo / Oware (abapa rules). Pits 0–5 belong to player 0 (bottom row, left to
 * right), pits 6–11 to player 1 (top row, right to left), so sowing goes
 * counter-clockwise by increasing index.
 *
 * - Sow one seed per pit, skipping the starting pit on laps of 12+ seeds.
 * - If the last seed lands in an opponent pit making 2 or 3, capture it and
 *   any directly preceding opponent pits that also hold 2 or 3.
 * - A capture that would take all of the opponent's seeds is forfeited.
 * - If the opponent has no seeds you must give them some if you can.
 * - 25+ seeds wins; if the player to move has no legal move, each player
 *   keeps the seeds on their own side and the game ends.
 */
class Oware(
    val pits: IntArray = IntArray(12) { 4 },
    val store: IntArray = IntArray(2),
    var current: Int = 0,
) {
    var lastMove = -1
        private set
    var lastCapture = 0
        private set
    var over = false
        private set
    private var movesWithoutCapture = 0

    fun copy() = Oware(pits.copyOf(), store.copyOf(), current).also {
        it.over = over; it.movesWithoutCapture = movesWithoutCapture
    }

    fun owner(pit: Int) = if (pit < 6) 0 else 1
    fun sideSeeds(player: Int) = (0 until 6).sumOf { pits[player * 6 + it] }

    /** Where the last seed of [pit] would land, and a board after sowing (no capture). */
    private fun sow(b: IntArray, pit: Int): Int {
        var seeds = b[pit]
        b[pit] = 0
        var i = pit
        while (seeds > 0) {
            i = (i + 1) % 12
            if (i == pit) continue
            b[i]++
            seeds--
        }
        return i
    }

    fun legalMoves(player: Int = current): List<Int> {
        if (over) return emptyList()
        val mine = (player * 6 until player * 6 + 6).filter { pits[it] > 0 }
        val opp = 1 - player
        if (sideSeeds(opp) > 0) return mine
        // Opponent is empty: only moves that reach them are allowed.
        return mine.filter { p ->
            val b = pits.copyOf()
            sow(b, p)
            (opp * 6 until opp * 6 + 6).any { b[it] > 0 }
        }
    }

    fun play(pit: Int): Boolean {
        if (pit !in legalMoves()) return false
        val opp = 1 - current
        val b = pits.copyOf()
        var i = sow(b, pit)
        var captured = 0
        val takenFrom = ArrayList<Int>()
        while (owner(i) == opp && (b[i] == 2 || b[i] == 3)) {
            captured += b[i]; takenFrom += i
            i = (i + 11) % 12
        }
        // Grand slam: capturing everything the opponent has is forfeited.
        val oppLeft = (opp * 6 until opp * 6 + 6).sumOf { if (it in takenFrom) 0 else b[it] }
        if (captured > 0 && oppLeft == 0) {
            captured = 0; takenFrom.clear()
        }
        for (t in takenFrom) b[t] = 0
        b.copyInto(pits)
        store[current] += captured
        lastMove = pit
        lastCapture = captured
        movesWithoutCapture = if (captured > 0) 0 else movesWithoutCapture + 1
        current = opp
        checkEnd()
        return true
    }

    private fun checkEnd() {
        if (store[0] > 24 || store[1] > 24 || (store[0] == 24 && store[1] == 24)) {
            over = true; return
        }
        // Endless cycling with few seeds: end and keep own side.
        if (legalMoves().isEmpty() || (movesWithoutCapture >= 60 && pits.sum() <= 10)) {
            for (p in 0..1) {
                store[p] += sideSeeds(p)
                for (k in 0 until 6) pits[p * 6 + k] = 0
            }
            over = true
        }
    }

    /** 0 or 1 for the winner, 2 for a draw, -1 while playing. */
    fun winner(): Int = when {
        !over -> -1
        store[0] > store[1] -> 0
        store[1] > store[0] -> 1
        else -> 2
    }

    /** CPU: maximise own capture minus the opponent's best reply (2-ply). */
    fun bestMove(): Int {
        val me = current
        var best = -1
        var bestScore = Int.MIN_VALUE
        for (m in legalMoves()) {
            val g = copy()
            g.play(m)
            var s = (g.store[me] - store[me]) * 10
            if (!g.over) {
                var worst = 0
                for (r in g.legalMoves()) {
                    val h = g.copy()
                    h.play(r)
                    worst = maxOf(worst, h.store[1 - me] - g.store[1 - me])
                }
                s -= worst * 10
            } else if (g.winner() == me) {
                s += 1000
            }
            // Mild preference for keeping seeds on our side.
            s += g.sideSeeds(me)
            if (s > bestScore) {
                bestScore = s; best = m
            }
        }
        return best
    }
}
