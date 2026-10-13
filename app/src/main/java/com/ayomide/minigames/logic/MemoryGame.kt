package com.ayomide.minigames.logic

import kotlin.random.Random

/** Pairs memory game. Flip two cards; matching pairs stay face up. */
class MemoryGame(pairs: Int = 8, random: Random = Random.Default) {

    companion object {
        val SYMBOLS = listOf("🍕", "⚽", "🎧", "🚀", "🐱", "🌶", "🎲", "🍉", "🦁", "🌍", "🎸", "💎")
    }

    enum class Flip { IGNORED, FIRST, MATCH, MISMATCH }

    val cards: List<String> = SYMBOLS.take(pairs).flatMap { listOf(it, it) }.shuffled(random)
    val faceUp = BooleanArray(cards.size)
    val matched = BooleanArray(cards.size)
    var moves = 0
        private set
    private var first = -1
    private var pendingHide: Pair<Int, Int>? = null

    val isComplete get() = matched.all { it }
    val isWaiting get() = pendingHide != null

    fun flip(i: Int): Flip {
        if (i !in cards.indices || pendingHide != null || faceUp[i] || matched[i]) return Flip.IGNORED
        faceUp[i] = true
        if (first < 0) {
            first = i
            return Flip.FIRST
        }
        moves++
        val a = first
        first = -1
        return if (cards[a] == cards[i]) {
            matched[a] = true; matched[i] = true
            Flip.MATCH
        } else {
            pendingHide = a to i
            Flip.MISMATCH
        }
    }

    /** Turns the last mismatched pair back down. */
    fun hideMismatch() {
        pendingHide?.let { (a, b) -> faceUp[a] = false; faceUp[b] = false }
        pendingHide = null
    }
}
