package cn.edu.sicnu.cs.heshuyu.gameapp20.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import cn.edu.sicnu.cs.heshuyu.gameapp20.R
import com.google.android.material.card.MaterialCardView

/**
 * 牌桌适配器（View 层）。
 *
 * 继承 [RecyclerView.Adapter] 并重载三个必需的方法：
 * [getItemCount]、[onCreateViewHolder]、[onBindViewHolder]；
 * 另外重载了带 payload 的 [onBindViewHolder]，用于播放翻牌动画。
 *
 * 点击事件通过 [OnCardClickListener] 回调给控制器（GameActivity），
 * 适配器本身只负责「把传入的牌数据画出来」，不参与任何游戏规则计算（MVC 的 View 层）。
 */
class CardAdapter(
    private val onCardClickListener: OnCardClickListener
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    /**
     * 一张牌在界面上的显示状态，由控制器根据 Model 计算后交给适配器。
     *
     * @param label 牌面上的文字（如「♥A」），背面时不会被显示
     * @param faceUp 是否正面朝上
     * @param matched 是否已经配对成功
     * @param isRed 是否是红色花色（决定牌面文字颜色）
     */
    data class CardItem(
        val label: String,
        val faceUp: Boolean,
        val matched: Boolean,
        val isRed: Boolean
    )

    /** 卡片点击回调，由控制器实现；适配器在用户点击某张牌时调用它 */
    fun interface OnCardClickListener {
        fun onCardClick(position: Int)
    }

    /** 牌的 ViewHolder：持有整张牌的容器和牌面文字 */
    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView as MaterialCardView
        val face: TextView = itemView.findViewById(R.id.tvCardFace)
    }

    private val items = mutableListOf<CardItem>()

    /** 每张牌的高度（像素），由控制器根据屏幕大小计算后设置，用于让牌桌铺满可用区域 */
    var cardHeight: Int = 0

    /**
     * 提交最新的牌桌状态。
     *
     * 只有状态发生变化的牌才会调用 [notifyItemChanged]（带 payload），
     * 这样翻牌时只重绘被点击的那几张牌，并能播放翻转动画。
     * 牌数变化（重新开始、旋转屏幕）时整桌换牌，此时整表刷新是合理的选择。
     */
    @SuppressLint("NotifyDataSetChanged")
    fun submitCards(newItems: List<CardItem>) {
        if (newItems.size != items.size) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
            return
        }
        for (position in newItems.indices) {
            if (items[position] != newItems[position]) {
                items[position] = newItems[position]
                notifyItemChanged(position, PAYLOAD_CARD_CHANGED)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        val holder = CardViewHolder(view)
        // 回调处理：把点击事件转交给控制器，并检查下标是否已经失效
        view.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onCardClickListener.onCardClick(position)
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.cardView.animate().cancel()
        holder.cardView.rotationY = 0f
        applyCardHeight(holder)
        bindCard(holder, position)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }
        applyCardHeight(holder)
        animateFlip(holder, position)
    }

    /** 按控制器算好的高度设置牌的大小，使牌桌在竖屏 / 横屏下都能铺满 */
    private fun applyCardHeight(holder: CardViewHolder) {
        if (cardHeight <= 0) {
            return
        }
        val params = holder.itemView.layoutParams
        if (params.height != cardHeight) {
            params.height = cardHeight
            holder.itemView.layoutParams = params
        }
    }

    /** 播放翻牌动画：牌先转到侧面（90°），换好牌面后再转回正面 */
    private fun animateFlip(holder: CardViewHolder, position: Int) {
        if (position !in items.indices) {
            return
        }
        val cardView = holder.cardView
        cardView.animate().cancel()
        cardView.rotationY = 0f
        // 还没完成布局、或这条 ViewHolder 已经被复用去显示别的牌时，直接刷新牌面，不播放动画
        if (!cardView.isLaidOut || holder.bindingAdapterPosition != position) {
            bindCard(holder, position)
            return
        }
        cardView.animate()
            .rotationY(90f)
            .setDuration(FLIP_HALF_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                // 动画结束时再次确认 ViewHolder 仍然显示这张牌，避免绑定到被复用的牌上
                if (holder.bindingAdapterPosition == position) {
                    bindCard(holder, position)
                }
                cardView.rotationY = -90f
                cardView.animate()
                    .rotationY(0f)
                    .setDuration(FLIP_HALF_DURATION)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            .start()
    }

    /** 设置牌面（背面 / 正面 / 已配对）的颜色、文字与无障碍描述 */
    private fun bindCard(holder: CardViewHolder, position: Int) {
        val item = items.getOrNull(position) ?: return
        val context = holder.itemView.context
        val cardView = holder.cardView
        val face = holder.face
        val number = position + 1

        when {
            !item.faceUp -> {
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.card_back_face)
                )
                cardView.strokeColor = ContextCompat.getColor(context, R.color.card_back_edge)
                face.setTextColor(ContextCompat.getColor(context, R.color.card_back_text))
                face.text = context.getString(R.string.card_back_symbol)
                cardView.contentDescription =
                    context.getString(R.string.card_desc_back, number)
            }

            item.matched -> {
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.card_matched_face)
                )
                cardView.strokeColor = ContextCompat.getColor(context, R.color.card_matched_edge)
                face.setTextColor(ContextCompat.getColor(context, R.color.card_matched_text))
                face.text = item.label
                cardView.contentDescription =
                    context.getString(R.string.card_desc_matched, number, item.label)
            }

            else -> {
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.card_front_face)
                )
                cardView.strokeColor = ContextCompat.getColor(context, R.color.card_front_edge)
                face.setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (item.isRed) R.color.suit_red else R.color.suit_black
                    )
                )
                face.text = item.label
                cardView.contentDescription =
                    context.getString(R.string.card_desc_front, number, item.label)
            }
        }
        // 已经配成组的牌不可再点击
        cardView.isClickable = !item.matched
    }

    private companion object {
        /** 牌面状态发生变化的 payload，用于触发翻牌动画 */
        const val PAYLOAD_CARD_CHANGED = "payload_card_changed"

        /** 翻牌动画一半的时长（毫秒）：转到 90° 再转回来 */
        const val FLIP_HALF_DURATION = 130L
    }
}
