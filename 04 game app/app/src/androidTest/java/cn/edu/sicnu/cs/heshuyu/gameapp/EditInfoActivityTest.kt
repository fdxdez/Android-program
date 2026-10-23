package cn.edu.sicnu.cs.heshuyu.gameapp

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.edu.sicnu.cs.heshuyu.gameapp.data.Gender
import cn.edu.sicnu.cs.heshuyu.gameapp.data.PlayerRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 信息修改界面的 UI 测试：表单回填、保存校验与保存结果。
 */
@RunWith(AndroidJUnit4::class)
class EditInfoActivityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        PlayerRepository.init(context)
        PlayerRepository.resetForTest()
    }

    @Test
    fun formIsFilledWithCurrentPlayer() {
        ActivityScenario.launch(EditInfoActivity::class.java).use {
            onView(withId(R.id.etName)).check(matches(withText(PlayerRepository.player.name)))
            onView(withId(R.id.etPhone)).check(matches(withText(PlayerRepository.player.phone)))
            onView(withId(R.id.rbMale)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun savingUpdatesPlayerRepository() {
        ActivityScenario.launch(EditInfoActivity::class.java).use {
            onView(withId(R.id.etName)).perform(replaceText("李小明"))
            onView(withId(R.id.etPhone)).perform(replaceText("13900001111"))
            onView(withId(R.id.rbFemale)).perform(click())
            onView(withId(R.id.btnSave)).perform(click())
        }

        assertEquals("李小明", PlayerRepository.player.name)
        assertEquals("13900001111", PlayerRepository.player.phone)
        assertEquals(Gender.FEMALE, PlayerRepository.player.gender)
    }

    @Test
    fun invalidPhoneShowsErrorAndDoesNotSave() {
        val originalName = PlayerRepository.player.name

        ActivityScenario.launch(EditInfoActivity::class.java).use {
            onView(withId(R.id.etPhone)).perform(replaceText("123"))
            onView(withId(R.id.btnSave)).perform(click())
            onView(withText(context.getString(R.string.error_phone_invalid)))
                .check(matches(isDisplayed()))
        }

        assertEquals(originalName, PlayerRepository.player.name)
    }

    @Test
    fun emptyNameShowsError() {
        ActivityScenario.launch(EditInfoActivity::class.java).use {
            onView(withId(R.id.etName)).perform(replaceText(""))
            onView(withId(R.id.btnSave)).perform(click())
            onView(withText(context.getString(R.string.error_name_empty)))
                .check(matches(isDisplayed()))
        }
    }
}
