package cn.edu.sicnu.cs.heshuyu.fragment.data

import java.io.Serializable

/**
 * 一张牌的可序列化状态（Model 层）。
 *
 * 界面上的牌（View 层）会变化、也会被 RecyclerView 复用，不能直接写文件；
 * 这里只保存「花色、点数、是否翻开、是否已配对」这四个必要字段，
 * 保存与恢复牌桌时都通过它中转。
 */
data class CardState(
    /** 花色，例如「♥」 */
    val suit: String,
    /** 点数，例如「A」 */
    val rank: String,
    /** 是否正面朝上（尚未配对的翻开状态） */
    var faceUp: Boolean = false,
    /** 是否已经配对成功 */
    var matched: Boolean = false
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
