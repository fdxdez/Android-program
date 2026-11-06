package cn.edu.sicnu.cs.heshuyu.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cn.edu.sicnu.cs.heshuyu.fragment.data.CardState
import cn.edu.sicnu.cs.heshuyu.fragment.data.GameFileStore
import cn.edu.sicnu.cs.heshuyu.fragment.data.TwoCardGame
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 两张牌翻牌游戏 Fragment（Controller 层），实验六要求的第 2 个 Fragment。
 *
 * 规则：牌桌 16 张（4 列 × 4 行，共 8 对），每次翻开两张，点数相同即配对成功；
 * 配对失败时两张牌延时翻回背面。全部配对完成即获胜。
 *
 * 历史信息（牌桌、分数、翻牌次数、配对进度、最高分）通过 [GameFileStore]
 * 序列化保存到 `two_card_game.dat`：
 * - `onCreate`：先读文件，读不到才发新牌；
 * - `onPause`：把当前游戏对象写回文件。
 */
class TwoCardGameFragment : BaseCardGameFragment() {

    /** 牌桌上的牌数：16 张 = 8 对 */
    override val columns: Int = COLUMNS

    override val cardCount: Int = TwoCardGame.DEFAULT_CARD_COUNT

    private lateinit var game: TwoCardGame

    private val uiHandler = Handler(Looper.getMainLooper())

    /** 配对失败、等待翻回的这段时间内不接受新的点击 */
    private var isBusy = false

    /** 一局结束时只弹一次结算对话框 */
    private var isGameFinished = false

    /** 等待执行的翻回任务，Fragment 销毁时取消 */
    private var flipBackTask: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 从文件恢复上一次的游戏状态；没有记录（第一次运行）时新建一副牌
        val saved = GameFileStore.load<TwoCardGame>(requireContext(), GameFileStore.FILE_TWO_CARD_GAME)
        game = saved ?: TwoCardGame(cardCount)
        // 恢复出来的状态不应该停留在「等待翻回」，直接清掉避免开局无法点击
        game.hidePendingMismatch()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_card_game, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<MaterialButton>(R.id.btnRestart).setOnClickListener { startNewGame() }
    }

    override fun cardsOfGame(): List<CardState> = game.allCards

    override fun isFaceUp(index: Int): Boolean = game.isFaceUp(index)

    override fun updateStats() {
        tvScore.text = getString(R.string.number_format, game.score)
        tvFlips.text = getString(R.string.number_format, game.flips)
        tvSets.text = getString(R.string.pairs_value_format, game.matchedPairs, game.pairCount)
    }

    override fun onBoardClicked(position: Int) {
        if (isBusy || isGameFinished) {
            return
        }

        when (game.chooseCardAtIndex(position).action) {
            TwoCardGame.Action.IGNORED -> return

            TwoCardGame.Action.FLIPPED_UP -> {
                renderBoard()
                tvStatus.text = getString(R.string.status_pick_second)
            }

            TwoCardGame.Action.FLIPPED_DOWN -> {
                renderBoard()
                tvStatus.text = getString(R.string.status_flipped_back)
            }

            TwoCardGame.Action.MATCHED -> {
                renderBoard()
                tvStatus.text = getString(R.string.status_matched_pair)
            }

            TwoCardGame.Action.MISMATCHED -> {
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

    /** 重新开始一局：模型 reset() 重新发牌，界面全部翻回背面 */
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

    /** 全部配对完成：刷新最高分并弹出结算对话框 */
    private fun onGameFinished() {
        if (isGameFinished) {
            return
        }
        isGameFinished = true
        tvStatus.text = getString(R.string.status_win)
        saveGame()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_win_title)
            .setMessage(
                getString(
                    R.string.dialog_win_message,
                    game.score,
                    game.bestScore,
                    game.flips
                )
            )
            .setCancelable(false)
            .setPositiveButton(R.string.dialog_win_positive) { dialog, _ ->
                dialog.dismiss()
                startNewGame()
            }
            .setNegativeButton(R.string.dialog_win_negative) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /** 把当前游戏对象序列化写回文件（游戏结束、界面不可见时各存一次） */
    private fun saveGame() {
        GameFileStore.save(requireContext(), GameFileStore.FILE_TWO_CARD_GAME, game)
    }

    override fun onPause() {
        super.onPause()
        saveGame()
    }

    override fun onDestroyView() {
        uiHandler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }

    private companion object {
        /** 牌桌列数：16 张排成 4 列 × 4 行 */
        const val COLUMNS = 4

        /** 配对失败后延时翻回的时间（毫秒） */
        const val FLIP_BACK_DELAY = 900L
    }
}
