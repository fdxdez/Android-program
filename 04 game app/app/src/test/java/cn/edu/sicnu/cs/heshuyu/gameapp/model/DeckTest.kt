package cn.edu.sicnu.cs.heshuyu.gameapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * Deck（模型）的单元测试：一副牌共 52 张，随机抽牌不重复且抽完后返回 null。
 */
class DeckTest {

    @Test
    fun newDeckHasFiftyTwoCards() {
        assertEquals(52, Deck().size)
    }

    @Test
    fun drawAllCardsReturnsFiftyTwoDifferentCards() {
        val deck = Deck(Random(2024L))
        val drawn = mutableSetOf<String>()
        while (deck.size > 0) {
            val card = deck.drawRandomCard()
            assertNotNull(card)
            drawn.add(card.toString())
        }
        assertEquals(52, drawn.size)
    }

    @Test
    fun drawingFromEmptyDeckReturnsNull() {
        val deck = Deck(Random(1L))
        repeat(52) { assertNotNull(deck.drawRandomCard()) }
        assertEquals(0, deck.size)
        assertNull(deck.drawRandomCard())
    }

    @Test
    fun drawnCardComesFromTheStandardCardSet() {
        val deck = Deck(Random(7L))
        repeat(10) {
            val card = deck.drawRandomCard()
            assertNotNull(card)
            assertTrue(card!!.suit in Card.validSuits)
            assertTrue(card.rank in Card.rankStrings)
        }
    }
}
