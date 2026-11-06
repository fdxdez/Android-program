package cn.edu.sicnu.cs.heshuyu.fragment.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 三张牌翻牌游戏的单元测试。
 *
 * 通过 [ThreeCardGame.indicesOfSet] 找到可以配成一组的三张牌，
 * 验证「三张点数相同才配对成功」的规则以及重新开始后的状态。
 */
class ThreeCardGameTest {

    @Test
    fun `新开一局时牌数为三的整数倍且都能配成组`() {
        val game = ThreeCardGame()
        assertEquals(ThreeCardGame.DEFAULT_CARD_COUNT, game.allCards.size)
        assertEquals(0, game.allCards.size % ThreeCardGame.CARDS_PER_MATCH)
        assertEquals(game.setCount, game.allCards.size / ThreeCardGame.CARDS_PER_MATCH)
        assertTrue(game.allCards.all { !it.faceUp && !it.matched })
    }

    @Test
    fun `每张牌都能找到另外两张点数相同的牌`() {
        val game = ThreeCardGame()
        game.allCards.indices.forEach { index ->
            val mates = game.indicesOfSet(index)
            assertEquals(ThreeCardGame.CARDS_PER_MATCH - 1, mates.size)
            mates.forEach { mate ->
                assertEquals(game.allCards[index].rank, game.allCards[mate].rank)
            }
        }
    }

    @Test
    fun `三张点数相同的牌配对成功并加分`() {
        val game = ThreeCardGame()
        val first = 0
        val mates = game.indicesOfSet(first)

        assertEquals(ThreeCardGame.Action.FLIPPED_UP, game.chooseCardAtIndex(first).action)
        assertEquals(ThreeCardGame.Action.FLIPPED_UP, game.chooseCardAtIndex(mates[0]).action)
        val result = game.chooseCardAtIndex(mates[1])

        assertEquals(ThreeCardGame.Action.MATCHED, result.action)
        assertEquals(1, game.matchedSets)
        assertEquals(3, game.flips)
        // 翻开三张各扣 1 分，配对成功加 3 × MATCH_BONUS 分
        assertEquals(
            ThreeCardGame.CARDS_PER_MATCH * ThreeCardGame.MATCH_BONUS -
                3 * ThreeCardGame.COST_TO_CHOOSE,
            game.score
        )
        assertTrue(listOf(first, mates[0], mates[1]).all { game.allCards[it].matched })
    }

    @Test
    fun `三张点数不同则配对失败并等待翻回`() {
        val game = ThreeCardGame()
        val first = 0
        val mates = game.indicesOfSet(first)
        // 找一张点数既不同于第一张、也不同于它的同伴的牌
        val other = game.allCards.indices.first {
            it != first && it !in mates && game.allCards[it].rank != game.allCards[first].rank
        }

        game.chooseCardAtIndex(first)
        game.chooseCardAtIndex(mates[0])
        val result = game.chooseCardAtIndex(other)

        assertEquals(ThreeCardGame.Action.MISMATCHED, result.action)
        assertEquals(ThreeCardGame.CARDS_PER_MATCH, game.pendingMismatch?.size)
        assertEquals(ThreeCardGame.Action.IGNORED, game.chooseCardAtIndex(1).action)

        game.hidePendingMismatch()
        assertFalse(game.allCards[first].faceUp)
        assertTrue(game.selectedIndices.isEmpty())
    }

    @Test
    fun `重新开始后清空配对进度并保留最高分`() {
        val game = ThreeCardGame()
        val first = 0
        val mates = game.indicesOfSet(first)
        game.chooseCardAtIndex(first)
        game.chooseCardAtIndex(mates[0])
        game.chooseCardAtIndex(mates[1])
        val best = game.bestScore

        game.reset()

        assertEquals(0, game.score)
        assertEquals(0, game.flips)
        assertEquals(0, game.matchedSets)
        assertEquals(best, game.bestScore)
        assertFalse(game.isGameOver)
    }
}
