package cn.edu.sicnu.cs.heshuyu.gameapp

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.edu.sicnu.cs.heshuyu.gameapp.data.PlayerRepository
import cn.edu.sicnu.cs.heshuyu.gameapp.model.Card
import cn.edu.sicnu.cs.heshuyu.gameapp.model.CardMatchingGame
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 翻牌游戏界面的 UI 测试：牌桌、翻牌、配对、翻回、重新开始与结算对话框。
 */
@RunWith(AndroidJUnit4::class)
class GameActivityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var scenario: ActivityScenario<GameActivity>

    @Before
    fun setUp() {
        PlayerRepository.init(context)
        PlayerRepository.resetForTest()
        scenario = ActivityScenario.launch(GameActivity::class.java)
        settle()
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun boardShowsSixteenFaceDownCards() {
        for (index in 0 until CARD_COUNT) {
            onView(withContentDescription(backDescription(index))).check(matches(isDisplayed()))
        }
        onView(withId(R.id.tvScore)).check(matches(withText("0")))
        onView(withId(R.id.tvPairs))
            .check(matches(withText(context.getString(R.string.pairs_value_format, 0, 8))))
    }

    @Test
    fun firstFlipCostsOnePointAndShowsTheCardFace() {
        clickFaceDownCard(0)

        onView(withId(R.id.tvScore)).check(matches(withText("-1")))
        onView(withId(R.id.tvFlips)).check(matches(withText("1")))
        onView(withContentDescription(frontDescription(0))).check(matches(isDisplayed()))
    }

    @Test
    fun clickingTheSameCardAgainFlipsItBack() {
        clickFaceDownCard(0)
        clickFaceUpCard(0)

        onView(withId(R.id.tvScore)).check(matches(withText("-1")))
        onView(withContentDescription(backDescription(0))).check(matches(isDisplayed()))
    }

    @Test
    fun matchingPairAddsScoreAndProgress() {
        val pairIndex = game().indexOfPair(0)
        clickFaceDownCard(0)
        clickFaceDownCard(pairIndex)

        onView(withId(R.id.tvScore)).check(matches(withText("14")))
        onView(withId(R.id.tvFlips)).check(matches(withText("2")))
        onView(withId(R.id.tvPairs))
            .check(matches(withText(context.getString(R.string.pairs_value_format, 1, 8))))
        onView(withContentDescription(matchedDescription(0))).check(matches(isDisplayed()))
    }

    @Test
    fun mismatchedPairFlipsBackAutomatically() {
        val pair = findMismatchPair()
        Assume.assumeTrue("本局随机发出的牌中没有互不匹配的一对牌", pair != null)
        val (first, second) = pair!!

        clickFaceDownCard(first)
        clickFaceDownCard(second)
        onView(withId(R.id.tvScore)).check(matches(withText("-4")))

        settle(FLIP_BACK_WAIT)
        onView(withContentDescription(backDescription(first))).check(matches(isDisplayed()))
        onView(withContentDescription(backDescription(second))).check(matches(isDisplayed()))
        onView(withId(R.id.tvFlips)).check(matches(withText("2")))
    }

    @Test
    fun restartButtonResetsTheGame() {
        clickFaceDownCard(0)
        onView(withId(R.id.tvScore)).check(matches(withText("-1")))

        onView(withId(R.id.btnRestart)).perform(click())
        settle()

        onView(withId(R.id.tvScore)).check(matches(withText("0")))
        onView(withId(R.id.tvFlips)).check(matches(withText("0")))
        onView(withId(R.id.tvPairs))
            .check(matches(withText(context.getString(R.string.pairs_value_format, 0, 8))))
        for (index in 0 until CARD_COUNT) {
            onView(withContentDescription(backDescription(index))).check(matches(isDisplayed()))
        }
    }

    @Test
    fun playingAllPairsShowsWinDialogAndRecordsHighScore() {
        val model = game()
        val played = mutableSetOf<Int>()
        var pairsPlayed = 0

        for (index in 0 until model.allCards.size) {
            if (index in played) {
                continue
            }
            val pairIndex = model.indexOfPair(index)
            clickFaceDownCard(index)
            clickFaceDownCard(pairIndex)
            played.add(index)
            played.add(pairIndex)
            pairsPlayed++
            if (pairsPlayed == 4) {
                captureScreenshot("board_matched")
            }
        }
        settle(600)

        onView(withText(context.getString(R.string.dialog_win_title)))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        captureScreenshot("win_dialog")
        assertEquals(112, PlayerRepository.highScore)
    }

    // ---------------------------------------------------------------- 工具方法

    private fun game(): CardMatchingGame {
        var model: CardMatchingGame? = null
        scenario.onActivity { activity ->
            val field = GameActivity::class.java.getDeclaredField("game")
            field.isAccessible = true
            model = field.get(activity) as CardMatchingGame
        }
        return requireNotNull(model) { "无法读取游戏模型" }
    }

    private fun findMismatchPair(): Pair<Int, Int>? {
        val model = game()
        val indices = model.allCards.indices
        for (first in indices) {
            for (second in indices) {
                if (first == second) {
                    continue
                }
                if (Card.scoreBetween(model.cardAtIndex(first), model.cardAtIndex(second)) == 0) {
                    return first to second
                }
            }
        }
        return null
    }

    private fun backDescription(index: Int): String =
        context.getString(R.string.card_desc_back, index + 1)

    private fun frontDescription(index: Int): String =
        context.getString(R.string.card_desc_front, index + 1, game().cardAtIndex(index).toString())

    private fun matchedDescription(index: Int): String =
        context.getString(R.string.card_desc_matched, index + 1, game().cardAtIndex(index).toString())

    private fun clickFaceDownCard(index: Int) {
        onView(withContentDescription(backDescription(index))).perform(click())
        settle()
    }

    private fun clickFaceUpCard(index: Int) {
        onView(withContentDescription(frontDescription(index))).perform(click())
        settle()
    }

    /** 等待界面动画结束，保证断言读到的是最终状态 */
    private fun settle(millis: Long = 400L) {
        Thread.sleep(millis)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    /** 通过 UiAutomation 截屏，保存到 /sdcard/Download/flipcard/ 以便导出到电脑 */
    private fun captureScreenshot(name: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        readToEnd(automation.executeShellCommand("mkdir -p $SCREENSHOT_DIR"))
        readToEnd(automation.executeShellCommand("screencap -p $SCREENSHOT_DIR/$name.png"))
    }

    private fun readToEnd(descriptor: ParcelFileDescriptor) {
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }

    private companion object {
        const val CARD_COUNT = 16
        const val FLIP_BACK_WAIT = 1400L
        const val SCREENSHOT_DIR = "/sdcard/Download/flipcard"
    }
}
