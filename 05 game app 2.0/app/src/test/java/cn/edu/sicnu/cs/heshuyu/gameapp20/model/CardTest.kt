package cn.edu.sicnu.cs.heshuyu.gameapp20.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [Card] 的单元测试：验证三张牌游戏的匹配规则。
 */
class CardTest {

    @Test
    fun threeCardsWithSameRankMatch() {
        val card = Card(suit = "♥", rank = "A")
        val others = arrayOf(Card(suit = "♦", rank = "A"), Card(suit = "♠", rank = "A"))

        assertEquals(Card.SCORE_THREE_OF_A_KIND, card.match(others))
    }

    @Test
    fun threeCardsWithDifferentRanksDoNotMatch() {
        val card = Card(suit = "♥", rank = "A")
        val others = arrayOf(Card(suit = "♦", rank = "A"), Card(suit = "♠", rank = "K"))

        assertEquals(0, card.match(others))
    }

    @Test
    fun onlyTwoCardsAreNotEnough() {
        val card = Card(suit = "♥", rank = "A")

        assertEquals(0, card.match(arrayOf(Card(suit = "♦", rank = "A"))))
    }

    @Test
    fun redSuitsAreRed() {
        assertTrue(Card(suit = "♥", rank = "A").isRed)
        assertTrue(Card(suit = "♦", rank = "A").isRed)
        assertFalse(Card(suit = "♠", rank = "A").isRed)
        assertFalse(Card(suit = "♣", rank = "A").isRed)
    }

    @Test
    fun toStringShowsSuitAndRank() {
        assertEquals("♥A", Card(suit = "♥", rank = "A").toString())
        assertEquals("♠10", Card(suit = "♠", rank = "10").toString())
    }
}
