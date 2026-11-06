package cn.edu.sicnu.cs.heshuyu.music

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.edu.sicnu.cs.heshuyu.music.data.MediaStoreHelper
import cn.edu.sicnu.cs.heshuyu.music.service.MusicPlayerService
import cn.edu.sicnu.cs.heshuyu.music.ui.TrackAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.util.concurrent.Executors

/**
 * 主界面（Controller）。
 *
 * 职责：
 * 1. 申请音频读取权限（Android 13 及以上为 [Manifest.permission.READ_MEDIA_AUDIO]），
 *    授予后用 [MediaStoreHelper]（ContentResolver）查询音乐列表并显示在 RecyclerView 中；
 * 2. 通过 [bindService] 绑定 [MusicPlayerService]，拿到服务里的 Binder 后调用
 *    播放 / 暂停 / 停止 / 上一首 / 下一首 / 跳转进度等方法（所有播放逻辑都在服务里）；
 * 3. 服务的 [MusicPlayerService.progressCallback] 每 500 毫秒回调一次，
 *    这里把播放进度显示到 SeekBar 上；拖动 SeekBar 时反过来调用服务的 seekTo。
 *
 * 扫描媒体库是 IO 操作，放在单线程池里执行，避免阻塞主线程（多线程编程）。
 */
class MainActivity : AppCompatActivity(), TrackAdapter.OnTrackClickListener {

    private lateinit var tvSongTitle: TextView
    private lateinit var tvSongArtist: TextView
    private lateinit var tvPosition: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvHint: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var btnPlayPause: MaterialButton
    private lateinit var trackAdapter: TrackAdapter

    /** 扫描媒体库用的单线程池，查询完成后切回主线程刷新界面 */
    private val scanExecutor = Executors.newSingleThreadExecutor()

    /** 绑定服务后得到的服务实例，未绑定时为 null */
    private var playerService: MusicPlayerService? = null

    /** 是否已经绑定（避免重复 bindService） */
    private var isBound = false

    /** 用户是否正在拖动 SeekBar，拖动期间不接受服务的进度回调，避免「拖不动」 */
    private var isUserSeeking = false

    /** 服务连接：绑定成功 / 意外断开时都会回调 */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MusicPlayerService.MusicBinder ?: return
            playerService = binder.getService()
            isBound = true
            // 注册进度回调：服务每 500 毫秒把播放位置推给界面（多线程，实时刷新 SeekBar）
            playerService?.progressCallback = { position, duration, playing ->
                onProgress(position, duration, playing)
            }
            // 绑定成功后把已经扫描到的列表同步给服务
            playerService?.setTracks(trackAdapter.currentTracks())
            renderNowPlaying()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            playerService = null
        }
    }

    /** 权限申请结果回调 */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startAndBindService()
            scanTracks()
        } else {
            tvHint.text = getString(R.string.hint_permission_denied)
            updateEmptyState(empty = true)
        }
    }

    /** 通知栏权限申请结果回调：不授权只影响通知显示，不影响播放 */
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 结果不影响播放，无需处理 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 标题栏显示「姓名 + 学号」
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.app_name)
        toolbar.inflateMenu(R.menu.menu_main)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_rescan) {
                scanTracks()
                true
            } else {
                false
            }
        }

        tvSongTitle = findViewById(R.id.tvSongTitle)
        tvSongArtist = findViewById(R.id.tvSongArtist)
        tvPosition = findViewById(R.id.tvPosition)
        tvDuration = findViewById(R.id.tvDuration)
        tvHint = findViewById(R.id.tvHint)
        tvEmpty = findViewById(R.id.tvEmpty)
        seekBar = findViewById(R.id.seekBar)
        btnPlayPause = findViewById(R.id.btnPlayPause)

        trackAdapter = TrackAdapter(this)
        val recyclerTracks = findViewById<RecyclerView>(R.id.recyclerTracks)
        recyclerTracks.layoutManager = LinearLayoutManager(this)
        recyclerTracks.adapter = trackAdapter

        findViewById<MaterialButton>(R.id.btnPlayPause).setOnClickListener {
            playerService?.togglePlayPause() ?: toastNoService()
        }
        findViewById<MaterialButton>(R.id.btnPrevious).setOnClickListener {
            playerService?.playPrevious() ?: toastNoService()
        }
        findViewById<MaterialButton>(R.id.btnNext).setOnClickListener {
            playerService?.playNext() ?: toastNoService()
        }
        findViewById<MaterialButton>(R.id.btnStop).setOnClickListener {
            playerService?.stopPlayback() ?: toastNoService()
        }

        // 拖动 SeekBar 调整播放进度
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    tvPosition.text = TrackAdapter.formatDuration(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(bar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(bar: SeekBar?) {
                isUserSeeking = false
                playerService?.seekTo(bar?.progress ?: 0)
            }
        })

        // 通知栏权限（Android 13+ 不授权时通知不显示，但播放不受影响）
        requestNotificationPermissionIfNeeded()
        ensureAudioPermissionAndLoad()
    }

    override fun onDestroy() {
        scanExecutor.shutdown()
        // 先取消回调，避免服务继续往已经销毁的界面推送进度
        playerService?.progressCallback = null
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        playerService = null
        super.onDestroy()
    }

    // ==================== 权限 ====================

    /** 读取音频所需的权限名：Android 13 起为 READ_MEDIA_AUDIO，之前为 READ_EXTERNAL_STORAGE */
    private val audioPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    /** 已有权限则直接加载，否则弹出权限申请对话框 */
    private fun ensureAudioPermissionAndLoad() {
        if (hasAudioPermission()) {
            startAndBindService()
            scanTracks()
        } else {
            tvHint.text = getString(R.string.permission_rationale)
            permissionLauncher.launch(audioPermission)
        }
    }

    private fun hasAudioPermission(): Boolean = ContextCompat.checkSelfPermission(
        this,
        audioPermission
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // ==================== 服务绑定 ====================

    /**
     * 启动并绑定播放服务。
     *
     * 先用 [ContextCompat.startForegroundService] 让服务成为前台服务（否则退到后台可能被回收），
     * 再 [bindService] 拿到 Binder 以便调用服务里的方法。
     */
    private fun startAndBindService() {
        val intent = Intent(this, MusicPlayerService::class.java)
        // API 26 起有 startForegroundService；minSdk 为 24，低版本用 ContextCompat 的兼容实现
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            ContextCompat.startForegroundService(this, intent)
        }
        if (!isBound) {
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    // ==================== 媒体库扫描 ====================

    /** 在后台线程用 ContentResolver 查询音乐列表，查询完成后回到主线程刷新界面 */
    private fun scanTracks() {
        tvHint.text = getString(R.string.hint_scanning)
        scanExecutor.execute {
            val tracks = MediaStoreHelper.queryAudioTracks(applicationContext)
            runOnUiThread {
                trackAdapter.submitTracks(tracks)
                tvHint.text = getString(R.string.tracks_count_format, tracks.size)
                updateEmptyState(empty = tracks.isEmpty())
                // 把最新的列表同步给播放服务
                playerService?.setTracks(tracks)
                renderNowPlaying()
            }
        }
    }

    private fun updateEmptyState(empty: Boolean) {
        tvEmpty.visibility = if (empty) android.view.View.VISIBLE else android.view.View.GONE
        if (empty) {
            tvEmpty.text = getString(R.string.hint_empty_tracks)
        }
    }

    // ==================== 界面刷新 ====================

    /** 点击列表中的歌曲：交给服务播放 */
    override fun onTrackClick(position: Int) {
        val service = playerService
        if (service == null) {
            toastNoService()
            return
        }
        if (!service.play(position)) {
            Toast.makeText(this, R.string.hint_empty_tracks, Toast.LENGTH_SHORT).show()
            return
        }
        trackAdapter.setPlayingIndex(position)
        renderNowPlaying()
    }

    /** 服务的进度回调：刷新 SeekBar、时间与播放/暂停按钮 */
    private fun onProgress(position: Int, duration: Int, playing: Boolean) {
        if (!isUserSeeking) {
            if (duration > 0) {
                seekBar.max = duration
                seekBar.progress = position.coerceIn(0, duration)
            } else {
                seekBar.progress = 0
            }
            tvPosition.text = TrackAdapter.formatDuration(position.toLong())
        }
        tvDuration.text = TrackAdapter.formatDuration(duration.toLong())
        updatePlayPauseIcon(playing)
    }

    /** 根据播放状态刷新「当前播放」卡片与按钮图标 */
    private fun renderNowPlaying() {
        val service = playerService
        val track = service?.currentTrack
        if (track == null) {
            tvSongTitle.text = getString(R.string.now_playing_none)
            tvSongArtist.text = getString(R.string.now_playing_hint)
            trackAdapter.setPlayingIndex(-1)
        } else {
            tvSongTitle.text = track.title
            tvSongArtist.text = getString(
                R.string.track_subtitle_format,
                track.artist.ifBlank { getString(R.string.unknown_artist) },
                TrackAdapter.formatDuration(track.duration)
            )
            trackAdapter.setPlayingIndex(service.tracks.indexOfFirst { it.id == track.id })
        }
        updatePlayPauseIcon(service?.isPlaying == true)
    }

    private fun updatePlayPauseIcon(playing: Boolean) {
        btnPlayPause.setIconResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        btnPlayPause.contentDescription =
            getString(if (playing) R.string.action_pause else R.string.action_play)
    }

    private fun toastNoService() {
        Toast.makeText(this, R.string.now_playing_hint, Toast.LENGTH_SHORT).show()
    }
}
