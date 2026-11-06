package cn.edu.sicnu.cs.heshuyu.fragment

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.edu.sicnu.cs.heshuyu.fragment.data.CardState
import cn.edu.sicnu.cs.heshuyu.fragment.ui.CardAdapter
import cn.edu.sicnu.cs.heshuyu.fragment.ui.GridSpacingItemDecoration

/**
 * 两个翻牌游戏 Fragment 的共同父类（Controller 层）。
 *
 * 把「牌桌适配器、列数、每张牌的高度计算、统计数据刷新、翻牌动画」等重复代码集中在这里，
 * [TwoCardGameFragment]（两张牌）与 [ThreeCardGameFragment]（三张牌）只需要提供各自的
 * 模型、以及处理「配对失败后延时翻回」的差异。
 *
 * 两个子类都实现 [CardAdapter.OnCardClickListener]，在用户点击牌时调用模型计算并刷新界面。
 */
abstract class BaseCardGameFragment : Fragment(), CardAdapter.OnCardClickListener {

    /** 牌桌适配器（View 层） */
    protected val cardAdapter = CardAdapter(this)

    protected lateinit var recyclerCards: RecyclerView
    protected lateinit var tvScore: TextView
    protected lateinit var tvFlips: TextView
    protected lateinit var tvSets: TextView
    protected lateinit var tvStatus: TextView

    /** 牌桌列数，由子类给出（两张牌 4 列、三张牌 6 列） */
    protected abstract val columns: Int

    /** 牌桌上的牌数 */
    protected abstract val cardCount: Int

    /** 取牌桌上所有的牌（由子类从各自的模型里读） */
    protected abstract fun cardsOfGame(): List<CardState>

    /** 下标为 index 的牌当前是否正面朝上 */
    protected abstract fun isFaceUp(index: Int): Boolean

    /** 刷新分数、翻牌次数、配对进度三个统计值 */
    protected abstract fun updateStats()

    /** 某张牌是否已经配对成功 */
    protected fun isMatched(index: Int): Boolean = cardsOfGame()[index].matched

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerCards = view.findViewById(R.id.recyclerCards)
        tvScore = view.findViewById(R.id.tvScore)
        tvFlips = view.findViewById(R.id.tvFlips)
        tvSets = view.findViewById(R.id.tvSets)
        tvStatus = view.findViewById(R.id.tvStatus)

        recyclerCards.layoutManager = GridLayoutManager(view.context, columns)
        recyclerCards.adapter = cardAdapter
        // 翻牌动画由适配器完成，关闭默认 item 动画，避免两种动画叠加
        recyclerCards.itemAnimator = null
        recyclerCards.addItemDecoration(
            GridSpacingItemDecoration(
                columns,
                resources.getDimensionPixelSize(R.dimen.card_spacing)
            )
        )
        recyclerCards.contentDescription = getString(R.string.board_desc_format, cardCount)
        // 牌桌尺寸变化（首次显示、切换 Fragment、显示/隐藏系统栏）时重新计算牌的大小
        recyclerCards.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val sizeChanged = (right - left) != (oldRight - oldLeft) ||
                (bottom - top) != (oldBottom - oldTop)
            if (sizeChanged) {
                applyCardHeight()
            }
        }
        renderBoard()
        updateStats()
    }

    /** 用户在 RecyclerView 中点击了第 position 张牌（由 [CardAdapter] 回调） */
    override fun onCardClick(position: Int) {
        onBoardClicked(position)
    }

    /** 处理一次翻牌点击，由子类实现 */
    protected abstract fun onBoardClicked(position: Int)

    /** 把模型状态转换成适配器需要的显示数据 */
    protected fun renderBoard() {
        cardAdapter.submitCards(
            cardsOfGame().mapIndexed { index, card ->
                CardAdapter.CardItem(
                    label = card.suit + card.rank,
                    faceUp = isFaceUp(index),
                    matched = card.matched,
                    isRed = card.suit == RED_SUIT_HEART || card.suit == RED_SUIT_DIAMOND
                )
            }
        )
    }

    /**
     * 按「行数 + 间距」计算每张牌的高度并同步给适配器与已经创建的牌，
     * 让牌桌刚好铺满可用区域（竖屏 4 行，横屏按列数自动成为更少的行）。
     */
    protected fun applyCardHeight() {
        if (!::recyclerCards.isInitialized || recyclerCards.height <= 0) {
            return
        }
        val rows = (cardCount + columns - 1) / columns
        if (rows <= 0) {
            return
        }
        val spacing = resources.getDimensionPixelSize(R.dimen.card_spacing)
        val gaps = spacing * (rows + 1)
        val available = recyclerCards.height -
            recyclerCards.paddingTop -
            recyclerCards.paddingBottom -
            gaps
        val height = (available / rows)
            .coerceAtLeast(resources.getDimensionPixelSize(R.dimen.card_min_height))
        if (height == cardAdapter.cardHeight) {
            return
        }
        cardAdapter.cardHeight = height
        for (index in 0 until recyclerCards.childCount) {
            val child = recyclerCards.getChildAt(index)
            val params: ViewGroup.LayoutParams = child.layoutParams
            if (params.height != height) {
                params.height = height
                child.layoutParams = params
            }
        }
        recyclerCards.requestLayout()
    }

    private companion object {
        const val RED_SUIT_HEART = "♥"
        const val RED_SUIT_DIAMOND = "♦"
    }
}
