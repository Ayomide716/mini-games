package com.ayomide.minigames.logic

import kotlin.random.Random

enum class Suit(val symbol: String, val label: String) {
    CIRCLE("●", "Circle"), TRIANGLE("▲", "Triangle"), CROSS("✚", "Cross"),
    SQUARE("■", "Square"), STAR("★", "Star"), WHOT("W", "Whot"),
}

data class WCard(val suit: Suit, val number: Int) {
    override fun toString() = "${suit.label} $number"
}

/**
 * Naija Whot for 2–4 players (player 0 is you). 54-card deck.
 *
 * Specials: 1 Hold on (play again), 2 Pick two, 5 Pick three, 8 Suspension
 * (next player skips), 14 General market (everyone else picks one),
 * 20 Whot (wild: you call the suit). Match the top card by suit or number,
 * or the called suit after a Whot. If you can't play, go to market (draw one).
 * First to finish their cards wins.
 */
class Whot(val players: Int = 2, private val random: Random = Random.Default, handSize: Int = 5) {

    companion object {
        fun deck(): List<WCard> {
            val out = ArrayList<WCard>()
            val full = listOf(1, 2, 3, 4, 5, 7, 8, 10, 11, 12, 13, 14)
            val short = listOf(1, 2, 3, 5, 7, 10, 11, 13, 14)
            for (n in full) out += WCard(Suit.CIRCLE, n)
            for (n in full) out += WCard(Suit.TRIANGLE, n)
            for (n in short) out += WCard(Suit.CROSS, n)
            for (n in short) out += WCard(Suit.SQUARE, n)
            for (n in listOf(1, 2, 3, 4, 5, 7, 8)) out += WCard(Suit.STAR, n)
            repeat(5) { out += WCard(Suit.WHOT, 20) }
            return out
        }
    }

    val hands: List<MutableList<WCard>> = List(players) { ArrayList() }
    val market = ArrayDeque<WCard>()
    val pile = ArrayList<WCard>()
    var current = 0
        private set
    /** Suit called after a Whot card, or null. */
    var calledSuit: Suit? = null
        private set
    var winner = -1
        private set
    /** Short description of the last action, for the UI. */
    var lastEvent = ""
        private set

    val top: WCard get() = pile.last()

    init {
        require(players in 2..4)
        market.addAll(deck().shuffled(random))
        repeat(handSize) { for (p in 0 until players) hands[p] += market.removeFirst() }
        // Start with a plain (non-special) card.
        var start = market.removeFirst()
        while (start.number in setOf(1, 2, 5, 8, 14, 20)) {
            market.addLast(start)
            start = market.removeFirst()
        }
        pile += start
    }

    private fun next(p: Int, steps: Int = 1) = (p + steps) % players

    fun canPlay(card: WCard): Boolean {
        if (card.suit == Suit.WHOT) return true
        val want = calledSuit
        if (want != null) return card.suit == want
        return card.suit == top.suit || card.number == top.number
    }

    fun playable(player: Int = current): List<WCard> = hands[player].filter { canPlay(it) }

    private fun draw(player: Int, n: Int): Int {
        var got = 0
        repeat(n) {
            if (market.isEmpty()) refillMarket()
            if (market.isEmpty()) return got
            hands[player] += market.removeFirst()
            got++
        }
        return got
    }

    private fun refillMarket() {
        if (pile.size <= 1) return
        val keep = pile.removeAt(pile.size - 1)
        market.addAll(pile.shuffled(random))
        pile.clear()
        pile += keep
    }

    /** Current player plays [card]. [suit] is required for a Whot card. */
    fun play(card: WCard, suit: Suit? = null): Boolean {
        if (winner >= 0 || card !in hands[current] || !canPlay(card)) return false
        if (card.suit == Suit.WHOT && (suit == null || suit == Suit.WHOT)) return false
        val me = current
        hands[me].remove(card)
        pile += card
        calledSuit = if (card.suit == Suit.WHOT) suit else null

        if (hands[me].isEmpty()) {
            winner = me
            lastEvent = "${name(me)} played $card and won!"
            return true
        }
        val victim = next(me)
        lastEvent = "${name(me)} played $card"
        current = when (card.number) {
            1 -> { lastEvent += ": hold on, play again"; me }
            2 -> { draw(victim, 2); lastEvent += ": ${name(victim)} picks 2"; next(me, 2) }
            5 -> { draw(victim, 3); lastEvent += ": ${name(victim)} picks 3"; next(me, 2) }
            8 -> { lastEvent += ": ${name(victim)} is suspended"; next(me, 2) }
            14 -> {
                for (p in 0 until players) if (p != me) draw(p, 1)
                lastEvent += ": general market, everyone picks 1"
                me
            }
            20 -> { lastEvent += " and asked for ${suit!!.label}"; victim }
            else -> victim
        }
        return true
    }

    /** Current player goes to market (draws one) and the turn passes. */
    fun goMarket() {
        if (winner >= 0) return
        val me = current
        val got = draw(me, 1)
        lastEvent = if (got > 0) "${name(me)} went to market" else "Market is empty: ${name(me)} passes"
        current = next(me)
    }

    fun name(p: Int) = if (p == 0) "You" else "CPU $p"

    /** Simple CPU: prefer attacking cards when the next player is low, keep Whot for last. */
    fun cpuChoice(): Pair<WCard, Suit?>? {
        val hand = hands[current]
        val options = playable()
        if (options.isEmpty()) return null
        val nextCount = hands[next(current)].size
        val bestSuit = hand.filter { it.suit != Suit.WHOT }.groupingBy { it.suit }.eachCount()
            .maxByOrNull { it.value }?.key ?: Suit.CIRCLE
        val nonWhot = options.filter { it.suit != Suit.WHOT }
        val pick = when {
            nonWhot.isEmpty() -> options.first()
            nextCount <= 2 -> nonWhot.maxByOrNull { specialRank(it.number) } ?: nonWhot.first()
            else -> nonWhot.sortedWith(compareBy<WCard>({ if (it.number == 1 || it.number == 14) 0 else 1 }, { -it.number })).first()
        }
        return pick to (if (pick.suit == Suit.WHOT) bestSuit else null)
    }

    private fun specialRank(n: Int) = when (n) {
        5 -> 5; 2 -> 4; 8 -> 3; 14 -> 2; 1 -> 1; else -> 0
    }
}
