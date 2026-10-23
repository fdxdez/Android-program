package cn.edu.sicnu.cs.heshuyu.gameapp.model

import java.util.Random

/**
 * 翻牌游戏的模型（Model 层），参照实验附件中的 CardMatchingGame 类实现。
 *
 * 计分规则（与附件代码保持一致）：
 * - 每翻开一张牌消耗 [COST_TO_CHOOSE] 分；
 * - 配对成功额外获得「匹配分 × [MATCH_BONUS]」；
 * - 配对失败扣除 [MISMATCH_PENALTY] 分。
 *
 * 发牌时会保证牌桌上的牌两两配对：先随机抽出 cardCount / 2 张不同的牌，再各复制一张后洗牌。
 *
 * 该类不持有任何界面对象，只负责游戏状态，界面由控制器（GameActivity）根据状态同步。
 */
class CardMatchingGame(val totalCards: Int = DEFAULT_CARD_COUNT) {

    /** 一次翻牌操作的结果，控制器据此刷新界面 */
    data class FlipResult(
        val action: Action,
        val firstIndex: Int = INDEX_NONE,
        val secondIndex: Int = INDEX_NONE
    )

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
        const val DEFAULT_CARD_COUNT = 16
        const val MATCH_BONUS = 4
        const val MISMATCH_PENALTY = 2
        const val COST_TO_CHOOSE = 1
        const val INDEX_NONE = -1
    }

    private val cards = mutableListOf<Card>()

    /** 当前分数 */
    var score: Int = 0
        private set

    /** 翻牌次数 */
    var flips: Int = 0
        private set

    /** 已经配对的牌对数 */
    var matchedPairs: Int = 0
        private set

    /** 正在等待翻回背面的一对牌的下标，没有则为 null */
    var pendingMismatch: Pair<Int, Int>? = null
        private set

    /** 牌桌上所有的牌（供 View 层读取，不允许修改结构） */
    val allCards: List<Card>
        get() = cards

    /** 总对数 */
    val pairCount: Int
        get() = totalCards / 2

    /** 是否已经把所有的牌都配对完成 */
    val isGameOver: Boolean
        get() = cards.isNotEmpty() && cards.all { it.isMatched }

    init {
        require(totalCards > 0 && totalCards % 2 == 0) {
            "totalCards must be a positive even number"
        }
        deal()
    }

    /** 重新开始一局：重新发牌，分数、翻牌次数清零 */
    fun restart() {
        deal()
    }

    fun cardAtIndex(index: Int): Card = cards[index]

    /** 下标为 index 的牌当前是否正面朝上 */
    fun isFaceUp(index: Int): Boolean {
        val card = cards[index]
        return card.isChosen || card.isMatched
    }

    /**
     * 点击下标为 index 的牌。
     *
     * @return 本次操作的结果，控制器据此播放翻转动画、刷新分数并判断胜负
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
        if (card.isMatched) {
            return FlipResult(Action.IGNORED)
        }
        if (card.isChosen) {
            card.isChosen = false
            return FlipResult(Action.FLIPPED_DOWN, index)
        }

        flips++
        score -= COST_TO_CHOOSE
        val otherIndex = cards.indices.firstOrNull { it != index && cards[it].isChosen && !cards[it].isMatched }
        card.isChosen = true

        if (otherIndex == null) {
            return FlipResult(Action.FLIPPED_UP, index)
        }

        val otherCard = cards[otherIndex]
        val matchScore = card.match(arrayOf(otherCard))
        if (matchScore > 0) {
            score += matchScore * MATCH_BONUS
            card.isMatched = true
            otherCard.isMatched = true
            matchedPairs++
            return FlipResult(Action.MATCHED, otherIndex, index)
        }

        score -= MISMATCH_PENALTY
        pendingMismatch = otherIndex to index
        return FlipResult(Action.MISMATCHED, otherIndex, index)
    }

    /** 把配对失败的两张牌翻回背面（由控制器延时调用） */
    fun hidePendingMismatch() {
        val pair = pendingMismatch ?: return
        cards[pair.first].isChosen = false
        cards[pair.second].isChosen = false
        pendingMismatch = null
    }

    /** 找出与下标 index 的牌成对的另一张牌，主要供测试使用 */
    fun indexOfPair(index: Int): Int {
        val card = cards[index]
        return cards.indices.firstOrNull { it != index && cards[it].suit == card.suit && cards[it].rank == card.rank }
            ?: INDEX_NONE
    }

    private fun deal() {
        cards.clear()
        val deck = Deck()
        val picked = mutableListOf<Card>()
        while (picked.size < pairCount) {
            val card = deck.drawRandomCard() ?: break
            picked.add(card)
        }
        for (card in picked) {
            cards.add(Card(suit = card.suit, rank = card.rank))
            cards.add(Card(suit = card.suit, rank = card.rank))
        }
        cards.shuffle(Random())
        score = 0
        flips = 0
        matchedPairs = 0
        pendingMismatch = null
    }
}
