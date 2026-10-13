package com.ayomide.minigames.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SudokuTest {
    @Test fun generatesValidUniquePuzzles() {
        for (level in Sudoku.Level.values()) {
            val p = Sudoku.generate(level, Random(level.ordinal + 1))
            assertTrue(Sudoku.isSolved(p.solution))
            assertEquals(1, Sudoku.countSolutions(p.givens.copyOf()))
            // Givens agree with the solution.
            for (i in 0 until 81) if (p.givens[i] != 0) assertEquals(p.solution[i], p.givens[i])
            assertTrue(p.givens.count { it != 0 } >= level.clues)
        }
    }

    @Test fun solverMatchesSolution() {
        val p = Sudoku.generate(Sudoku.Level.MEDIUM, Random(9))
        val solved = Sudoku.solve(p.givens)
        assertNotNull(solved)
        assertTrue(solved!!.contentEquals(p.solution))
    }

    @Test fun conflicts() {
        val g = IntArray(81)
        g[0] = 5; g[8] = 5 // same row
        g[40] = 3
        assertEquals(setOf(0, 8), Sudoku.conflicts(g))
        assertFalse(Sudoku.isSolved(g))
    }
}

class WhotTest {
    @Test fun deckHas54Cards() {
        val d = Whot.deck()
        assertEquals(54, d.size)
        assertEquals(5, d.count { it.suit == Suit.WHOT })
        assertEquals(7, d.count { it.suit == Suit.STAR })
    }

    @Test fun dealAndConservation() {
        val g = Whot(3, Random(1))
        assertTrue(g.hands.all { it.size == 5 })
        assertEquals(54, g.hands.sumOf { it.size } + g.market.size + g.pile.size)
        assertFalse(g.top.number in setOf(1, 2, 5, 8, 14, 20))
    }

    @Test fun matchingRules() {
        val g = Whot(2, Random(2))
        val top = g.top
        assertTrue(g.canPlay(WCard(top.suit, if (top.number == 3) 4 else 3)))
        assertTrue(g.canPlay(WCard(Suit.WHOT, 20)))
        val otherSuit = Suit.values().first { it != top.suit && it != Suit.WHOT }
        val otherNumber = (1..14).first { it != top.number }
        assertFalse(g.canPlay(WCard(otherSuit, otherNumber)))
    }

    @Test fun pickTwoAndWhot() {
        val g = Whot(2, Random(3))
        val top = g.top
        val two = WCard(top.suit, 2)
        g.hands[0].add(two)
        val before = g.hands[1].size
        assertTrue(g.play(two))
        assertEquals(before + 2, g.hands[1].size)
        assertEquals(0, g.current) // 2 players: victim loses turn, back to you

        val whot = WCard(Suit.WHOT, 20)
        g.hands[0].add(whot)
        assertFalse(g.play(whot)) // must call a suit
        assertTrue(g.play(whot, Suit.STAR))
        assertEquals(Suit.STAR, g.calledSuit)
        assertEquals(1, g.current)
        assertTrue(g.canPlay(WCard(Suit.STAR, 7)))
        assertFalse(g.canPlay(WCard(Suit.CIRCLE, 20).copy(suit = Suit.CIRCLE, number = 3)))
    }

    @Test fun holdOnAndGeneralMarket() {
        val g = Whot(3, Random(4))
        val top = g.top
        val one = WCard(top.suit, 1)
        g.hands[0].add(one)
        assertTrue(g.play(one))
        assertEquals(0, g.current)
        val fourteen = WCard(top.suit, 14)
        g.hands[0].add(fourteen)
        val s1 = g.hands[1].size; val s2 = g.hands[2].size
        assertTrue(g.play(fourteen))
        assertEquals(s1 + 1, g.hands[1].size)
        assertEquals(s2 + 1, g.hands[2].size)
        assertEquals(0, g.current)
    }

    @Test fun marketAndWin() {
        val g = Whot(2, Random(5))
        val n = g.hands[0].size
        g.goMarket()
        assertEquals(n + 1, g.hands[0].size)
        assertEquals(1, g.current)
        // Give player 1 a single matching card: playing it wins.
        g.hands[1].clear()
        val c = WCard(g.top.suit, if (g.top.number == 3) 4 else 3)
        g.hands[1].add(c)
        assertTrue(g.play(c))
        assertEquals(1, g.winner)
    }

    @Test fun cpuGamesFinishAndConserveCards() {
        for (seed in 1..20) {
            val g = Whot(4, Random(seed))
            var turns = 0
            while (g.winner < 0 && turns < 2000) {
                val choice = g.cpuChoice()
                if (choice == null) g.goMarket() else assertTrue(g.play(choice.first, choice.second))
                assertEquals(54, g.hands.sumOf { it.size } + g.market.size + g.pile.size)
                turns++
            }
        }
    }
}
