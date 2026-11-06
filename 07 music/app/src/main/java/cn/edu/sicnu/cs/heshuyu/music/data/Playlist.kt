package cn.edu.sicnu.cs.heshuyu.music.data

/**
 * 播放列表的纯逻辑部分（Model 层），不依赖任何 Android 组件，方便单元测试。
 *
 * 只维护「歌曲列表 + 当前下标」，上一首 / 下一首的下标计算都在这里完成，
 * 真正的播放由 [cn.edu.sicnu.cs.heshuyu.music.service.MusicPlayerService] 负责。
 */
class Playlist {

    private val tracks = mutableListOf<Track>()

    /** 当前播放歌曲的下标，没有歌曲时为 -1 */
    var currentIndex: Int = -1
        private set

    /** 播放列表中的歌曲 */
    val items: List<Track>
        get() = tracks

    /** 是否为空列表 */
    val isEmpty: Boolean
        get() = tracks.isEmpty()

    /** 歌曲数量 */
    val size: Int
        get() = tracks.size

    /** 当前歌曲，没有则为 null */
    val currentTrack: Track?
        get() = tracks.getOrNull(currentIndex)

    /** 用新查询到的列表替换整个播放列表，并把下标复位到第一首 */
    fun replaceAll(newTracks: List<Track>) {
        tracks.clear()
        tracks.addAll(newTracks)
        currentIndex = if (tracks.isEmpty()) INDEX_NONE else 0
    }

    /** 选中的歌曲下标（点击列表某一项时调用），下标非法时返回 false */
    fun select(index: Int): Boolean {
        if (index !in tracks.indices) {
            return false
        }
        currentIndex = index
        return true
    }

    /** 下一首的下标；已经是最后一首时返回 null（表示列表播完，停止播放） */
    fun nextIndex(): Int? {
        if (tracks.isEmpty()) {
            return null
        }
        val next = currentIndex + 1
        return if (next in tracks.indices) next else null
    }

    /** 上一首的下标；已经是第一首时返回 null */
    fun previousIndex(): Int? {
        if (tracks.isEmpty()) {
            return null
        }
        val previous = currentIndex - 1
        return if (previous in tracks.indices) previous else null
    }

    companion object {
        /** 没有选中歌曲时的下标 */
        const val INDEX_NONE = -1
    }
}
