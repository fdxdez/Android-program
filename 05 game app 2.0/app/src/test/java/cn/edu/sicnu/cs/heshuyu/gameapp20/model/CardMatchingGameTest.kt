package cn.edu.sicnu.cs.heshuyu.gameapp20.model

import cn.edu.sicnu.cs.heshuyu.gameapp20.model.CardMatchingGame.Action
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [CardMatchingGame] 的单元测试：发牌、翻牌、三张牌配对、计分、翻回与 reset。
 */
class CardMatchingGameTest {

    private val cardCount = 24

    /** 用固定的随机种子创建模型，保证每个测试的牌桌一致、结果可复现 */
    private fun newGame(): CardMatchingGame = CardMatchingGame(cardCount, Random(2024))

    /** 找出点数互不相同的三张牌的下标，用于构造「配对失败」的场景 */
    private fun findMismatchIndices(game: CardMatchingGame): List<Int> {
        val indices = mutableListOf<Int>()
        for (index in game.allCards.indices) {
            val rank = game.cardAtIndex(index).rank
            if (indices.none { game.cardAtIndex(it).rank == rank }) {
                indices.add(index)
            }
            if (indices.size == CardMatchingGame.CARDS_PER_MATCH) {
                break
            }
        }
        return indices
    }

    @Test
    fun gameDealsRequestedNumberOfCards() {
        val game = newGame()

        assertEquals(cardCount, game.allCards.size)
        assertEquals(cardCount / 3, game.setCount)
    }

    @Test
    fun everyRankAppearsExactlyThreeTimes() {
        val game = newGame()
        val counts = game.allCards.groupingBy { it.rank }.eachCount()

        assertEquals(cardCount / 3, counts.size)
        assertTrue(counts.values.all { it == CardMatchingGame.CARDS_PER_MATCH })
    }

    @Test
    fun newGameHasZeroScoreAndNoChosenCards() {
        val game = newGame()

        assertEquals(0, game.score)
        assertEquals(0, game.flips)
        assertEquals(0, game.matchedSets)
        assertFalse(game.isGameOver)
        assertNull(game.pendingMismatch)
        assertTrue(game.selectedIndices.isEmpty())
        assertTrue(game.allCards.none { it.isChosen || it.isMatched })
    }

    @Test
    fun firstFlipCostsOnePoint() {
        val game = newGame()

        val result = game.chooseCardAtIndex(0)

        assertEquals(Action.FLIPPED_UP, result.action)
        assertEquals(-CardMatchingGame.COST_TO_CHOOSE, game.score)
        assertEquals(1, game.flips)
        assertTrue(game.isFaceUp(0))
        assertEquals(listOf(0), game.selectedIndices)
    }

    @Test
    fun clickingTheSameCardAgainFlipsItBack() {
        val game = newGame()
        game.chooseCardAtIndex(0)

        val result = game.chooseCardAtIndex(0)

        assertEquals(Action.FLIPPED_DOWN, result.action)
        assertEquals(-CardMatchingGame.COST_TO_CHOOSE, game.score)
        assertEquals(1, game.flips)
        assertFalse(game.isFaceUp(0))
    }

    @Test
    fun threeCardsWithSameRankMatch() {
        val game = newGame()
        val first = 0
        val others = game.indicesOfSet(first)
        assertEquals(CardMatchingGame.CARDS_PER_MATCH - 1, others.size)

        game.chooseCardAtIndex(first)
        game.chooseCardAtIndex(others[0])
        val result = game.chooseCardAtIndex(others[1])

        assertEquals(Action.MATCHED, result.action)
        assertEquals(CardMatchingGame.CARDS_PER_MATCH, result.affectedIndices.size)
        // 三张牌各扣 1 分，配对成功加 3 × MATCH_BONUS
        assertEquals(
            -3 + Card.SCORE_THREE_OF_A_KIND * CardMatchingGame.MATCH_BONUS,
            game.score
        )
        assertTrue(game.cardAtIndex(first).isMatched)
        assertTrue(game.cardAtIndex(others[0]).isMatched)
        assertTrue(game.cardAtIndex(others[1]).isMatched)
        assertEquals(1, game.matchedSets)
        assertTrue(game.isFaceUp(first))
        assertTrue(game.selectedIndices.isEmpty())
    }

    @Test
    fun matchedCardCannotBeChosenAgain() {
        val game = newGame()
        val others = game.indicesOfSet(0)
        game.chooseCardAtIndex(0)
        game.chooseCardAtIndex(others[0])
        game.chooseCardAtIndex(others[1])
        val scoreAfterMatch = game.score

        val result = game.chooseCardAtIndex(0)

        assertEquals(Action.IGNORED, result.action)
        assertEquals(scoreAfterMatch, game.score)
        assertEquals(1, game.matchedSets)
    }

    @Test
    fun mismatchedCardsStayVisibleUntilHidden() {
        val game = newGame()
        val indices = findMismatchIndices(game)
        assertEquals(CardMatchingGame.CARDS_PER_MATCH, indices.size)

        indices.dropLast(1).forEach { game.chooseCardAtIndex(it) }
        val result = game.chooseCardAtIndex(indices.last())

        assertEquals(Action.MISMATCHED, result.action)
        // 三张牌各扣 1 分，配对失败再扣 MISMATCH_PENALTY
        assertEquals(-3 - CardMatchingGame.MISMATCH_PENALTY, game.score)
        assertNotNull(game.pendingMismatch)
        indices.forEach { assertTrue(game.isFaceUp(it)) }
    }

    @Test
    fun noCardCanBeChosenWhileWaitingForFlipBack() {
        val game = newGame()
        val indices = findMismatchIndices(game)
        indices.forEach { game.chooseCardAtIndex(it) }
        val scoreBefore = game.score

        val result = game.chooseCardAtIndex(indices.first() + 1)

        assertEquals(Action.IGNORED, result.action)
        assertEquals(scoreBefore, game.score)
    }

    @Test
    fun hidePendingMismatchFlipsAllThreeCardsBack() {
        val game = newGame()
        val indices = findMismatchIndices(game)
        indices.forEach { game.chooseCardAtIndex(it) }

        game.hidePendingMismatch()

        assertNull(game.pendingMismatch)
        assertTrue(game.selectedIndices.isEmpty())
        indices.forEach { assertFalse(game.isFaceUp(it)) }
    }

    @Test
    fun matchingAllSetsFinishesTheGame() {
        val game = newGame()
        val byRank = game.allCards.indices.groupBy { game.allCards[it].rank }

        for (indices in byRank.values) {
            indices.forEach { game.chooseCardAtIndex(it) }
        }

        assertTrue(game.isGameOver)
        assertEquals(cardCount / 3, game.matchedSets)
        assertEquals(cardCount, game.flips)
        val expectedScore = cardCount / 3 *
            (Card.SCORE_THREE_OF_A_KIND * CardMatchingGame.MATCH_BONUS) -
            cardCount * CardMatchingGame.COST_TO_CHOOSE
        assertEquals(expectedScore, game.score)
    }

    @Test
    fun resetClearsScoreAndCards() {
        val game = newGame()
        val others = game.indicesOfSet(0)
        game.chooseCardAtIndex(0)
        game.chooseCardAtIndex(others[0])
        game.chooseCardAtIndex(others[1])

        game.reset()

        assertEquals(0, game.score)
        assertEquals(0, game.flips)
        assertEquals(0, game.matchedSets)
        assertFalse(game.isGameOver)
        assertNull(game.pendingMismatch)
        assertTrue(game.allCards.none { it.isChosen || it.isMatched })
        assertEquals(cardCount, game.allCards.size)
    }

    @Test
    fun outOfRangeIndexIsIgnored() {
        val game = newGame()

        assertEquals(Action.IGNORED, game.chooseCardAtIndex(-1).action)
        assertEquals(Action.IGNORED, game.chooseCardAtIndex(cardCount).action)
    }

    @Test(expected = IllegalArgumentException::class)
    fun cardCountMustBeMultipleOfThree() {
        CardMatchingGame(16)
    }
}
