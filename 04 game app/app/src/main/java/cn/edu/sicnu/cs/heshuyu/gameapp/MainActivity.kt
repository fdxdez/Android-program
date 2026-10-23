package cn.edu.sicnu.cs.heshuyu.gameapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import cn.edu.sicnu.cs.heshuyu.gameapp.data.Gender
import cn.edu.sicnu.cs.heshuyu.gameapp.data.PlayerRepository
import cn.edu.sicnu.cs.heshuyu.gameapp.ui.WindowInsetsHelper
import com.google.android.material.button.MaterialButton

/**
 * 主界面：显示当前玩家的基本信息（姓名、性别、电话、当前最高分），
 * 并进入「修改个人信息」和「翻牌游戏」两个二级界面。
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        WindowInsetsHelper.apply(this, findViewById(R.id.root), findViewById(R.id.toolbar))

        PlayerRepository.init(this)

        findViewById<MaterialButton>(R.id.btnStartGame).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnEditInfo).setOnClickListener {
            startActivity(Intent(this, EditInfoActivity::class.java))
        }
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
