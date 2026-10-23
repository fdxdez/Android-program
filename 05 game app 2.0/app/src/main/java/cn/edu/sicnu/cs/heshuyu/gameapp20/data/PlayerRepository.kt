package cn.edu.sicnu.cs.heshuyu.gameapp20.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 玩家信息仓库：在内存中保存当前玩家信息，并用 SharedPreferences 保存「当前最高分」。
 *
 * 姓名、性别、电话只保存在内存里；最高分属于游戏成绩，写入 SharedPreferences，退出应用后仍然保留。
 */
object PlayerRepository {

    /** 学号，显示在主界面副标题与「关于」对话框中 */
    const val STUDENT_ID = "2024180411"

    private const val PREFERENCE_NAME = "game_app_20_prefs"
    private const val KEY_HIGH_SCORE = "key_high_score"

    /** 默认玩家：姓名 heshuyu，学号 2024180411 */
    var player: Player = Player(name = "heshuyu", gender = Gender.MALE, phone = "13800138000")
        private set

    /** 当前最高分，初始为 0 */
    var highScore: Int = 0
        private set

    private var preferences: SharedPreferences? = null

    /** 在 Activity 中初始化，读取已保存的最高分 */
    fun init(context: Context) {
        if (preferences == null) {
            preferences = context.applicationContext
                .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        }
        highScore = preferences?.getInt(KEY_HIGH_SCORE, 0) ?: 0
    }

    /** 修改玩家信息（只保存在内存中） */
    fun updatePlayer(name: String, gender: Gender, phone: String) {
        player = Player(name = name, gender = gender, phone = phone)
    }

    /**
     * 记录一局游戏的分数。
     *
     * @return 如果刷新了最高分返回 true，否则返回 false
     */
    fun recordScore(score: Int): Boolean {
        if (score <= highScore) {
            return false
        }
        highScore = score
        preferences?.edit { putInt(KEY_HIGH_SCORE, highScore) }
        return true
    }

    /** 仅供测试使用：把内存中的状态恢复为默认值 */
    fun resetForTest() {
        player = Player(name = "heshuyu", gender = Gender.MALE, phone = "13800138000")
        highScore = 0
    }
}
