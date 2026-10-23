package cn.edu.sicnu.cs.heshuyu.gameapp20.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 网格布局的间距装饰器（View 层）。
 *
 * [GridLayoutManager] 本身不支持设置行列间距，这里给每个 item 加上四边的偏移，
 * 让牌与牌之间、牌与屏幕边缘之间都保持一致的间距。
 */
class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION || spanCount <= 0) {
            return
        }
        val column = position % spanCount
        outRect.left = spacing - column * spacing / spanCount
        outRect.right = (column + 1) * spacing / spanCount
        outRect.top = if (position < spanCount) spacing else 0
        outRect.bottom = spacing
    }
}
