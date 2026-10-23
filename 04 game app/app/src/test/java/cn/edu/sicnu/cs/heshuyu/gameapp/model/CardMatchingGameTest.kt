package cn.edu.sicnu.cs.heshuyu.gameapp.model

import cn.edu.sicnu.cs.heshuyu.gameapp.model.CardMatchingGame.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test

/**
 * CardMatchingGame（模型）的单元测试：发牌、翻牌、计分、翻回与重新开始。
 */
class CardMatchingGameTest {

    private val cardCount = 16
    private val game = CardMatchingGame(cardCount)

    /** 找出一对互不匹配的牌（花色与点数都不相同），找不到时跳过该测试 */
    private fun findMismatchPair(): Pair<Int, Int> {
        val indices = game.allCards.indices
        for (first in indices) {
            for (second in indices) {
                if (first == second) {
                    continue
                }
                if (Card.scoreBetween(game.cardAtIndex(first), game.cardAtIndex(second)) == 0) {
                    return first to second
                }
            }
        }
        Assume.assumeTrue("本局随机发出的牌中没有互不匹配的一对牌", false)
        return 0 to 0
    }

    @Test
    fun gameDealsRequestedNumberOfCards() {
        assertEquals(cardCount, game.allCards.size)
        assertEquals(cardCount / 2, game.pairCount)
    }

    @Test
    fun cardsAreDealtInPairs() {
        val counts = game.allCards.groupingBy { it.toString() }.eachCount()
        assertEquals(cardCount / 2, counts.size)
        assertTrue(counts.values.all { it == 2 })
    }

    @Test
    fun newGameHasZeroScoreAndNoChosenCards() {
        assertEquals(0, game.score)
        assertEquals(0, game.flips)
        assertEquals(0, game.matchedPairs)
        assertFalse(game.isGameOver)
        assertNull(game.pendingMismatch)
        assertTrue(game.allCards.none { it.isChosen || it.isMatched })
    }

    @Test
    fun firstFlipCostsOnePoint() {
        val result = game.chooseCardAtIndex(0)
        assertEquals(Action.FLIPPED_UP, result.action)
        assertEquals(-1, game.score)
        assertEquals(1, game.flips)
        assertTrue(game.isFaceUp(0))
    }

    @Test
    fun clickingTheSameCardAgainFlipsItBack() {
        game.chooseCardAtIndex(0)
        val result = game.chooseCardAtIndex(0)
        assertEquals(Action.FLIPPED_DOWN, result.action)
        assertEquals(-1, game.score)
        assertEquals(1, game.flips)
        assertFalse(game.isFaceUp(0))
    }

    @Test
    fun matchingPairAddsBonusAndStaysFaceUp() {
        val pairIndex = game.indexOfPair(0)
        assertTrue(pairIndex >= 0)

        game.chooseCardAtIndex(0)
        val result = game.chooseCardAtIndex(pairIndex)

        assertEquals(Action.MATCHED, result.action)
        // 两张牌各扣 1 分，配对成功加 4 × MATCH_BONUS
        assertEquals(-2 + 4 * CardMatchingGame.MATCH_BONUS, game.score)
        assertTrue(game.cardAtIndex(0).isMatched)
        assertTrue(game.cardAtIndex(pairIndex).isMatched)
        assertEquals(1, game.matchedPairs)
        assertTrue(game.isFaceUp(0))
    }

    @Test
    fun matchedCardCannotBeChosenAgain() {
        val pairIndex = game.indexOfPair(0)
        game.chooseCardAtIndex(0)
        game.chooseCardAtIndex(pairIndex)
        val scoreAfterMatch = game.score

        val result = game.chooseCardAtIndex(0)

        assertEquals(Action.IGNORED, result.action)
        assertEquals(scoreAfterMatch, game.score)
        assertEquals(1, game.matchedPairs)
    }

    @Test
    fun mismatchedPairKeepsCardsVisibleUntilHidden() {
        val (first, second) = findMismatchPair()
        game.chooseCardAtIndex(first)
        val result = game.chooseCardAtIndex(second)

        assertEquals(Action.MISMATCHED, result.action)
        // 两张牌各扣 1 分，配对失败再扣 MISMATCH_PENALTY
        assertEquals(-2 - CardMatchingGame.MISMATCH_PENALTY, game.score)
        assertNotNull(game.pendingMismatch)
        assertTrue(game.isFaceUp(first))
        assertTrue(game.isFaceUp(second))
    }

    @Test
    fun noCardCanBeChosenWhileWaitingForFlipBack() {
        val (first, second) = findMismatchPair()
        game.chooseCardAtIndex(first)
        game.chooseCardAtIndex(second)
        val scoreBefore = game.score

        val result = game.chooseCardAtIndex(first + 1)

        assertEquals(Action.IGNORED, result.action)
        assertEquals(scoreBefore, game.score)
    }

    @Test
    fun hidePendingMismatchFlipsBothCardsBack() {
        val (first, second) = findMismatchPair()
        game.chooseCardAtIndex(first)
        game.chooseCardAtIndex(second)

        game.hidePendingMismatch()

        assertNull(game.pendingMismatch)
        assertFalse(game.isFaceUp(first))
        assertFalse(game.isFaceUp(second))
    }

    @Test
    fun matchingAllCardsFinishesTheGame() {
        for (index in 0 until cardCount) {
            if (game.cardAtIndex(index).isMatched) {
                continue
            }
            game.chooseCardAtIndex(index)
            game.chooseCardAtIndex(game.indexOfPair(index))
        }

        assertTrue(game.isGameOver)
        assertEquals(cardCount / 2, game.matchedPairs)
        // 每张牌都被翻开过一次
        assertEquals(cardCount, game.flips)
        // 每对牌得 4 × MATCH_BONUS 分，每翻一张牌扣 COST_TO_CHOOSE 分
        val expectedScore = cardCount / 2 * (4 * CardMatchingGame.MATCH_BONUS) -
            cardCount * CardMatchingGame.COST_TO_CHOOSE
        assertEquals(expectedScore, game.score)
    }

    @Test
    fun restartResetsScoreAndCards() {
        val pairIndex = game.indexOfPair(0)
        game.chooseCardAtIndex(0)
        game.chooseCardAtIndex(pairIndex)

        game.restart()

        assertEquals(0, game.score)
        assertEquals(0, game.flips)
        assertEquals(0, game.matchedPairs)
        assertFalse(game.isGameOver)
        assertNull(game.pendingMismatch)
        assertTrue(game.allCards.none { it.isChosen || it.isMatched })
    }

    @Test
    fun outOfRangeIndexIsIgnored() {
        assertEquals(Action.IGNORED, game.chooseCardAtIndex(-1).action)
        assertEquals(Action.IGNORED, game.chooseCardAtIndex(cardCount).action)
    }

    @Test(expected = IllegalArgumentException::class)
    fun oddNumberOfCardsIsRejected() {
        CardMatchingGame(15)
    }
}
