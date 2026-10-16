package cn.edu.sicnu.cs.heshuyu.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** 二级 Activity 4：秒表，横竖屏切换后继续计时 */
class Activity4 : AppCompatActivity() {

    companion object {
        // onSaveInstanceState 的 Bundle key，不显示给用户
        private const val KEY_ELAPSED = "key_elapsed"
        private const val KEY_RUNNING = "key_running"

        /** 刷新间隔（毫秒），30ms 足够顺滑 */
        private const val TICK_INTERVAL = 30L
    }

    private lateinit var tvTime: TextView
    private val handler = Handler(Looper.getMainLooper())

    /** 已累计的毫秒数（暂停时保持不变） */
    private var elapsedMillis = 0L

    /** 本轮计时的起点：elapsedRealtime - elapsedMillis */
    private var startBase = 0L

    private var isRunning = false

    private val ticker = object : Runnable {
        override fun run() {
            updateTimeText()
            handler.postDelayed(this, TICK_INTERVAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_4)

        tvTime = findViewById(R.id.tv_time)

        // ===== 关键：恢复旋转前保存的状态 =====
        if (savedInstanceState != null) {
            elapsedMillis = savedInstanceState.getLong(KEY_ELAPSED, 0L)
            isRunning = savedInstanceState.getBoolean(KEY_RUNNING, false)
        }
        if (isRunning) {
            // 用 elapsedRealtime 重新对齐起点，旋转耗时也照算，计时不断
            startBase = SystemClock.elapsedRealtime() - elapsedMillis
        }
        updateTimeText()

        findViewById<Button>(R.id.btn_start).setOnClickListener { start() }
        findViewById<Button>(R.id.btn_pause).setOnClickListener { pause() }
        findViewById<Button>(R.id.btn_reset).setOnClickListener { reset() }
    }

    private fun start() {
        if (isRunning) return
        startBase = SystemClock.elapsedRealtime() - elapsedMillis
        isRunning = true
        handler.post(ticker)
    }

    private fun pause() {
        if (!isRunning) return
        elapsedMillis = SystemClock.elapsedRealtime() - startBase
        isRunning = false
        handler.removeCallbacks(ticker)
        updateTimeText()
    }

    private fun reset() {
        handler.removeCallbacks(ticker)
        isRunning = false
        elapsedMillis = 0L
        startBase = 0L
        updateTimeText()
    }

    private fun updateTimeText() {
        val total = if (isRunning) SystemClock.elapsedRealtime() - startBase else elapsedMillis
        val totalSeconds = total / 1000
        val hours = totalSeconds / 3600
        val minutes = totalSeconds % 3600 / 60
        val seconds = totalSeconds % 60
        tvTime.text = getString(R.string.time_format, hours, minutes, seconds)
    }

    // ===== 保存状态：旋转 / 被系统回收前调用 =====
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(
            KEY_ELAPSED,
            if (isRunning) SystemClock.elapsedRealtime() - startBase else elapsedMillis
        )
        outState.putBoolean(KEY_RUNNING, isRunning)
    }

    // 只停刷新，不改计时状态，这样旋转过程中时间照走
    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    override fun onResume() {
        super.onResume()
        if (isRunning) handler.post(ticker)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(ticker)
    }
}