package cn.edu.sicnu.cs.heshuyu.fragment.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 两张牌翻牌游戏的单元测试。
 *
 * 通过 [TwoCardGame.indexOfPair] 找到可以配对的两张牌，验证「配对成功加分、配对失败扣分」等规则，
 * 这些测试不需要 Android 运行环境，用 `gradlew test` 即可执行。
 */
class TwoCardGameTest {

    @Test
    fun `新开一局时牌数为默认值且所有牌都是背面`() {
        val game = TwoCardGame()
        assertEquals(TwoCardGame.DEFAULT_CARD_COUNT, game.allCards.size)
        assertEquals(game.pairCount, game.allCards.size / 2)
        assertEquals(0, game.score)
        assertEquals(0, game.flips)
        assertFalse(game.isGameOver)
        assertTrue(game.allCards.all { !it.faceUp && !it.matched })
    }

    @Test
    fun `每张牌都能找到点数相同的另一张牌`() {
        val game = TwoCardGame()
        game.allCards.indices.forEach { index ->
            val pair = game.indexOfPair(index)
            assertTrue("第 $index 张牌应该找得到配对", pair >= 0)
            assertEquals(game.allCards[index].rank, game.allCards[pair].rank)
        }
    }

    @Test
    fun `翻开点数相同的两张牌后配对成功并加分`() {
        val game = TwoCardGame()
        val first = 0
        val second = game.indexOfPair(first)

        assertEquals(TwoCardGame.Action.FLIPPED_UP, game.chooseCardAtIndex(first).action)
        val result = game.chooseCardAtIndex(second)

        assertEquals(TwoCardGame.Action.MATCHED, result.action)
        assertEquals(1, game.matchedPairs)
        assertEquals(2, game.flips)
        // 翻开两张各扣 1 分，配对成功加 MATCH_BONUS 分
        assertEquals(TwoCardGame.MATCH_BONUS - 2 * TwoCardGame.COST_TO_CHOOSE, game.score)
        assertTrue(game.allCards[first].matched)
        assertTrue(game.allCards[second].matched)
    }

    @Test
    fun `翻开点数不同的两张牌后配对失败并等待翻回`() {
        val game = TwoCardGame()
        val first = 0
        // 找一张点数与第一张不同的牌
        val second = game.allCards.indices.first { game.allCards[it].rank != game.allCards[first].rank }

        game.chooseCardAtIndex(first)
        val result = game.chooseCardAtIndex(second)

        assertEquals(TwoCardGame.Action.MISMATCHED, result.action)
        assertNotNull(game.pendingMismatch)
        // 配对失败时不允许再点击其它牌
        assertEquals(TwoCardGame.Action.IGNORED, game.chooseCardAtIndex(1).action)

        game.hidePendingMismatch()
        assertFalse(game.allCards[first].faceUp)
        assertFalse(game.allCards[second].faceUp)
    }

    @Test
    fun `重新开始后分数与翻牌次数清零但保留最高分`() {
        val game = TwoCardGame()
        val first = 0
        game.chooseCardAtIndex(first)
        game.chooseCardAtIndex(game.indexOfPair(first))
        val best = game.bestScore

        game.reset()

        assertEquals(0, game.score)
        assertEquals(0, game.flips)
        assertEquals(0, game.matchedPairs)
        assertEquals(best, game.bestScore)
        assertFalse(game.isGameOver)
    }
}
