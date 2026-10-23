package cn.edu.sicnu.cs.heshuyu.gameapp

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.edu.sicnu.cs.heshuyu.gameapp.data.PlayerRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 主界面的 UI 测试。
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        PlayerRepository.init(context)
        PlayerRepository.resetForTest()
    }

    @Test
    fun showsCurrentPlayerInformation() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.tvName)).check(matches(withText(PlayerRepository.player.name)))
            onView(withId(R.id.tvStudentId)).check(
                matches(withText(context.getString(R.string.main_subtitle_format, PlayerRepository.STUDENT_ID)))
            )
            onView(withId(R.id.tvGender)).check(
                matches(withText(context.getString(R.string.gender_male)))
            )
            onView(withId(R.id.tvPhone)).check(matches(withText(PlayerRepository.player.phone)))
            onView(withId(R.id.tvHighScore)).check(matches(isDisplayed()))
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun startGameButtonOpensGameScreen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.btnStartGame)).perform(click())
            onView(withId(R.id.gridCards)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun editInfoButtonOpensEditScreen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.btnEditInfo)).perform(click())
            onView(withId(R.id.etName)).check(matches(isDisplayed()))
            onView(withId(R.id.etPhone)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun highScoreIsShownAfterRecorded() {
        PlayerRepository.recordScore(42)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.tvHighScore)).check(
                matches(withText(context.getString(R.string.score_value_format, 42)))
            )
        }
        assertEquals(42, PlayerRepository.highScore)
    }
}
