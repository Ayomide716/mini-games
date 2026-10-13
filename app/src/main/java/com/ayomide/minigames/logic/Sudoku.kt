package com.ayomide.minigames.logic

import kotlin.random.Random

/** Sudoku generator/solver. Grids are 81 ints, row-major, 0 = empty. */
object Sudoku {

    enum class Level(val label: String, val clues: Int) {
        EASY("Easy", 40), MEDIUM("Medium", 32), HARD("Hard", 26)
    }

    data class Puzzle(val givens: IntArray, val solution: IntArray)

    fun canPlace(g: IntArray, idx: Int, v: Int): Boolean {
        val r = idx / 9
        val c = idx % 9
        for (k in 0 until 9) {
            if (k != c && g[r * 9 + k] == v) return false
            if (k != r && g[k * 9 + c] == v) return false
        }
        val br = r / 3 * 3
        val bc = c / 3 * 3
        for (y in br until br + 3) for (x in bc until bc + 3) {
            val i = y * 9 + x
            if (i != idx && g[i] == v) return false
        }
        return true
    }

    /** Fills [g] in place with a random full solution. */
    private fun fill(g: IntArray, random: Random): Boolean {
        val idx = g.indexOfFirst { it == 0 }
        if (idx < 0) return true
        for (v in (1..9).shuffled(random)) {
            if (canPlace(g, idx, v)) {
                g[idx] = v
                if (fill(g, random)) return true
                g[idx] = 0
            }
        }
        return false
    }

    /** Counts solutions up to [limit], choosing the most constrained cell first. */
    fun countSolutions(g: IntArray, limit: Int = 2): Int {
        var best = -1
        var bestCount = 10
        for (i in 0 until 81) {
            if (g[i] != 0) continue
            var n = 0
            for (v in 1..9) if (canPlace(g, i, v)) n++
            if (n == 0) return 0
            if (n < bestCount) {
                bestCount = n; best = i
            }
        }
        if (best < 0) return 1
        var total = 0
        for (v in 1..9) {
            if (!canPlace(g, best, v)) continue
            g[best] = v
            total += countSolutions(g, limit - total)
            g[best] = 0
            if (total >= limit) break
        }
        return total
    }

    fun solve(g: IntArray): IntArray? {
        val copy = g.copyOf()
        return if (solveInPlace(copy)) copy else null
    }

    private fun solveInPlace(g: IntArray): Boolean {
        val idx = g.indexOfFirst { it == 0 }
        if (idx < 0) return true
        for (v in 1..9) {
            if (canPlace(g, idx, v)) {
                g[idx] = v
                if (solveInPlace(g)) return true
                g[idx] = 0
            }
        }
        return false
    }

    /** Random puzzle with a unique solution and about [level].clues givens. */
    fun generate(level: Level, random: Random = Random.Default): Puzzle {
        val solution = IntArray(81)
        fill(solution, random)
        val puzzle = solution.copyOf()
        var filled = 81
        for (i in (0 until 81).shuffled(random)) {
            if (filled <= level.clues) break
            val keep = puzzle[i]
            puzzle[i] = 0
            if (countSolutions(puzzle.copyOf()) != 1) puzzle[i] = keep else filled--
        }
        return Puzzle(puzzle, solution)
    }

    /** Cells whose value clashes with another in its row, column or box. */
    fun conflicts(g: IntArray): Set<Int> =
        (0 until 81).filter { g[it] != 0 && !canPlace(g, it, g[it]) }.toSet()

    fun isSolved(g: IntArray) = g.none { it == 0 } && conflicts(g).isEmpty()
}
