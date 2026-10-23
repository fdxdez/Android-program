package cn.edu.sicnu.cs.heshuyu.gameapp.model

import java.util.Random

/**
 * 一副 52 张的牌（Model 层），参照实验附件中的 Deck 类实现。
 *
 * [random] 可以注入固定的随机种子，方便在单元测试中复现结果。
 */
class Deck(private val random: Random = Random()) {

    private val cards = mutableListOf<Card>()

    init {
        for (suit in Card.validSuits) {
            for (rank in Card.rankStrings) {
                cards.add(Card(suit = suit, rank = rank))
            }
        }
    }

    /** 剩余的牌数 */
    val size: Int
        get() = cards.size

    /** 随机抽一张牌，牌堆为空时返回 null */
    fun drawRandomCard(): Card? {
        if (cards.isEmpty()) {
            return null
        }
        return cards.removeAt(random.nextInt(cards.size))
    }
}
