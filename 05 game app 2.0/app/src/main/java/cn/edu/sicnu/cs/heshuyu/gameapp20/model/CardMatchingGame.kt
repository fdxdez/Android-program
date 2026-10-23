package cn.edu.sicnu.cs.heshuyu.gameapp20.model

import java.util.Random

/**
 * 三张牌翻牌游戏的模型（Model 层）。
 *
 * 计分规则：
 * - 每翻开一张牌消耗 [COST_TO_CHOOSE] 分；
 * - 三张牌点数相同即配对成功，额外获得「匹配分 × [MATCH_BONUS]」；
 * - 配对失败扣除 [MISMATCH_PENALTY] 分。
 *
 * 发牌由 [Deck.drawCardsOfSameRank] 完成：每次抽出三张点数相同的牌作为一组，
 * 因此牌桌上每三张同点数的牌恰好可以配成一组（竖屏 24 张 = 8 组，横屏 27 张 = 9 组）。
 *
 * 该类不持有任何界面对象，只负责游戏状态；界面由控制器（GameActivity）
 * 通过 RecyclerView 的适配器根据状态同步，即 MVC 设计模式中的 Model。
 */
class CardMatchingGame(
    val totalCards: Int = DEFAULT_CARD_COUNT,
    private val random: Random = Random()
) {

    /** 一次翻牌操作的结果，控制器据此刷新界面（只暴露需要重绘的牌的下标） */
    data class FlipResult(
        val action: Action,
        val affectedIndices: List<Int> = emptyList()
    )

    /** 翻牌动作的类型 */
    enum class Action {
        /** 本次点击无效（已配对的牌、或正在等待翻回的牌） */
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
        /** 三张牌配成一组 */
        const val CARDS_PER_MATCH = Card.CARDS_PER_MATCH

        /** 默认牌数（竖屏 4 列 × 6 行） */
        const val DEFAULT_CARD_COUNT = 24

        const val MATCH_BONUS = 4
        const val MISMATCH_PENALTY = 2
        const val COST_TO_CHOOSE = 1
        const val INDEX_NONE = -1
    }

    private val cards = mutableListOf<Card>()

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

    /** 牌桌上所有的牌（供 View 层读取，不允许修改结构） */
    val allCards: List<Card>
        get() = cards

    /** 已经翻开、还没有配成组的牌的下标 */
    val selectedIndices: List<Int>
        get() = chosenIndices

    /** 总组数 */
    val setCount: Int
        get() = totalCards / CARDS_PER_MATCH

    /** 是否已经把所有的牌都配对完成 */
    val isGameOver: Boolean
        get() = cards.isNotEmpty() && cards.all { it.isMatched }

    init {
        require(totalCards > 0 && totalCards % CARDS_PER_MATCH == 0) {
            "totalCards must be a positive multiple of $CARDS_PER_MATCH"
        }
        reset()
    }

    /**
     * 重新开始一局：重新发牌，分数、翻牌次数、配对进度全部清零。
     *
     * 实验五要求给模型新增的 reset 方法，控制器在「重新开始」时调用它。
     */
    fun reset() {
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
     * 与实验四的「两张牌」不同，这里每翻开第三张牌时把已选中的三张牌一起比较：
     * 点数全部相同则配对成功，否则三张牌一起翻回背面（由控制器延时调用 [hidePendingMismatch]）。
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
            chosenIndices.remove(index)
            return FlipResult(Action.FLIPPED_DOWN, listOf(index))
        }

        flips++
        score -= COST_TO_CHOOSE
        card.isChosen = true
        chosenIndices.add(index)

        // 还没凑够三张：等玩家继续翻牌
        if (chosenIndices.size < CARDS_PER_MATCH) {
            return FlipResult(Action.FLIPPED_UP, listOf(index))
        }

        val selected = chosenIndices.toList()
        val selectedCards = selected.map { cards[it] }
        val matchScore = selectedCards.first().match(selectedCards.drop(1).toTypedArray())
        if (matchScore > 0) {
            score += matchScore * MATCH_BONUS
            selectedCards.forEach {
                it.isChosen = false
                it.isMatched = true
            }
            chosenIndices.clear()
            matchedSets++
            return FlipResult(Action.MATCHED, selected)
        }

        score -= MISMATCH_PENALTY
        pendingMismatch = selected
        return FlipResult(Action.MISMATCHED, selected)
    }

    /** 把配对失败的三张牌一起翻回背面（由控制器延时调用） */
    fun hidePendingMismatch() {
        val pending = pendingMismatch ?: return
        pending.forEach { cards[it].isChosen = false }
        chosenIndices.clear()
        pendingMismatch = null
    }

    /**
     * 找出与下标 index 的牌点数相同、可以和它配成一组的两张牌的下标，主要供测试使用。
     */
    fun indicesOfSet(index: Int): List<Int> = cards.indices
        .filter { it != index && cards[it].rank == cards[index].rank }
        .take(CARDS_PER_MATCH - 1)

    /** 重新发牌：牌桌上的牌重新洗过，所有状态清零 */
    private fun deal() {
        val deck = Deck(random)
        val board = mutableListOf<Card>()
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
