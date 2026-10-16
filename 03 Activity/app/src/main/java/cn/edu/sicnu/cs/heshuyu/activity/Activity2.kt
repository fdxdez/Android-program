package cn.edu.sicnu.cs.heshuyu.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

/** 二级 Activity 2：输入内容，点确定后把内容回传 MainActivity */
class Activity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_2)

        val etInput = findViewById<EditText>(R.id.et_input)

        findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            // 把输入内容放进 Intent，作为"返回结果"
            val resultIntent = Intent().apply {
                putExtra(getString(R.string.extra_result), etInput.text.toString())
            }
            setResult(RESULT_OK, resultIntent)
            finish()   // 结束自己，回到 MainActivity
        }
    }
}