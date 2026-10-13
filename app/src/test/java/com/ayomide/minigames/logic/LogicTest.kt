package com.ayomide.minigames.logic

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TicTacToeTest {
    @Test fun detectsResults() {
        assertEquals(1, TicTacToe.result(intArrayOf(1, 1, 1, 0, 2, 2, 0, 0, 0)))
        assertEquals(2, TicTacToe.result(intArrayOf(2, 1, 1, 2, 1, 0, 2, 0, 0)))
        assertEquals(3, TicTacToe.result(intArrayOf(1, 2, 1, 1, 2, 2, 2, 1, 1)))
        assertEquals(0, TicTacToe.result(IntArray(9)))
    }

    @Test fun takesWinAndBlocks() {
        // O to move can win at 5 (row 3,4,5).
        assertEquals(5, TicTacToe.bestMove(intArrayOf(1, 1, 0, 2, 2, 0, 1, 0, 0), 2))
        // O must block X at 2.
        assertEquals(2, TicTacToe.bestMove(intArrayOf(1, 1, 0, 0, 2, 0, 0, 0, 0), 2))
    }

    @Test fun perfectPlayIsADraw() {
        val g = TicTacToe()
        while (g.result() == 0) g.play(TicTacToe.bestMove(g.board, g.turn))
        assertEquals(3, g.result())
    }

    @Test fun cannotPlayTakenCell() {
        val g = TicTacToe()
        assertTrue(g.play(4))
        assertFalse(g.play(4))
        assertEquals(1, g.board[4])
    }
}

class MemoryTest {
    @Test fun matchAndMismatch() {
        val m = MemoryGame(8, Random(3))
        val a = 0
        val pair = m.cards.indices.first { it != a && m.cards[it] == m.cards[a] }
        val other = m.cards.indices.first { m.cards[it] != m.cards[a] }
        assertEquals(MemoryGame.Flip.FIRST, m.flip(a))
        assertEquals(MemoryGame.Flip.MISMATCH, m.flip(other))
        assertTrue(m.isWaiting)
        assertEquals(MemoryGame.Flip.IGNORED, m.flip(pair)) // must hide first
        m.hideMismatch()
        assertFalse(m.faceUp[a])
        assertEquals(MemoryGame.Flip.FIRST, m.flip(a))
        assertEquals(MemoryGame.Flip.MATCH, m.flip(pair))
        assertEquals(2, m.moves)
        assertTrue(m.matched[a] && m.matched[pair])
    }

    @Test fun deckHasPairs() {
        val m = MemoryGame(8, Random(1))
        assertEquals(16, m.cards.size)
        assertTrue(m.cards.groupingBy { it }.eachCount().values.all { it == 2 })
    }
}

class OwareTest {
    @Test fun sowsCounterClockwise() {
        val g = Oware()
        assertTrue(g.play(2)) // 4 seeds into 3,4,5,6
        assertArrayEquals(intArrayOf(4, 4, 0, 5, 5, 5, 5, 4, 4, 4, 4, 4), g.pits)
        assertEquals(1, g.current)
    }

    @Test fun rejectsOpponentPitAndEmptyPit() {
        val g = Oware()
        assertFalse(g.play(7))
        g.play(2)
        assertFalse(g.play(2)) // not player 1's pit
    }

    @Test fun capturesChain() {
        // Player 0 plays pit 5 (2 seeds) → lands 6,7. Pits 6 and 7 become 2 and 3.
        val pits = intArrayOf(0, 0, 0, 0, 0, 2, 1, 2, 4, 4, 4, 4)
        val g = Oware(pits, IntArray(2), 0)
        g.play(5)
        assertEquals(5, g.store[0])
        assertEquals(0, g.pits[6])
        assertEquals(0, g.pits[7])
    }

    @Test fun grandSlamForfeited() {
        // Capturing would empty player 1's side entirely → no capture.
        // Player 0 keeps 3 seeds in pit 0 so the game continues afterwards.
        val pits = intArrayOf(3, 0, 0, 0, 0, 2, 1, 2, 0, 0, 0, 0)
        val g = Oware(pits, intArrayOf(10, 10), 0)
        g.play(5)
        assertEquals(10, g.store[0])
        assertEquals(2, g.pits[6])
        assertEquals(3, g.pits[7])
    }

    @Test fun mustFeedEmptyOpponent() {
        val pits = intArrayOf(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0)
        val g = Oware(pits, intArrayOf(20, 20), 0)
        assertEquals(listOf(5), g.legalMoves())
    }

    @Test fun cannotFeedEndsGame() {
        val pits = intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val g = Oware(pits, intArrayOf(23, 22), 1) // player 1 to move with no seeds
        // Player 1 has no moves; game is only checked after a move, so simulate from player 0.
        val g2 = Oware(intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1), intArrayOf(23, 21), 1)
        g2.play(11) // sows into pit 0 → player 0 now has 2 seeds and must feed; can't
        assertTrue(g2.over)
        assertEquals(25, g2.store[0])
        assertEquals(0, g2.winner())
        assertTrue(g.legalMoves().isEmpty())
    }

    @Test fun seedsAreConserved() {
        val g = Oware()
        val r = Random(7)
        var n = 0
        while (!g.over && n < 400) {
            val m = g.legalMoves()
            g.play(m[r.nextInt(m.size)]); n++
            assertEquals(48, g.pits.sum() + g.store.sum())
        }
    }

    @Test fun cpuTakesCapture() {
        val pits = intArrayOf(0, 0, 0, 0, 1, 2, 1, 2, 4, 4, 4, 4)
        val g = Oware(pits, IntArray(2), 0)
        assertEquals(5, g.bestMove())
    }
}
