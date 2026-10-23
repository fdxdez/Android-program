package cn.edu.sicnu.cs.heshuyu.gameapp20

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cn.edu.sicnu.cs.heshuyu.gameapp20.data.Gender
import cn.edu.sicnu.cs.heshuyu.gameapp20.data.PlayerRepository
import cn.edu.sicnu.cs.heshuyu.gameapp20.ui.AboutDialog
import cn.edu.sicnu.cs.heshuyu.gameapp20.ui.EdgeToEdgeHelper
import com.google.android.material.button.MaterialButton

/**
 * 主界面（Controller）。
 *
 * 1. 显示当前玩家的基本信息（姓名、学号、性别、电话、当前最高分）；
 * 2. 通过菜单「关于」弹出关于对话框（实验五要求）；
 * 3. 进入「翻牌游戏」和「修改个人信息」两个二级界面。
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeHelper.enable(this)
        setContentView(R.layout.activity_main)
        EdgeToEdgeHelper.fitContent(this, findViewById(R.id.root))

        PlayerRepository.init(this)

        findViewById<MaterialButton>(R.id.btnStartGame).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnEditInfo).setOnClickListener {
            startActivity(Intent(this, EditInfoActivity::class.java))
        }
    }

    /**
     * 给主界面添加菜单：菜单资源中只有「关于」一项，
     * 它会出现在主题自带 ActionBar 的溢出菜单中。
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /** 点击菜单项后弹出关于对话框 */
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_about -> {
            AboutDialog.show(this)
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    /** 从修改界面返回时自动刷新，保证主界面显示的是最新信息 */
    override fun onResume() {
        super.onResume()
        renderPlayerInfo()
    }

    private fun renderPlayerInfo() {
        val player = PlayerRepository.player

        findViewById<TextView>(R.id.tvAvatar).text = player.name
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString()
            ?: getString(R.string.avatar_placeholder)
        findViewById<TextView>(R.id.tvName).text =
            player.name.ifBlank { getString(R.string.main_unknown) }
        findViewById<TextView>(R.id.tvStudentId).text =
            getString(R.string.main_subtitle_format, PlayerRepository.STUDENT_ID)
        findViewById<TextView>(R.id.tvGender).text = getString(
            if (player.gender == Gender.MALE) R.string.gender_male else R.string.gender_female
        )
        findViewById<TextView>(R.id.tvPhone).text =
            player.phone.ifBlank { getString(R.string.main_unknown) }
        findViewById<TextView>(R.id.tvHighScore).text =
            if (PlayerRepository.highScore > 0) {
                getString(R.string.score_value_format, PlayerRepository.highScore)
            } else {
                getString(R.string.high_score_empty)
            }
    }
}
