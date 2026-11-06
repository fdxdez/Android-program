package cn.edu.sicnu.cs.heshuyu.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cn.edu.sicnu.cs.heshuyu.fragment.data.CardState
import cn.edu.sicnu.cs.heshuyu.fragment.data.GameFileStore
import cn.edu.sicnu.cs.heshuyu.fragment.data.ThreeCardGame
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 三张牌翻牌游戏 Fragment（Controller 层），实验六要求的第 3 个 Fragment。
 *
 * 规则：牌桌 24 张（6 列 × 4 行，共 8 组），每次翻开三张，三张点数全部相同即配对成功；
 * 配对失败时三张牌延时翻回背面。全部配对完成即获胜。
 *
 * 历史信息（牌桌、分数、翻牌次数、配对进度、最高分）通过 [GameFileStore]
 * 序列化保存到 `three_card_game.dat`，退出应用后重新打开可以恢复。
 */
class ThreeCardGameFragment : BaseCardGameFragment() {

    /** 牌桌列数：24 张排成 6 列 × 4 行 */
    override val columns: Int = COLUMNS

    override val cardCount: Int = ThreeCardGame.DEFAULT_CARD_COUNT

    private lateinit var game: ThreeCardGame

    private val uiHandler = Handler(Looper.getMainLooper())

    private var isBusy = false
    private var isGameFinished = false
    private var flipBackTask: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 从文件恢复上一次的游戏状态；没有记录（第一次运行）时新建一副牌
        val saved = GameFileStore.load<ThreeCardGame>(
            requireContext(),
            GameFileStore.FILE_THREE_CARD_GAME
        )
        game = saved ?: ThreeCardGame(cardCount)
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
        tvSets.text = getString(R.string.sets_value_format, game.matchedSets, game.setCount)
    }

    override fun onBoardClicked(position: Int) {
        if (isBusy || isGameFinished) {
            return
        }

        when (game.chooseCardAtIndex(position).action) {
            ThreeCardGame.Action.IGNORED -> return

            ThreeCardGame.Action.FLIPPED_UP -> {
                renderBoard()
                tvStatus.text = getString(R.string.status_pick_third)
            }

            ThreeCardGame.Action.FLIPPED_DOWN -> {
                renderBoard()
                tvStatus.text = getString(R.string.status_flipped_back)
            }

            ThreeCardGame.Action.MATCHED -> {
                renderBoard()
                tvStatus.text = getString(R.string.status_matched_set)
            }

            ThreeCardGame.Action.MISMATCHED -> {
                renderBoard()
                tvStatus.text = getString(R.string.status_mismatch_set)
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
        GameFileStore.save(requireContext(), GameFileStore.FILE_THREE_CARD_GAME, game)
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
        /** 牌桌列数：24 张排成 6 列 × 4 行 */
        const val COLUMNS = 6

        /** 配对失败后延时翻回的时间（毫秒） */
        const val FLIP_BACK_DELAY = 900L
    }
}
