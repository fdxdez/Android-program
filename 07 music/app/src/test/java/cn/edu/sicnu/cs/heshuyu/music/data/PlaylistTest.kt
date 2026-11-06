package cn.edu.sicnu.cs.heshuyu.music.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 播放列表逻辑的单元测试。
 *
 * [Playlist] 与 [Track] 都不依赖 Android 组件（媒体地址是普通字符串），
 * 因此可以直接跑 JVM 单元测试，用 `gradlew test` 即可执行。
 */
class PlaylistTest {

    private fun track(id: Long, title: String) = Track(
        id = id,
        title = title,
        artist = "歌手$id",
        album = "专辑",
        duration = 60_000L * id,
        uri = "content://media/external/audio/media/$id"
    )

    @Test
    fun `替换列表后默认选中第一首`() {
        val playlist = Playlist()
        assertTrue(playlist.isEmpty)
        assertNull(playlist.currentTrack)

        playlist.replaceAll(listOf(track(1, "第一首"), track(2, "第二首")))

        assertEquals(2, playlist.size)
        assertEquals(0, playlist.currentIndex)
        assertEquals("第一首", playlist.currentTrack?.title)
    }

    @Test
    fun `空列表时上一首下一首都没有下标`() {
        val playlist = Playlist()
        playlist.replaceAll(emptyList())

        assertEquals(Playlist.INDEX_NONE, playlist.currentIndex)
        assertNull(playlist.nextIndex())
        assertNull(playlist.previousIndex())
    }

    @Test
    fun `下一首依次前进最后一首之后返回 null`() {
        val playlist = Playlist()
        playlist.replaceAll(listOf(track(1, "A"), track(2, "B"), track(3, "C")))

        assertEquals(1, playlist.nextIndex())
        assertTrue(playlist.select(1))
        assertEquals(2, playlist.nextIndex())
        assertTrue(playlist.select(2))
        // 已经是最后一首
        assertNull(playlist.nextIndex())
    }

    @Test
    fun `上一首依次后退第一首之前返回 null`() {
        val playlist = Playlist()
        playlist.replaceAll(listOf(track(1, "A"), track(2, "B")))
        assertTrue(playlist.select(1))

        assertEquals(0, playlist.previousIndex())
        assertTrue(playlist.select(0))
        // 已经是第一首
        assertNull(playlist.previousIndex())
    }

    @Test
    fun `选择越界下标会失败并且不改变当前歌曲`() {
        val playlist = Playlist()
        playlist.replaceAll(listOf(track(1, "A")))
        playlist.select(0)

        assertFalse(playlist.select(5))
        assertFalse(playlist.select(-1))
        assertEquals("A", playlist.currentTrack?.title)
    }
}
