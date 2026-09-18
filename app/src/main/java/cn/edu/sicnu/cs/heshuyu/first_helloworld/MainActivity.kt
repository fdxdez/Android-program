package cn.edu.sicnu.cs.heshuyu.first_helloworld

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // 三种语言的数据：(显示名, 问候语, 国旗资源)
    private val languages = listOf(
        Triple("中文", "你好，世界！", R.drawable.flag_china),
        Triple("English", "Hello World!", R.drawable.flag_usa),
        Triple("Français", "Bonjour le monde !", R.drawable.flag_france)
    )

    private lateinit var helloText: TextView
    private lateinit var flagView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ===== 纯代码构建界面 =====
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 120, 60, 60)
        }

        flagView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        helloText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 28f
            gravity = android.view.Gravity.CENTER
        }

        root.addView(flagView)
        root.addView(helloText)

        // 三个语言按钮，一行一个
        languages.forEach { (label, _, _) ->
            val btn = Button(this).apply {
                text = label
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            btn.setOnClickListener { showLanguage(btn.text.toString()) }
            root.addView(btn)
        }

        setContentView(root)

        // 默认显示中文
        showLanguage("中文")
    }

    private fun showLanguage(label: String) {
        val item = languages.find { it.first == label } ?: return
        helloText.text = item.second
        flagView.setImageResource(item.third)
        Toast.makeText(this, "当前语言：$label", Toast.LENGTH_SHORT).show()
    }
}