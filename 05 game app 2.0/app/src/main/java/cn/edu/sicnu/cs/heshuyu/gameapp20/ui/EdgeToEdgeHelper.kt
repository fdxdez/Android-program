package cn.edu.sicnu.cs.heshuyu.gameapp20.ui

import android.graphics.Color
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 边到边（edge-to-edge）显示适配。
 *
 * Android 15 起，targetSdk 35+ 的应用会被强制使用边到边显示：窗口铺满整个屏幕，
 * 状态栏、导航栏都盖在内容上面。这时如果什么都不做，页面顶部会被标题栏遮挡
 * （内容的第一行会被 ActionBar 压住），底部会被导航栏遮挡。
 *
 * 这里统一处理：
 * 1. [enable]：状态栏、导航栏设为透明，状态栏图标保持白色（下面就是主题色的 ActionBar）；
 * 2. [fitContent]：给根布局让出系统栏的位置——
 *    - 左右、底部：直接使用系统栏的 inset；
 *    - 顶部：量出 ActionBar 真正的底边再下移，避免内容被标题栏挡住。
 *    顶部之所以不能直接用系统栏的 inset，是因为使用 DarkActionBar 主题时，
 *    AppCompat 让 ActionBar 盖在内容之上（它自己加上了状态栏高度的 padding），
 *    只把其余的系统栏 inset 传给内容，所以必须按标题栏的实际高度让位。
 */
object EdgeToEdgeHelper {

    fun enable(activity: ComponentActivity) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
    }

    fun fitContent(activity: AppCompatActivity, root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            // 刘海屏在横屏时挖孔在左右两侧，也要让位，所以把 displayCutout 一起算进来
            val types = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            val bars = insets.getInsets(types)
            view.setPadding(bars.left, view.paddingTop, bars.right, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
        moveBelowActionBar(activity, root)
    }

    /**
     * 在第一次绘制之前把内容下移到 ActionBar 的下方。
     *
     * 放在 onPreDraw 里做，是因为要等布局完成才能拿到 ActionBar 的真实位置；
     * 调整完返回 false 表示「先别画，重新布局一次」，所以用户看不到跳动。
     */
    private fun moveBelowActionBar(activity: AppCompatActivity, root: View) {
        root.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    val observer = root.viewTreeObserver
                    if (observer.isAlive) {
                        observer.removeOnPreDrawListener(this)
                    }
                    return !applyActionBarOffset(activity, root)
                }
            }
        )
    }

    /** @return true 表示顶部间距被调整过，需要重新布局后再绘制 */
    private fun applyActionBarOffset(activity: AppCompatActivity, root: View): Boolean {
        val actionBar = activity.findViewById<View>(androidx.appcompat.R.id.action_bar_container)
            ?: return false
        if (actionBar.height == 0) {
            return false
        }
        val rootPosition = IntArray(2).also { root.getLocationInWindow(it) }
        val actionBarPosition = IntArray(2).also { actionBar.getLocationInWindow(it) }
        val offset = actionBarPosition[1] + actionBar.height - rootPosition[1]
        if (offset <= root.paddingTop) {
            return false
        }
        root.setPadding(root.paddingLeft, offset, root.paddingRight, root.paddingBottom)
        return true
    }
}
