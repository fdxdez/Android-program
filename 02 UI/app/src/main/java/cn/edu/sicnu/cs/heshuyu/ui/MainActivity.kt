package cn.edu.sicnu.cs.heshuyu.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.edu.sicnu.cs.heshuyu.ui.model.ProgramAdviser

/**
 * MVC 中的 Controller（控制器）：接收用户事件，调用模型层，刷新界面。
 * 本类中不出现任何直接书写的字符串，全部从 strings.xml 读取。
 */
class MainActivity : AppCompatActivity() {

    /** 区1：动态添加的 TextView 全部放进这个 LinearLayout（它在 ScrollView 内部） */
    private lateinit var textContainer: LinearLayout
    private lateinit var scrollView: ScrollView

    /** 区2：ProgramAdviser 模型对象（MVC 中的 Model） */
    private lateinit var adviser: ProgramAdviser

    /** 区1已添加的 TextView 个数，用于生成序号 */
    private var textViewCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ---------------- 区1：点击按钮，用代码动态生成 TextView ----------------
        textContainer = findViewById(R.id.textContainer)
        scrollView = findViewById(R.id.scrollView)
        findViewById<Button>(R.id.btnAddTextView).setOnClickListener {
            addTextView()
        }

        // ---------------- 区2：MVC —— 取输入 → 问模型 → 刷新界面 ----------------
        adviser = ProgramAdviser(this)

        val programSpinner = findViewById<Spinner>(R.id.spinnerProgram)
        programSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            adviser.programs
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            // 事件响应：查询模型层并返回结果
            val advice: String = adviser.getAdvice(programSpinner.selectedItemPosition)
            findViewById<TextView>(R.id.tvAdvice).text = advice
        }
    }

    /** 用代码创建一个 TextView 并加入 ScrollView 内的 LinearLayout */
    private fun addTextView() {
        textViewCount++
        val item = TextView(this).apply {
            text = getString(R.string.new_text_view_item, textViewCount)
            textSize = 20f
        }
        textContainer.addView(item)
        // 新增后自动滚到底部，保证最新一条可见
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }
}