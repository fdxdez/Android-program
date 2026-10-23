package cn.edu.sicnu.cs.heshuyu.gameapp.ui

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 沉浸式（edge-to-edge）界面适配。
 *
 * Android 15 起，targetSdk 35+ 的应用会强制使用 edge-to-edge 显示，
 * 这里把状态栏的高度加到自定义标题栏（Toolbar）上，让状态栏与标题栏颜色一致，
 * 同时让根布局避开底部导航栏，避免标题或按钮被系统栏遮住。
 */
object WindowInsetsHelper {

    fun apply(activity: Activity, root: View, toolbar: View) {
        val actionBarSize = resolveActionBarSize(activity)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = toolbar.layoutParams
            if (params != null) {
                val targetHeight = actionBarSize + bars.top
                if (params.height != targetHeight) {
                    params.height = targetHeight
                    toolbar.layoutParams = params
                }
            }
            toolbar.setPadding(
                toolbar.paddingLeft,
                bars.top,
                toolbar.paddingRight,
                toolbar.paddingBottom
            )
            view.setPadding(0, 0, 0, bars.bottom)
            insets
        }
    }

    private fun resolveActionBarSize(context: Context): Int {
        val typedValue = TypedValue()
        if (context.theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            return TypedValue.complexToDimensionPixelSize(
                typedValue.data,
                context.resources.displayMetrics
            )
        }
        return (56 * context.resources.displayMetrics.density).toInt()
    }
}
