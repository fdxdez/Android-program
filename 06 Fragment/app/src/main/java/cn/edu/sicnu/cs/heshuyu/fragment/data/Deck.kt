package cn.edu.sicnu.cs.heshuyu.fragment.data

import java.util.Random

/**
 * 一副 52 张的牌（Model 层）。
 *
 * [random] 可以注入固定随机种子，方便单元测试复现结果。
 */
class Deck(private val random: Random = Random()) {

    companion object {
        /** 13 种点数 */
        @JvmField
        val rankStrings = arrayOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")

        /** 4 种花色 */
        @JvmField
        val validSuits = arrayOf("♥", "♦", "♠", "♣")
    }

    private val cards = mutableListOf<CardState>()

    init {
        for (suit in validSuits) {
            for (rank in rankStrings) {
                cards.add(CardState(suit = suit, rank = rank))
            }
        }
    }

    /** 剩余的牌数 */
    val size: Int
        get() = cards.size

    /** 随机抽一张牌，牌堆为空时返回 null */
    fun drawRandomCard(): CardState? {
        if (cards.isEmpty()) {
            return null
        }
        return cards.removeAt(random.nextInt(cards.size))
    }

    /**
     * 一次抽出 [count] 张点数相同、花色互不相同的牌（三张牌游戏发牌时使用）。
     *
     * 先随机挑一个「剩余牌数还够」的点数，再从该点数里随机取 [count] 张，
     * 因此同一组牌既不会重复，点数也完全相同。
     *
     * @return 抽到的牌；剩余牌中已经没有足够的同点数牌时返回空列表
     */
    fun drawCardsOfSameRank(count: Int): List<CardState> {
        if (count <= 0) {
            return emptyList()
        }
        val groups = cards.groupBy { it.rank }.filterValues { it.size >= count }
        if (groups.isEmpty()) {
            return emptyList()
        }
        val rank = groups.keys.elementAt(random.nextInt(groups.size))
        val picked = groups.getValue(rank).shuffled(random).take(count)
        cards.removeAll(picked)
        return picked
    }
}
