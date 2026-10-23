package cn.edu.sicnu.cs.heshuyu.gameapp20

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.edu.sicnu.cs.heshuyu.gameapp20.data.PlayerRepository
import cn.edu.sicnu.cs.heshuyu.gameapp20.model.CardMatchingGame
import cn.edu.sicnu.cs.heshuyu.gameapp20.ui.AboutDialog
import cn.edu.sicnu.cs.heshuyu.gameapp20.ui.CardAdapter
import cn.edu.sicnu.cs.heshuyu.gameapp20.ui.EdgeToEdgeHelper
import cn.edu.sicnu.cs.heshuyu.gameapp20.ui.GridSpacingItemDecoration
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 翻牌游戏的控制器（Controller）。
 *
 * 职责（MVC 中的 Controller）：
 * 1. 接收用户在 RecyclerView 上的翻牌点击回调 [onCardClick]；
 * 2. 调用模型 [CardMatchingGame] 处理游戏逻辑；
 * 3. 把模型状态转换成 [CardAdapter.CardItem] 交给适配器刷新界面，并更新分数、配对进度；
 * 4. 支持横屏 / 竖屏不同牌数：列数与牌数来自 integers 资源，横竖屏各一套。
 */
class GameActivity : AppCompatActivity(), CardAdapter.OnCardClickListener {

    private lateinit var game: CardMatchingGame
    private lateinit var recyclerCards: RecyclerView
    private lateinit var tvScore: TextView
    private lateinit var tvFlips: TextView
    private lateinit var tvSets: TextView
    private lateinit var tvStatus: TextView

    /** 牌桌适配器（View 层），点击事件回调给本 Activity */
    private val cardAdapter = CardAdapter(this)

    /** 列数：竖屏 4 列，横屏 9 列（见 values 与 values-land 下的 integers.xml） */
    private val columns: Int by lazy { resources.getInteger(R.integer.game_columns) }

    /** 牌数：竖屏 24 张，横屏 27 张（见 values 与 values-land 下的 integers.xml） */
    private val cardCount: Int by lazy { resources.getInteger(R.integer.game_card_count) }

    private val uiHandler = Handler(Looper.getMainLooper())
    private var isBusy = false
    private var isGameFinished = false
    private var flipBackTask: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeHelper.enable(this)
        setContentView(R.layout.activity_game)
        EdgeToEdgeHelper.fitContent(this, findViewById(R.id.root))

        // 顶部 ActionBar 显示返回箭头
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        PlayerRepository.init(this)

        tvScore = findViewById(R.id.tvScore)
        tvFlips = findViewById(R.id.tvFlips)
        tvSets = findViewById(R.id.tvSets)
        tvStatus = findViewById(R.id.tvStatus)

        recyclerCards = findViewById(R.id.recyclerCards)
        recyclerCards.layoutManager = GridLayoutManager(this, columns)
        recyclerCards.adapter = cardAdapter
        // 翻牌动画由适配器自己完成，关闭默认 item 动画，避免两种动画叠加
        recyclerCards.itemAnimator = null
        recyclerCards.addItemDecoration(
            GridSpacingItemDecoration(
                columns,
                resources.getDimensionPixelSize(R.dimen.card_spacing)
            )
        )
        recyclerCards.contentDescription = getString(R.string.board_desc_format, cardCount)
        // 牌桌尺寸变化时（首次显示、旋转屏幕、让出系统栏之后）重新计算牌的大小，让牌桌刚好铺满可用区域
        recyclerCards.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val sizeChanged = (right - left) != (oldRight - oldLeft) ||
                (bottom - top) != (oldBottom - oldTop)
            if (sizeChanged) {
                applyCardHeight()
            }
        }

        game = CardMatchingGame(cardCount)
        startNewGame()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_game, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }

        R.id.action_restart -> {
            startNewGame()
            true
        }

        R.id.action_about -> {
            AboutDialog.show(this)
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        uiHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    /** 用户在 RecyclerView 中点击了第 position 张牌（由 [CardAdapter] 回调） */
    override fun onCardClick(position: Int) {
        if (isBusy || isGameFinished) {
            return
        }

        val result = game.chooseCardAtIndex(position)
        when (result.action) {
            CardMatchingGame.Action.IGNORED -> return

            CardMatchingGame.Action.FLIPPED_UP -> {
                renderBoard()
                tvStatus.text = getString(R.string.status_pick_second)
            }

            CardMatchingGame.Action.FLIPPED_DOWN -> {
                renderBoard()
                tvStatus.text = getString(R.string.status_flipped_back)
            }

            CardMatchingGame.Action.MATCHED -> {
                renderBoard()
                tvStatus.text = getString(R.string.status_matched)
            }

            CardMatchingGame.Action.MISMATCHED -> {
                renderBoard()
                tvStatus.text = getString(R.string.status_mismatch)
                isBusy = true
                val task = Runnable {
                    game.hidePendingMismatch()
                    renderBoard()
                    isBusy = false
                    tvStatus.text = getString(R.string.status_ready)
                }
                flipBackTask = task
                uiHandler.postDelayed(task, FLIP_BACK_DELAY)
            }
        }

        updateStats()
        if (game.isGameOver) {
            onGameFinished()
        }
    }

    /** 重新开始一局：模型 reset()，界面全部翻回背面 */
    private fun startNewGame() {
        flipBackTask?.let { uiHandler.removeCallbacks(it) }
        flipBackTask = null
        isBusy = false
        isGameFinished = false
        game.reset()
        tvStatus.text = getString(R.string.status_ready)
        renderBoard()
        updateStats()
    }

    /** 把模型状态转换成适配器需要的显示数据 */
    private fun renderBoard() {
        cardAdapter.submitCards(
            game.allCards.mapIndexed { index, card ->
                CardAdapter.CardItem(
                    label = card.toString(),
                    faceUp = game.isFaceUp(index),
                    matched = card.isMatched,
                    isRed = card.isRed
                )
            }
        )
    }

    /**
     * 按「行数 + 间距」计算每张牌的高度，并同步给适配器与已经创建的牌，
     * 这样竖屏 6 行、横屏 3 行都能刚好铺满屏幕。
     */
    private fun applyCardHeight() {
        val rows = (cardCount + columns - 1) / columns
        if (rows <= 0) {
            return
        }
        val spacing = resources.getDimensionPixelSize(R.dimen.card_spacing)
        // 间距装饰器给每行的牌都加了底部间距，第一行还有一次顶部间距
        val gaps = spacing * (rows + 1)
        val available = recyclerCards.height -
            recyclerCards.paddingTop -
            recyclerCards.paddingBottom -
            gaps
        val height = (available / rows)
            .coerceAtLeast(resources.getDimensionPixelSize(R.dimen.card_min_height))
        if (height == cardAdapter.cardHeight) {
            return
        }
        cardAdapter.cardHeight = height
        for (index in 0 until recyclerCards.childCount) {
            val child = recyclerCards.getChildAt(index)
            val params = child.layoutParams
            if (params.height != height) {
                params.height = height
                child.layoutParams = params
            }
        }
        recyclerCards.requestLayout()
    }

    /** 刷新分数、翻牌次数与配对进度 */
    private fun updateStats() {
        tvScore.text = getString(R.string.number_format, game.score)
        tvFlips.text = getString(R.string.number_format, game.flips)
        tvSets.text = getString(R.string.sets_value_format, game.matchedSets, game.setCount)
    }

    /** 全部配对完成：保存最高分并弹出结算对话框 */
    private fun onGameFinished() {
        if (isGameFinished) {
            return
        }
        isGameFinished = true
        tvStatus.text = getString(R.string.status_win)

        val isNewRecord = PlayerRepository.recordScore(game.score)
        val message = buildString {
            append(getString(R.string.dialog_win_message, game.score, PlayerRepository.highScore))
            if (isNewRecord) {
                append("\n")
                append(getString(R.string.dialog_new_record))
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_win_title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.dialog_win_positive) { dialog, _ ->
                dialog.dismiss()
                startNewGame()
            }
            .setNegativeButton(R.string.dialog_win_negative) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }

    private companion object {
        /** 配对失败后延时翻回的时间（毫秒） */
        const val FLIP_BACK_DELAY = 900L
    }
}
