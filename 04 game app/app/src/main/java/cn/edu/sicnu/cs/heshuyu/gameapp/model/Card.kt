package cn.edu.sicnu.cs.heshuyu.gameapp.model

/**
 * 一张牌（Model 层）。
 *
 * 参照实验附件中的 Card 类实现：保存花色、点数，以及「是否被翻开」和「是否已经配对」两个状态，
 * 并提供 [match] 方法计算两张牌之间的匹配得分。
 */
class Card(
    val suit: String,
    val rank: String,
    var isChosen: Boolean = false,
    var isMatched: Boolean = false
) {
    companion object {
        /** 13 种点数 */
        @JvmField
        val rankStrings = arrayOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")

        /** 4 种花色 */
        @JvmField
        val validSuits = arrayOf("♥", "♦", "♠", "♣")

        /** 红色的花色 */
        @JvmField
        val redSuits = arrayOf("♥", "♦")

        /**
         * 匹配得分：点数相同得 4 分，仅花色相同得 1 分，都不相同得 0 分。
         */
        fun scoreBetween(first: Card, second: Card): Int = when {
            first.rank == second.rank -> 4
            first.suit == second.suit -> 1
            else -> 0
        }
    }

    /** 是否为红色牌（红桃、方块），界面据此设置文字颜色 */
    val isRed: Boolean
        get() = suit == redSuits[0] || suit == redSuits[1]

    /** 牌面上的文字，例如「♥A」 */
    override fun toString(): String = suit + rank

    /**
     * 与一组牌比较，返回匹配得分，规则与附件代码一致。
     */
    fun match(otherCards: Array<Card>): Int {
        if (otherCards.size != 1) {
            return 0
        }
        return scoreBetween(this, otherCards[0])
    }
}
