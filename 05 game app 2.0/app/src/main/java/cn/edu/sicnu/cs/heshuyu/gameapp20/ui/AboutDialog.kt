package cn.edu.sicnu.cs.heshuyu.gameapp20.ui

import android.content.Context
import cn.edu.sicnu.cs.heshuyu.gameapp20.R
import cn.edu.sicnu.cs.heshuyu.gameapp20.data.PlayerRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 「关于」对话框。
 *
 * 实验五要求：在主界面菜单中点击「关于」后弹出关于对话框，
 * 内容包含应用名称（姓名 + 学号）、作者、学号、版本和玩法说明。
 * 所有文案都来自字符串资源，避免在代码中写死文字。
 */
object AboutDialog {

    fun show(context: Context) {
        val message = listOf(
            context.getString(R.string.about_app_line, context.getString(R.string.app_name)),
            context.getString(R.string.about_author_line, PlayerRepository.player.name),
            context.getString(R.string.about_student_id_line, PlayerRepository.STUDENT_ID),
            context.getString(R.string.about_version_line, context.getString(R.string.app_version)),
            "",
            context.getString(R.string.about_rules_title),
            context.getString(R.string.about_rules_content)
        ).joinToString("\n")

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.about_title)
            .setMessage(message)
            .setPositiveButton(R.string.dialog_ok, null)
            .show()
    }
}
