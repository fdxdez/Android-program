package cn.edu.sicnu.cs.heshuyu.music.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 时长格式化工具 [TrackAdapter.formatDuration] 的单元测试。
 */
class TrackAdapterFormatTest {

    @Test
    fun `零毫秒格式化为 0 比 00`() {
        assertEquals("0:00", TrackAdapter.formatDuration(0L))
    }

    @Test
    fun `不足十分钟时补零`() {
        assertEquals("0:05", TrackAdapter.formatDuration(5_000L))
        assertEquals("1:05", TrackAdapter.formatDuration(65_000L))
        assertEquals("3:07", TrackAdapter.formatDuration(187_000L))
    }

    @Test
    fun `超过十分钟时不补零`() {
        assertEquals("12:34", TrackAdapter.formatDuration(754_000L))
    }

    @Test
    fun `负数按零处理`() {
        assertEquals("0:00", TrackAdapter.formatDuration(-100L))
    }
}
