package com.ayomide.minigames.logic

import kotlin.random.Random

/** Board cells: 0 empty, 1 = X, 2 = O. X always starts. */
class TicTacToe {

    val board = IntArray(9)
    var turn = 1
        private set

    companion object {
        val LINES = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6),
        )

        /** 1 or 2 for a winner, 3 for draw, 0 while playing. */
        fun result(b: IntArray): Int {
            for (l in LINES) {
                val v = b[l[0]]
                if (v != 0 && v == b[l[1]] && v == b[l[2]]) return v
            }
            return if (b.all { it != 0 }) 3 else 0
        }

        fun winningLine(b: IntArray): IntArray? =
            LINES.firstOrNull { l -> b[l[0]] != 0 && b[l[0]] == b[l[1]] && b[l[0]] == b[l[2]] }

        /** Perfect play via minimax; prefers faster wins and slower losses. */
        fun bestMove(b: IntArray, player: Int): Int {
            var best = -1
            var bestScore = Int.MIN_VALUE
            for (i in 0 until 9) {
                if (b[i] != 0) continue
                b[i] = player
                val s = -negamax(b, 3 - player, 1)
                b[i] = 0
                if (s > bestScore) {
                    bestScore = s; best = i
                }
            }
            return best
        }

        private fun negamax(b: IntArray, player: Int, depth: Int): Int {
            when (val r = result(b)) {
                3 -> return 0
                0 -> Unit
                else -> return if (r == player) 10 - depth else depth - 10
            }
            var best = Int.MIN_VALUE
            for (i in 0 until 9) {
                if (b[i] != 0) continue
                b[i] = player
                best = maxOf(best, -negamax(b, 3 - player, depth + 1))
                b[i] = 0
            }
            return best
        }

        /** Easy: random most of the time. Medium: sometimes perfect. Hard: always perfect. */
        fun cpuMove(b: IntArray, player: Int, level: Int, random: Random = Random.Default): Int {
            val free = (0 until 9).filter { b[it] == 0 }
            val perfectChance = when (level) {
                0 -> 0.25
                1 -> 0.7
                else -> 1.0
            }
            return if (random.nextDouble() < perfectChance) bestMove(b, player) else free[random.nextInt(free.size)]
        }
    }

    fun play(i: Int): Boolean {
        if (i !in 0..8 || board[i] != 0 || result(board) != 0) return false
        board[i] = turn
        turn = 3 - turn
        return true
    }

    fun result() = result(board)
}
