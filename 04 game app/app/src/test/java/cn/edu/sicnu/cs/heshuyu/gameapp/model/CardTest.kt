package cn.edu.sicnu.cs.heshuyu.gameapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Card（模型）的单元测试：验证匹配得分规则与牌的初始状态。
 */
class CardTest {

    @Test
    fun cardToStringCombinesSuitAndRank() {
        assertEquals("♥A", Card("♥", "A").toString())
        assertEquals("♠10", Card("♠", "10").toString())
    }

    @Test
    fun newCardIsNeitherChosenNorMatched() {
        val card = Card("♣", "K")
        assertFalse(card.isChosen)
        assertFalse(card.isMatched)
    }

    @Test
    fun sameRankScoresFourPoints() {
        val score = Card("♥", "A").match(arrayOf(Card("♠", "A")))
        assertEquals(4, score)
    }

    @Test
    fun sameSuitScoresOnePoint() {
        val score = Card("♥", "A").match(arrayOf(Card("♥", "K")))
        assertEquals(1, score)
    }

    @Test
    fun differentRankAndSuitScoresZero() {
        val score = Card("♥", "A").match(arrayOf(Card("♠", "K")))
        assertEquals(0, score)
    }

    @Test
    fun comparingWithMoreThanOneCardScoresZero() {
        val cards = arrayOf(Card("♥", "A"), Card("♥", "A"))
        assertEquals(0, Card("♥", "A").match(cards))
    }

    @Test
    fun comparingWithNoCardScoresZero() {
        assertEquals(0, Card("♥", "A").match(emptyArray()))
    }

    @Test
    fun heartAndDiamondAreRed() {
        assertTrue(Card("♥", "2").isRed)
        assertTrue(Card("♦", "2").isRed)
        assertFalse(Card("♠", "2").isRed)
        assertFalse(Card("♣", "2").isRed)
    }

    @Test
    fun scoreBetweenMatchesTheRuleOfMatch() {
        val first = Card("♦", "Q")
        val second = Card("♣", "Q")
        assertEquals(Card.scoreBetween(first, second), first.match(arrayOf(second)))
    }
}
