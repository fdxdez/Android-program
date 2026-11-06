package cn.edu.sicnu.cs.heshuyu.fragment.data

import java.io.Serializable
import java.util.Random

/**
 * 三张牌的翻牌游戏（Model 层），可供 [Serializable] 序列化后写入文件。
 *
 * 规则（实验五的三张牌配对规则）：
 * - 每翻开三张牌比较一次，三张点数全部相同才配对成功，否则三张一起翻回背面；
 * - 每翻开一张牌消耗 [COST_TO_CHOOSE] 分；
 * - 配对成功获得 `匹配分 × [MATCH_BONUS]`，配对失败扣 [MISMATCH_PENALTY] 分；
 * - 牌桌上的牌全部配对完成即获胜，并把本局分数写入 [bestScore]（历史最高分）。
 *
 * 发牌时按「三张同点数为一组」抽取，因此牌桌上的牌一定能配成若干组。
 */
class ThreeCardGame(
    /** 牌桌上的牌数，必须能被 3 整除 */
    val totalCards: Int = DEFAULT_CARD_COUNT,
    /** 随机数发生器，保存到文件时不需要序列化，恢复时重新创建一个即可 */
    @Transient private val random: Random = Random()
) : Serializable {

    /** 一次翻牌操作的结果，控制器据此刷新界面 */
    data class FlipResult(
        val action: Action,
        val affectedIndices: List<Int> = emptyList()
    ) : Serializable

    /** 翻牌动作的类型 */
    enum class Action {
        /** 本次点击无效（已配对的牌、或正在等待翻回） */
        IGNORED,

        /** 翻开了一张牌 */
        FLIPPED_UP,

        /** 把已经翻开的牌翻回背面 */
        FLIPPED_DOWN,

        /** 三张牌配对成功 */
        MATCHED,

        /** 三张牌配对失败，等待控制器延时翻回 */
        MISMATCHED
    }

    companion object {
        private const val serialVersionUID = 1L

        /** 三张牌配成一组 */
        const val CARDS_PER_MATCH = 3

        /** 默认牌数：24 张 = 8 组（6 列 × 4 行） */
        const val DEFAULT_CARD_COUNT = 24

        const val MATCH_BONUS = 4
        const val MISMATCH_PENALTY = 2
        const val COST_TO_CHOOSE = 1
        const val INDEX_NONE = -1
    }

    private val cards = mutableListOf<CardState>()

    /** 当前已经翻开、还没有配成组的牌的下标 */
    private val chosenIndices = mutableListOf<Int>()

    /** 当前分数 */
    var score: Int = 0
        private set

    /** 翻牌次数 */
    var flips: Int = 0
        private set

    /** 已经配对的组数 */
    var matchedSets: Int = 0
        private set

    /** 正在等待翻回背面的三张牌的下标，没有则为 null */
    var pendingMismatch: List<Int>? = null
        private set

    /** 历史最高分（退出应用后依然保留） */
    var bestScore: Int = 0
        private set

    /** 牌桌上所有的牌（供 View 层读取，不允许修改结构） */
    val allCards: List<CardState>
        get() = cards

    /** 已经翻开、还没有配成组的牌的下标 */
    val selectedIndices: List<Int>
        get() = chosenIndices

    /** 总组数 */
    val setCount: Int
        get() = totalCards / CARDS_PER_MATCH

    /** 是否已经把所有的牌都配对完成 */
    val isGameOver: Boolean
        get() = cards.isNotEmpty() && cards.all { it.matched }

    init {
        require(totalCards > 0 && totalCards % CARDS_PER_MATCH == 0) {
            "totalCards must be a positive multiple of $CARDS_PER_MATCH"
        }
        if (cards.isEmpty()) {
            deal()
        }
    }

    /** 下标为 index 的牌当前是否正面朝上 */
    fun isFaceUp(index: Int): Boolean {
        val card = cards[index]
        return card.faceUp || card.matched
    }

    /** 重新开始一局：重新发牌，分数、翻牌次数、配对进度清零（历史最高分保留） */
    fun reset() {
        deal()
    }

    /**
     * 点击下标为 index 的牌。
     *
     * 每翻开第三张牌时把选中的三张一起比较：点数全部相同即配对成功，
     * 否则三张一起翻回背面（由控制器延时调用 [hidePendingMismatch]）。
     */
    fun chooseCardAtIndex(index: Int): FlipResult {
        if (index !in cards.indices) {
            return FlipResult(Action.IGNORED)
        }
        if (pendingMismatch != null) {
            return FlipResult(Action.IGNORED)
        }

        val card = cards[index]
        if (card.matched) {
            return FlipResult(Action.IGNORED)
        }
        if (card.faceUp) {
            card.faceUp = false
            chosenIndices.remove(index)
            return FlipResult(Action.FLIPPED_DOWN, listOf(index))
        }

        flips++
        score -= COST_TO_CHOOSE
        card.faceUp = true
        chosenIndices.add(index)

        // 还没凑够三张：等玩家继续翻牌
        if (chosenIndices.size < CARDS_PER_MATCH) {
            return FlipResult(Action.FLIPPED_UP, listOf(index))
        }

        val selected = chosenIndices.toList()
        val selectedCards = selected.map { cards[it] }
        if (selectedCards.all { it.rank == selectedCards.first().rank }) {
            score += CARDS_PER_MATCH * MATCH_BONUS
            selectedCards.forEach {
                it.faceUp = false
                it.matched = true
            }
            chosenIndices.clear()
            matchedSets++
            if (score > bestScore) {
                bestScore = score
            }
            return FlipResult(Action.MATCHED, selected)
        }

        score -= MISMATCH_PENALTY
        pendingMismatch = selected
        return FlipResult(Action.MISMATCHED, selected)
    }

    /** 把配对失败的三张牌一起翻回背面（由控制器延时调用） */
    fun hidePendingMismatch() {
        val pending = pendingMismatch ?: return
        pending.forEach { cards[it].faceUp = false }
        chosenIndices.clear()
        pendingMismatch = null
    }

    /** 找出与下标 index 的牌点数相同、可以和它配成一组的两张牌的下标，主要供测试使用 */
    fun indicesOfSet(index: Int): List<Int> = cards.indices
        .filter { it != index && cards[it].rank == cards[index].rank }
        .take(CARDS_PER_MATCH - 1)

    /**
     * 重新发牌：每次抽出三张点数相同、花色互不相同的牌作为一组。
     *
     * 竖屏 24 张 = 8 组，牌桌上每三张同点数的牌恰好可以配成一组。
     */
    private fun deal() {
        val deck = Deck(random)
        val board = mutableListOf<CardState>()
        while (board.size < totalCards) {
            val group = deck.drawCardsOfSameRank(CARDS_PER_MATCH)
            if (group.isEmpty()) {
                break
            }
            board.addAll(group)
        }
        cards.clear()
        cards.addAll(board.shuffled(random))
        chosenIndices.clear()
        score = 0
        flips = 0
        matchedSets = 0
        pendingMismatch = null
    }
}
