package cn.edu.sicnu.cs.heshuyu.fragment.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 给 RecyclerView 的网格布局加等距间隔（View 层的辅助类）。
 *
 * 普通 ItemDecoration 只加一侧间距会让边缘留白不一致，
 * 这里给每个 item 上下左右各留一半间距，整体看上去间距均匀。
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
        if (position == RecyclerView.NO_POSITION) {
            return
        }
        val column = position % spanCount
        outRect.left = spacing * column / spanCount
        outRect.right = spacing - (column + 1) * spacing / spanCount
        if (position >= spanCount) {
            outRect.top = spacing
        }
    }
}
