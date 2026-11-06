package cn.edu.sicnu.cs.heshuyu.music.data

/**
 * 一首歌曲（Model 层）。
 *
 * [MediaStoreHelper] 用 ContentResolver 从系统媒体库查询出这些字段后构造本对象，
 * 播放服务（[cn.edu.sicnu.cs.heshuyu.music.service.MusicPlayerService]）与界面
 * （[cn.edu.sicnu.cs.heshuyu.music.MainActivity]）之间传递的就是它。
 *
 * 注意：媒体地址用字符串保存而不是 `android.net.Uri`，
 * 这样本类和 [Playlist] 都是纯 Kotlin 数据，可以直接跑 JVM 单元测试；
 * 需要 Uri 时由服务用 `MediaPlayer.setDataSource(context, Uri.parse(uri))` 转换。
 *
 * @param id 媒体库中的记录 id（MediaStore.Audio.Media._ID）
 * @param title 歌曲名
 * @param artist 歌手
 * @param album 专辑
 * @param duration 时长（毫秒）
 * @param uri 播放用的 content:// 地址
 */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: String
) {
    /** 界面上显示的一行文字：「歌曲名 - 歌手」 */
    val displayName: String
        get() = if (artist.isBlank()) title else "$title - $artist"
}
