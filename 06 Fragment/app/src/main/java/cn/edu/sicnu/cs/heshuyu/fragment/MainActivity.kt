package cn.edu.sicnu.cs.heshuyu.fragment

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.edu.sicnu.cs.heshuyu.fragment.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * 主界面（Controller）。
 *
 * 实验六要求「采用 Fragment 重构 APP」，这里不再使用多个 Activity，
 * 而是在一个 Activity 中通过 [androidx.fragment.app.FragmentManager] 切换 3 个 Fragment：
 *
 * 1. [ProfileFragment]：个人信息（姓名、学号、性别、电话）；
 * 2. [TwoCardGameFragment]：两张牌的翻牌游戏；
 * 3. [ThreeCardGameFragment]：三张牌的翻牌游戏。
 *
 * 三个界面的数据都由 [cn.edu.sicnu.cs.heshuyu.fragment.data.GameFileStore] 序列化保存，
 * 退出应用后重新打开可以恢复。
 *
 * 为避免每次切换都重建 Fragment（重建会重新读文件、丢失游戏动画状态），
 * 这里用 [androidx.fragment.app.FragmentTransaction.show] / [androidx.fragment.app.FragmentTransaction.hide]
 * 的方式切换已经创建好的实例。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var profileFragment: ProfileFragment
    private lateinit var twoCardGameFragment: TwoCardGameFragment
    private lateinit var threeCardGameFragment: ThreeCardGameFragment

    private var currentFragment: androidx.fragment.app.Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 标题栏显示「姓名 + 学号」（提交要求：APP 应用标题为自己的姓名+学号）
        findViewById<MaterialToolbar>(R.id.toolbar).title = getString(R.string.app_name)

        // 旋转屏幕重建时复用 FragmentManager 已经恢复的实例，避免重复创建
        profileFragment = findOrCreate(TAG_PROFILE) { ProfileFragment() }
        twoCardGameFragment = findOrCreate(TAG_TWO_CARD) { TwoCardGameFragment() }
        threeCardGameFragment = findOrCreate(TAG_THREE_CARD) { ThreeCardGameFragment() }

        val navigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        navigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> switchTo(profileFragment)
                R.id.nav_two_card -> switchTo(twoCardGameFragment)
                R.id.nav_three_card -> switchTo(threeCardGameFragment)
                else -> return@setOnItemSelectedListener false
            }
            true
        }

        if (savedInstanceState == null) {
            switchTo(profileFragment)
        } else {
            navigation.selectedItemId = R.id.nav_profile
        }
    }

    /** 优先使用 FragmentManager 中已经存在的实例（界面重建时用），否则新建 */
    private inline fun <T : androidx.fragment.app.Fragment> findOrCreate(
        tag: String,
        create: () -> T
    ): T {
        @Suppress("UNCHECKED_CAST")
        val existing = supportFragmentManager.findFragmentByTag(tag) as? T
        return existing ?: create()
    }

    /** 切换到目标 Fragment：不在 FragmentManager 中的先 add 再隐藏其它界面 */
    private fun switchTo(target: androidx.fragment.app.Fragment) {
        if (target === currentFragment) {
            return
        }
        supportFragmentManager.beginTransaction().apply {
            // 用淡入淡出的方式切换，让界面变化更自然
            setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            for (fragment in listOf(profileFragment, twoCardGameFragment, threeCardGameFragment)) {
                if (fragment === target) {
                    if (fragment.isAdded) {
                        show(fragment)
                    } else {
                        add(R.id.fragmentContainer, fragment, tagOf(fragment))
                    }
                } else if (fragment.isAdded) {
                    hide(fragment)
                }
            }
        }.commit()
        currentFragment = target
    }

    private fun tagOf(fragment: androidx.fragment.app.Fragment): String = when (fragment) {
        profileFragment -> TAG_PROFILE
        twoCardGameFragment -> TAG_TWO_CARD
        else -> TAG_THREE_CARD
    }
    private companion object {
        const val TAG_PROFILE = "fragment_profile"
        const val TAG_TWO_CARD = "fragment_two_card"
        const val TAG_THREE_CARD = "fragment_three_card"
    }
}
