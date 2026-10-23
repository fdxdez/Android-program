package cn.edu.sicnu.cs.heshuyu.gameapp20.model

/**
 * 一张牌（Model 层）。
 *
 * 保存花色、点数，以及「是否被翻开」和「是否已经配对」两个状态，
 * 并提供 [match] 方法计算「一张牌与另外两张牌」之间的匹配得分。
 *
 * 实验五要求把游戏改成三张牌配对，所以 [match] 由「两张牌比较」改为「三张牌比较」：
 * 三张牌点数全部相同才算配对成功。
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

        /** 三张牌点数相同时的匹配得分（每张点数相同的牌记 1 分） */
        const val SCORE_THREE_OF_A_KIND = 3

        /** 参与一次配对比较的牌数（含自己），即三张牌游戏 */
        const val CARDS_PER_MATCH = 3
    }

    /** 是否为红色牌（红桃、方块），界面据此设置文字颜色 */
    val isRed: Boolean
        get() = suit == redSuits[0] || suit == redSuits[1]

    /** 牌面上的文字，例如「♥A」 */
    override fun toString(): String = suit + rank

    /**
     * 与另外几张牌比较，返回匹配得分。
     *
     * 三张牌游戏规则：本牌与 [otherCards] 中的两张牌点数全部相同 → [SCORE_THREE_OF_A_KIND] 分；
     * 其它情况（点数不同、张数不是两张）→ 0 分。
     */
    fun match(otherCards: Array<Card>): Int {
        if (otherCards.size != CARDS_PER_MATCH - 1) {
            return 0
        }
        val allSameRank = otherCards.all { it.rank == rank }
        return if (allSameRank) SCORE_THREE_OF_A_KIND else 0
    }
}
