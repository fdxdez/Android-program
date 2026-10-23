package cn.edu.sicnu.cs.heshuyu.gameapp20.data

/** 性别枚举，界面文本由字符串资源提供 */
enum class Gender {
    MALE,
    FEMALE
}

/**
 * 玩家信息。
 *
 * 个人信息只需要保存在内存中，不写入文件，因此使用普通的数据类。
 */
data class Player(
    var name: String,
    var gender: Gender,
    var phone: String
)
