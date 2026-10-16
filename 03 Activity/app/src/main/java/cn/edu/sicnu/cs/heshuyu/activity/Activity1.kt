package cn.edu.sicnu.cs.heshuyu.activity

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** 二级 Activity 1：显示 MainActivity 正向传过来的参数 */
class Activity1 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_1)

        // 取出 MainActivity 传来的参数
        val message = intent.getStringExtra(getString(R.string.extra_message)).orEmpty()

        findViewById<TextView>(R.id.tv_message).text =
            getString(R.string.activity1_received_format, message)
    }
}