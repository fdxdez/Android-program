package cn.edu.sicnu.cs.heshuyu.music.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import cn.edu.sicnu.cs.heshuyu.music.MainActivity
import cn.edu.sicnu.cs.heshuyu.music.R
import cn.edu.sicnu.cs.heshuyu.music.data.Playlist
import cn.edu.sicnu.cs.heshuyu.music.data.Track

/**
 * 音乐播放服务（实验七的核心）。
 *
 * 实验要求「所有与音乐播放相关的功能在服务中实现」，因此播放、暂停、停止、
 * 上一首、下一首、跳转进度全部在本 Service 里完成；
 * Activity 通过 [bindService] 拿到 [MusicBinder]，再通过它读取歌曲名、播放进度等信息。
 *
 * 实现要点：
 * 1. 用 [MediaPlayer] 播放 ContentResolver 查询到的 `content://` 音频；
 * 2. 播放时用 `startForeground` 把服务提升为前台服务，避免退到后台被系统回收；
 * 3. 每换一首歌都通过 [NotificationCompat] 把当前歌曲名显示到通知栏（实验要求(4)），
 *    通知上带「上一首 / 播放暂停 / 下一首」三个按钮，并借助
 *    [NotificationCompat.Builder.setOngoing] + [Notification.FLAG_ONGOING_EVENT] 让通知栏
 *    也能控制播放；
 * 4. [progressCallback] 由 Activity 设置，服务每 500 毫秒回调一次播放进度，
 *    供 SeekBar 实时显示（实验要求(3)，多线程：定时任务跑在主线程 Handler 上，
 *    界面读取播放位置属于跨进程/跨线程通信）。
 */
class MusicPlayerService : Service() {

    /**
     * 绑定用的 Binder。
     *
     * Activity 通过 [MusicBinder.getService] 拿到服务实例，从而调用播放控制方法、
     * 读取 [currentTrack]、[currentPosition]、[duration] 等信息。
     */
    inner class MusicBinder : Binder() {
        /** 取得服务实例 */
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    /** Activity 设置的进度回调，服务每次轮询都调用它把最新进度推给界面 */
    var progressCallback: ((position: Int, duration: Int, isPlaying: Boolean) -> Unit)? = null

    private val binder = MusicBinder()

    /** MediaPlayer 只在服务内部使用 */
    private var mediaPlayer: MediaPlayer? = null

    /** 播放列表（含当前下标） */
    private val playlist = Playlist()

    private val uiHandler = Handler(Looper.getMainLooper())

    /** 定时读取播放进度的任务 */
    private val progressTask = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (isPrepared) {
                    progressCallback?.invoke(
                        player.currentPosition,
                        player.duration.coerceAtLeast(0),
                        player.isPlaying
                    )
                }
            }
            uiHandler.postDelayed(this, PROGRESS_INTERVAL)
        }
    }

    /** MediaPlayer 是否已经准备好（prepare 完成），避免在未就绪时读取时长 */
    private var isPrepared = false

    /** 当前歌曲 */
    val currentTrack: Track?
        get() = playlist.currentTrack

    /** 播放列表中的所有歌曲 */
    val tracks: List<Track>
        get() = playlist.items

    /** 当前播放位置（毫秒） */
    val currentPosition: Int
        get() = mediaPlayer?.takeIf { isPrepared }?.currentPosition ?: 0

    /** 当前歌曲总时长（毫秒） */
    val duration: Int
        get() = mediaPlayer?.takeIf { isPrepared }?.duration ?: (currentTrack?.duration?.toInt() ?: 0)

    /** 是否正在播放 */
    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // 先建好通知并进入前台，避免 startForegroundService 后 5 秒内没有调用 startForeground 导致崩溃
        startForeground(NOTIFICATION_ID, buildNotification())
        uiHandler.postDelayed(progressTask, PROGRESS_INTERVAL)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 通知栏上的按钮通过 Intent 触发本服务的动作
        when (intent?.action) {
            ACTION_TOGGLE -> togglePlayPause()
            ACTION_PREVIOUS -> playPrevious()
            ACTION_NEXT -> playNext()
            ACTION_STOP -> stopPlayback()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        uiHandler.removeCallbacksAndMessages(null)
        releasePlayer()
        // minSdk = 24，因此不需要按版本区分 stopForeground 的新旧重载
        @Suppress("DEPRECATION")
        stopForeground(true)
        super.onDestroy()
    }

    // ==================== 对外提供的播放控制 ====================

    /** 用查询到的歌曲列表替换播放列表（重新扫描媒体库后调用） */
    fun setTracks(newTracks: List<Track>) {
        val keepPlaying = isPlaying
        playlist.replaceAll(newTracks)
        if (!keepPlaying) {
            releasePlayer()
        }
        updateNotification()
    }

    /**
     * 播放列表中下标为 index 的歌曲（点击列表项或上一首/下一首时调用）。
     *
     * @return 是否成功开始播放
     */
    fun play(index: Int): Boolean {
        if (!playlist.select(index)) {
            return false
        }
        return startCurrent()
    }

    /** 播放 / 暂停切换 */
    fun togglePlayPause() {
        val player = mediaPlayer
        if (player == null || !isPrepared) {
            // 还没有准备过播放器：直接从第一首开始
            if (!playlist.isEmpty) {
                play(if (playlist.currentIndex >= 0) playlist.currentIndex else 0)
            }
            return
        }
        if (player.isPlaying) {
            player.pause()
        } else {
            player.start()
        }
        updateNotification()
    }

    /** 下一首；已经是最后一首时停止播放 */
    fun playNext() {
        val next = playlist.nextIndex()
        if (next == null) {
            stopPlayback()
            return
        }
        play(next)
    }

    /** 上一首；已经是第一首时重新从头播放当前歌曲 */
    fun playPrevious() {
        val previous = playlist.previousIndex()
        if (previous == null) {
            seekTo(0)
            return
        }
        play(previous)
    }

    /** 停止播放并把播放位置复位 */
    fun stopPlayback() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            isPrepared = false
            player.reset()
        }
        updateNotification()
    }

    /** 跳转到指定播放位置（拖动 SeekBar 时调用） */
    fun seekTo(positionMs: Int) {
        val player = mediaPlayer ?: return
        if (!isPrepared) {
            return
        }
        val target = positionMs.coerceIn(0, player.duration.coerceAtLeast(0))
        player.seekTo(target)
        // 立刻回调一次，让 SeekBar 马上跟手
        progressCallback?.invoke(target, player.duration.coerceAtLeast(0), player.isPlaying)
    }

    // ==================== 内部实现 ====================

    /** 播放当前选中的歌曲 */
    private fun startCurrent(): Boolean {
        val track = playlist.currentTrack ?: return false
        return try {
            releasePlayer()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                // 媒体地址是普通字符串，这里解析成 Uri 后交给 MediaPlayer 播放
                @Suppress("UseKtx")
                setDataSource(this@MusicPlayerService, Uri.parse(track.uri))
                setOnPreparedListener { player ->
                    isPrepared = true
                    player.start()
                    updateNotification()
                }
                setOnCompletionListener {
                    // 一首放完自动播下一首，最后一首放完则停止
                    if (playlist.nextIndex() == null) {
                        stopPlayback()
                    } else {
                        playNext()
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer 播放出错 what=$what extra=$extra")
                    stopPlayback()
                    true
                }
                prepareAsync()
            }
            true
        } catch (e: Exception) {
            // 文件被删除、格式不支持等异常都不应该让应用崩溃
            Log.e(TAG, "播放失败：${track.title}", e)
            stopPlayback()
            false
        }
    }

    /** 释放 MediaPlayer */
    private fun releasePlayer() {
        mediaPlayer?.runCatching {
            reset()
            release()
        }
        mediaPlayer = null
        isPrepared = false
    }

    // ==================== 通知栏 ====================

    /** 创建通知渠道（Android 8.0 之后必须） */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * 构造通知：标题显示当前歌曲名，正文显示歌手，并带三个控制按钮。
     * 新播放一首歌时调用本方法更新通知（实验要求(4)）。
     */
    private fun buildNotification(): Notification {
        val track = currentTrack
        val title = track?.title ?: getString(R.string.notification_no_song)
        val text = when {
            track == null -> getString(R.string.notification_waiting)
            !isPlaying -> getString(R.string.notification_paused, track.artist.ifBlank { getString(R.string.unknown_artist) })
            else -> track.artist.ifBlank { getString(R.string.unknown_artist) }
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(
                R.drawable.ic_skip_previous,
                getString(R.string.action_previous),
                servicePendingIntent(ACTION_PREVIOUS, REQUEST_PREVIOUS)
            )
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                getString(if (isPlaying) R.string.action_pause else R.string.action_play),
                servicePendingIntent(ACTION_TOGGLE, REQUEST_TOGGLE)
            )
            .addAction(
                R.drawable.ic_skip_next,
                getString(R.string.action_next),
                servicePendingIntent(ACTION_NEXT, REQUEST_NEXT)
            )
            .build()
    }

    /** 通知按钮的 PendingIntent：重新启动本服务并带上动作 */
    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this,
        requestCode,
        Intent(this, MusicPlayerService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** 刷新通知栏（换歌、暂停/播放、停止时都会调用） */
    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            manager.notify(NOTIFICATION_ID, buildNotification())
        } catch (e: SecurityException) {
            // 用户没有授予通知权限时忽略即可，播放本身不受影响
            Log.w(TAG, "无法更新通知栏", e)
        }
    }

    companion object {
        private const val TAG = "MusicPlayerService"

        /** 通知渠道 id */
        const val CHANNEL_ID = "music_playback"

        /** 前台服务通知 id */
        const val NOTIFICATION_ID = 1001

        /** 进度回调间隔（毫秒），SeekBar 每 500 毫秒刷新一次 */
        const val PROGRESS_INTERVAL = 500L

        const val ACTION_TOGGLE = "cn.edu.sicnu.cs.heshuyu.music.action.TOGGLE"
        const val ACTION_PREVIOUS = "cn.edu.sicnu.cs.heshuyu.music.action.PREVIOUS"
        const val ACTION_NEXT = "cn.edu.sicnu.cs.heshuyu.music.action.NEXT"
        const val ACTION_STOP = "cn.edu.sicnu.cs.heshuyu.music.action.STOP"

        private const val REQUEST_TOGGLE = 1
        private const val REQUEST_PREVIOUS = 2
        private const val REQUEST_NEXT = 3
    }
}
