package cn.edu.sicnu.cs.heshuyu.fragment.data

import java.io.Serializable
import java.util.Random

/**
 * 两张牌的翻牌游戏（Model 层），可供 [Serializable] 序列化后写入文件。
 *
 * 规则（沿用实验四的两张牌配对规则）：
 * - 每次翻开两张牌，两张点数相同即配对成功，否则一起翻回背面；
 * - 每翻开一张牌消耗 [COST_TO_CHOOSE] 分；
 * - 配对成功获得 `匹配分 × [MATCH_BONUS]`，配对失败扣 [MISMATCH_PENALTY] 分；
 * - 牌桌上的牌全部配对完成即获胜，并把本局分数写入 [bestScore]（历史最高分）。
 *
 * 发牌时保证牌桌上的牌成对出现：先抽出「对数」张互不相同的牌，再各复制一张后洗牌，
 * 所以任意一张牌在牌桌上一定找得到和它点数相同的另一张。
 */
class TwoCardGame(
    /** 牌桌上的牌数，必须是正偶数 */
    val totalCards: Int = DEFAULT_CARD_COUNT,
    /** 随机数发生器，保存到文件时不需要序列化，恢复时重新创建一个即可 */
    @Transient private val random: Random = Random()
) : Serializable {

    /** 一次翻牌操作的结果，控制器据此刷新界面 */
    data class FlipResult(
        val action: Action,
        val firstIndex: Int = INDEX_NONE,
        val secondIndex: Int = INDEX_NONE
    ) : Serializable

    /** 翻牌动作的类型 */
    enum class Action {
        /** 本次点击无效（已配对的牌、或正在等待翻回） */
        IGNORED,

        /** 翻开了第一张牌 */
        FLIPPED_UP,

        /** 把已经翻开的牌翻回背面 */
        FLIPPED_DOWN,

        /** 两张牌配对成功 */
        MATCHED,

        /** 两张牌配对失败，等待控制器延时翻回 */
        MISMATCHED
    }

    companion object {
        private const val serialVersionUID = 1L

        /** 默认牌数：16 张 = 8 对（4 列 × 4 行） */
        const val DEFAULT_CARD_COUNT = 16

        const val MATCH_BONUS = 4
        const val MISMATCH_PENALTY = 2
        const val COST_TO_CHOOSE = 1
        const val INDEX_NONE = -1
    }

    private val cards = mutableListOf<CardState>()

    /** 当前分数 */
    var score: Int = 0
        private set

    /** 翻牌次数 */
    var flips: Int = 0
        private set

    /** 已经配对成功的对数 */
    var matchedPairs: Int = 0
        private set

    /** 正在等待翻回背面的一对牌的下标，没有则为 null */
    var pendingMismatch: Pair<Int, Int>? = null
        private set

    /** 历史最高分（退出应用后依然保留） */
    var bestScore: Int = 0
        private set

    /** 牌桌上所有的牌（供 View 层读取，不允许修改结构） */
    val allCards: List<CardState>
        get() = cards

    /** 总对数 */
    val pairCount: Int
        get() = totalCards / 2

    /** 是否已经把所有的牌都配对完成 */
    val isGameOver: Boolean
        get() = cards.isNotEmpty() && cards.all { it.matched }

    init {
        require(totalCards > 0 && totalCards % 2 == 0) {
            "totalCards must be a positive even number"
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
     * @return 本次操作的结果，控制器据此刷新界面、判断胜负
     */
    fun chooseCardAtIndex(index: Int): FlipResult {
        if (index !in cards.indices) {
            return FlipResult(Action.IGNORED)
        }
        // 等待翻回期间不接受新的点击，避免状态错乱
        if (pendingMismatch != null) {
            return FlipResult(Action.IGNORED)
        }

        val card = cards[index]
        if (card.matched) {
            return FlipResult(Action.IGNORED)
        }
        if (card.faceUp) {
            card.faceUp = false
            return FlipResult(Action.FLIPPED_DOWN, index)
        }

        flips++
        score -= COST_TO_CHOOSE
        // 找出另一张已经翻开、还没有配对的牌
        val otherIndex = cards.indices.firstOrNull { it != index && cards[it].faceUp && !cards[it].matched }
        card.faceUp = true

        if (otherIndex == null) {
            return FlipResult(Action.FLIPPED_UP, index)
        }

        val otherCard = cards[otherIndex]
        if (otherCard.rank == card.rank) {
            score += MATCH_BONUS
            card.matched = true
            otherCard.matched = true
            matchedPairs++
            if (score > bestScore) {
                bestScore = score
            }
            return FlipResult(Action.MATCHED, otherIndex, index)
        }

        score -= MISMATCH_PENALTY
        pendingMismatch = otherIndex to index
        return FlipResult(Action.MISMATCHED, otherIndex, index)
    }

    /** 把配对失败的两张牌翻回背面（由控制器延时调用） */
    fun hidePendingMismatch() {
        val pair = pendingMismatch ?: return
        cards[pair.first].faceUp = false
        cards[pair.second].faceUp = false
        pendingMismatch = null
    }

    /** 找出与下标 index 的牌点数相同、可以和它配对的那张牌，主要供测试使用 */
    fun indexOfPair(index: Int): Int {
        val card = cards[index]
        return cards.indices.firstOrNull { it != index && cards[it].rank == card.rank } ?: INDEX_NONE
    }

    /** 重新发牌：随机抽「对数」张不同的牌，各复制一张后洗牌 */
    private fun deal() {
        cards.clear()
        val deck = Deck(random)
        val picked = mutableListOf<CardState>()
        while (picked.size < pairCount) {
            val card = deck.drawRandomCard() ?: break
            picked.add(card)
        }
        for (card in picked) {
            // 复制成两张，保证「点数相同」的一对一定同时存在
            cards.add(CardState(suit = card.suit, rank = card.rank))
            cards.add(CardState(suit = card.suit, rank = card.rank))
        }
        cards.shuffle(random)
        score = 0
        flips = 0
        matchedPairs = 0
        pendingMismatch = null
    }
}
