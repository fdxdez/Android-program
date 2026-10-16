package cn.edu.sicnu.cs.heshuyu.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    companion object {
        /** 请求码：启动 Activity2 并等它返回结果 */
        private const val REQUEST_CODE_ACTIVITY2 = 100
    }

    private lateinit var tvResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvResult = findViewById(R.id.tv_result)

        // (1) 正向传参：启动 Activity1
        findViewById<Button>(R.id.btn_activity1).setOnClickListener {
            val intent = Intent(this, Activity1::class.java)
            intent.putExtra(
                getString(R.string.extra_message),
                getString(R.string.activity1_message)
            )
            startActivity(intent)
        }

        // (2) 需要拿返回值：启动 Activity2
        findViewById<Button>(R.id.btn_activity2).setOnClickListener {
            startActivityForResult(
                Intent(this, Activity2::class.java),
                REQUEST_CODE_ACTIVITY2
            )
        }

        // (3) 启动 Activity3（相机在 Activity3 内部处理）
        findViewById<Button>(R.id.btn_activity3).setOnClickListener {
            startActivity(Intent(this, Activity3::class.java))
        }

        // (4) 启动 Activity4（秒表）
        findViewById<Button>(R.id.btn_activity4).setOnClickListener {
            startActivity(Intent(this, Activity4::class.java))
        }
    }

    /** Activity2 返回结果后回调这里 */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ACTIVITY2 && resultCode == RESULT_OK) {
            val text = data?.getStringExtra(getString(R.string.extra_result)).orEmpty()
            tvResult.text = getString(R.string.main_result_format, text)
        }
    }
}