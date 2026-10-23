package cn.edu.sicnu.cs.heshuyu.gameapp20.model

import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [Deck] 的单元测试：验证抽牌与「三张同点数」发牌。
 */
class DeckTest {

    @Test
    fun newDeckHasFiftyTwoCards() {
        assertEquals(52, Deck().size)
    }

    @Test
    fun drawingOneCardReducesSize() {
        val deck = Deck(Random(1))
        val card = deck.drawRandomCard()

        assertNotNull(card)
        assertEquals(51, deck.size)
    }

    @Test
    fun drawnCardsShareTheSameRankAndDifferentSuits() {
        val deck = Deck(Random(7))
        val group = deck.drawCardsOfSameRank(3)

        assertEquals(3, group.size)
        assertEquals(1, group.map { it.rank }.distinct().size)
        assertEquals(3, group.map { it.suit }.distinct().size)
        assertEquals(49, deck.size)
    }

    @Test
    fun drawingStopsWhenNoRankHasEnoughCards() {
        val deck = Deck(Random(11))
        var groups = 0
        while (true) {
            val group = deck.drawCardsOfSameRank(3)
            if (group.isEmpty()) {
                break
            }
            assertEquals(3, group.size)
            groups++
        }

        // 13 种点数各抽走 3 张，每种点数剩 1 张，无法再组成一组
        assertEquals(13, groups)
        assertEquals(13, deck.size)
    }

    @Test
    fun drawingZeroCardsReturnsEmptyList() {
        val deck = Deck(Random(3))

        assertTrue(deck.drawCardsOfSameRank(0).isEmpty())
        assertEquals(52, deck.size)
    }
}
