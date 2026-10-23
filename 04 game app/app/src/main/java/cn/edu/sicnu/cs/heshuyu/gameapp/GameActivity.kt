package cn.edu.sicnu.cs.heshuyu.gameapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import cn.edu.sicnu.cs.heshuyu.gameapp.data.PlayerRepository
import cn.edu.sicnu.cs.heshuyu.gameapp.model.Card
import cn.edu.sicnu.cs.heshuyu.gameapp.model.CardMatchingGame
import cn.edu.sicnu.cs.heshuyu.gameapp.ui.WindowInsetsHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 翻牌游戏的控制器（Controller）。
 *
 * 职责：
 * 1. 接收用户的翻牌点击 [onCardTapped]；
 * 2. 调用模型 [CardMatchingGame] 处理业务逻辑；
 * 3. 根据模型状态刷新界面 [renderCard] / [updateStats]，并支持重新开始一局。
 */
class GameActivity : AppCompatActivity() {

    private lateinit var game: CardMatchingGame
    private lateinit var gridCards: GridLayout
    private lateinit var tvScore: TextView
    private lateinit var tvFlips: TextView
    private lateinit var tvPairs: TextView
    private lateinit var tvStatus: TextView

    private val cardViews = mutableListOf<MaterialCardView>()
    private val cardFaces = mutableListOf<TextView>()

    /** 记录界面上每张牌当前显示的状态，避免重复播放翻转动画 */
    private val displayedFaceUp = BooleanArray(CARD_COUNT)
    private val displayedMatched = BooleanArray(CARD_COUNT)

    private val uiHandler = Handler(Looper.getMainLooper())
    private var isBusy = false
    private var isGameFinished = false
    private var flipBackTask: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game)
        WindowInsetsHelper.apply(this, findViewById(R.id.root), findViewById(R.id.toolbar))

        PlayerRepository.init(this)

        gridCards = findViewById(R.id.gridCards)
        tvScore = findViewById(R.id.tvScore)
        tvFlips = findViewById(R.id.tvFlips)
        tvPairs = findViewById(R.id.tvPairs)
        tvStatus = findViewById(R.id.tvStatus)
        gridCards.contentDescription = getString(R.string.board_desc_format, CARD_COUNT)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnRestart).setOnClickListener { startNewGame() }
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        game = CardMatchingGame(CARD_COUNT)
        buildBoard()
        startNewGame()
    }

    override fun onDestroy() {
        uiHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    /** 根据牌桌大小动态创建牌（View 层由多张牌组成） */
    private fun buildBoard() {
        val gap = dp(CARD_GAP_DP)
        gridCards.removeAllViews()
        cardViews.clear()
        cardFaces.clear()

        for (index in 0 until CARD_COUNT) {
            val cardView = layoutInflater.inflate(R.layout.item_card, gridCards, false) as MaterialCardView
            val params = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
            )
            params.width = 0
            params.height = 0
            params.setMargins(gap, gap, gap, gap)
            cardView.layoutParams = params
            cardView.tag = index
            cardView.setOnClickListener { onCardTapped(index) }

            gridCards.addView(cardView)
            cardViews.add(cardView)
            cardFaces.add(cardView.findViewById(R.id.tvCardFace))
        }
    }

    /** 重新开始一局：模型重新发牌，界面全部翻回背面 */
    private fun startNewGame() {
        flipBackTask?.let { uiHandler.removeCallbacks(it) }
        flipBackTask = null
        isBusy = false
        isGameFinished = false
        game.restart()

        for (index in 0 until CARD_COUNT) {
            displayedFaceUp[index] = false
            displayedMatched[index] = false
            renderCard(index, animate = false)
        }
        tvStatus.text = getString(R.string.status_ready)
        updateStats()
    }

    /** 用户点击第 index 张牌 */
    private fun onCardTapped(index: Int) {
        if (isBusy || isGameFinished) {
            return
        }
        val result = game.chooseCardAtIndex(index)
        when (result.action) {
            CardMatchingGame.Action.IGNORED -> return
            CardMatchingGame.Action.FLIPPED_UP -> {
                renderCard(index)
                tvStatus.text = getString(R.string.status_pick_second)
            }

            CardMatchingGame.Action.FLIPPED_DOWN -> {
                renderCard(index)
                tvStatus.text = getString(R.string.status_flipped_back)
            }

            CardMatchingGame.Action.MATCHED -> {
                renderCard(result.firstIndex)
                renderCard(result.secondIndex)
                tvStatus.text = getString(R.string.status_matched)
            }

            CardMatchingGame.Action.MISMATCHED -> {
                renderCard(result.firstIndex)
                renderCard(result.secondIndex)
                tvStatus.text = getString(R.string.status_mismatch)
                isBusy = true
                val task = Runnable {
                    game.hidePendingMismatch()
                    renderCard(result.firstIndex)
                    renderCard(result.secondIndex)
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

    /** 把模型状态同步到某一张牌上，animate 为 true 时播放翻转动画 */
    private fun renderCard(index: Int, animate: Boolean = true) {
        val card = game.cardAtIndex(index)
        val faceUp = game.isFaceUp(index)
        val matched = card.isMatched
        if (animate && displayedFaceUp[index] == faceUp && displayedMatched[index] == matched) {
            return
        }
        displayedFaceUp[index] = faceUp
        displayedMatched[index] = matched

        val cardView = cardViews[index]
        cardView.animate().cancel()
        if (!animate) {
            cardView.rotationY = 0f
            bindCardAppearance(index, card, faceUp)
            return
        }
        cardView.animate()
            .rotationY(90f)
            .setDuration(FLIP_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                bindCardAppearance(index, card, faceUp)
                cardView.rotationY = -90f
                cardView.animate()
                    .rotationY(0f)
                    .setDuration(FLIP_DURATION)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            .start()
    }

    /** 设置牌面（背面 / 正面 / 已配对）的颜色、文字与无障碍描述 */
    private fun bindCardAppearance(index: Int, card: Card, faceUp: Boolean) {
        val cardView = cardViews[index]
        val face = cardFaces[index]
        val number = index + 1
        when {
            !faceUp -> {
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_back_face))
                cardView.strokeColor = ContextCompat.getColor(this, R.color.card_back_edge)
                face.text = getString(R.string.card_back_symbol)
                face.setTextColor(ContextCompat.getColor(this, R.color.card_back_text))
                cardView.contentDescription = getString(R.string.card_desc_back, number)
                cardView.isClickable = true
            }

            card.isMatched -> {
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_matched_face))
                cardView.strokeColor = ContextCompat.getColor(this, R.color.card_matched_edge)
                face.text = card.toString()
                face.setTextColor(ContextCompat.getColor(this, R.color.card_matched_text))
                cardView.contentDescription = getString(R.string.card_desc_matched, number, card.toString())
                cardView.isClickable = false
            }

            else -> {
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_front_face))
                cardView.strokeColor = ContextCompat.getColor(this, R.color.card_front_edge)
                face.text = card.toString()
                face.setTextColor(
                    ContextCompat.getColor(this, if (card.isRed) R.color.suit_red else R.color.suit_black)
                )
                cardView.contentDescription = getString(R.string.card_desc_front, number, card.toString())
                cardView.isClickable = true
            }
        }
    }

    /** 刷新分数、翻牌次数与配对进度 */
    private fun updateStats() {
        tvScore.text = game.score.toString()
        tvFlips.text = game.flips.toString()
        tvPairs.text = getString(R.string.pairs_value_format, game.matchedPairs, game.pairCount)
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val CARD_COUNT = 16
        const val FLIP_BACK_DELAY = 900L
        const val FLIP_DURATION = 130L
        const val CARD_GAP_DP = 5
    }
}
