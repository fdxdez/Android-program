package cn.edu.sicnu.cs.heshuyu.fragment.data

import java.io.Serializable

/**
 * 玩家个人信息（Model 层）。
 *
 * 实验六要求「无论是个人信息还是翻牌游戏都需要采用序列化的方法存储历史信息」，
 * 因此本类实现 [Serializable] 接口，可以整体写进 `openFileOutput()` 得到的文件里，
 * 下次打开应用时再用 `ObjectInputStream` 读回来。
 */
data class Player(
    /** 姓名 */
    var name: String = DEFAULT_NAME,
    /** 学号 */
    var studentId: String = DEFAULT_STUDENT_ID,
    /** 性别，用字符串保存，便于序列化与显示 */
    var gender: String = GENDER_MALE,
    /** 电话 */
    var phone: String = DEFAULT_PHONE,
    /** 是否已经保存过（用于区分「首次进入」与「已填写」两种情况） */
    var saved: Boolean = false
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L

        /** 默认姓名 */
        const val DEFAULT_NAME = "heshuyu"

        /** 默认学号 */
        const val DEFAULT_STUDENT_ID = "2024180411"

        /** 默认电话 */
        const val DEFAULT_PHONE = "13800138000"

        /** 性别：男 */
        const val GENDER_MALE = "男"

        /** 性别：女 */
        const val GENDER_FEMALE = "女"
    }
}
