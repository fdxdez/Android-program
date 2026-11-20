package cn.edu.sicnu.cs.heshuyu.mid_termassessment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import cn.edu.sicnu.cs.heshuyu.mid_termassessment.model.Student
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout

/**
 * 第 2 个 Activity：新增学生交互界面。
 *
 * 1. 输入学号、姓名、班级，选择性别；
 * 2. "确定"按钮校验输入后把 [Student] 放入 Intent 回传给第 1 个 Activity；
 * 3. "取消"按钮（以及标题栏返回箭头）直接关闭界面，不返回任何数据。
 */
class AddStudentActivity : AppCompatActivity() {

    private lateinit var rootView: View
    private lateinit var appBar: AppBarLayout
    private lateinit var formScroll: NestedScrollView
    private lateinit var studentIdInput: TextInputLayout
    private lateinit var nameInput: TextInputLayout
    private lateinit var classInput: TextInputLayout
    private lateinit var genderGroup: RadioGroup
    private lateinit var confirmButton: MaterialButton

    /** 列表中已有的学号，用于重复校验。 */
    private lateinit var existingIds: Set<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_student)

        bindViews()
        configureSystemBars()
        applyWindowInsets()
        clearErrorsOnEdit()

        existingIds = intent.getStringArrayListExtra(EXTRA_EXISTING_IDS)?.toSet() ?: emptySet()

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            submitCancel()
        }
        confirmButton.setOnClickListener { submitForm() }
        findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
            submitCancel()
        }
    }

    private fun bindViews() {
        rootView = findViewById(R.id.root)
        appBar = findViewById(R.id.app_bar)
        formScroll = findViewById(R.id.form_scroll)
        studentIdInput = findViewById(R.id.til_student_id)
        nameInput = findViewById(R.id.til_name)
        classInput = findViewById(R.id.til_class)
        genderGroup = findViewById(R.id.rg_gender)
        confirmButton = findViewById(R.id.btn_confirm)
    }

    private fun configureSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = true
        }
    }

    /** 处理系统栏与软键盘间距，保证输入框不会被遮挡。 */
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            appBar.updatePadding(top = systemBars.top)
            formScroll.updatePadding(bottom = maxOf(systemBars.bottom, ime.bottom))
            insets
        }
    }

    /** "确定"：校验表单，通过则把学生数据回传并关闭界面。 */
    private fun submitForm() {
        val studentId = studentIdInput.text().orEmpty()
        val name = nameInput.text().orEmpty()
        val className = classInput.text().orEmpty()

        clearErrors()

        when {
            studentId.isEmpty() -> {
                studentIdInput.showError(R.string.error_student_id_empty)
            }

            !STUDENT_ID_PATTERN.matches(studentId) -> {
                studentIdInput.showError(R.string.error_student_id_format)
            }

            existingIds.contains(studentId) -> {
                studentIdInput.showError(R.string.error_student_id_duplicated)
            }

            name.isEmpty() -> {
                nameInput.showError(R.string.error_name_empty)
            }

            className.isEmpty() -> {
                classInput.showError(R.string.error_class_empty)
            }

            else -> {
                val student = Student(
                    studentId = studentId,
                    name = name,
                    gender = selectedGender(),
                    className = className
                )
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_STUDENT, student))
                finish()
            }
        }
    }

    /** "取消"：不携带任何数据关闭界面，第 1 个 Activity 因此不会刷新列表。 */
    private fun submitCancel() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun selectedGender(): String =
        if (genderGroup.checkedRadioButtonId == R.id.rb_female) {
            getString(R.string.gender_female)
        } else {
            getString(R.string.gender_male)
        }

    private fun clearErrors() {
        studentIdInput.error = null
        nameInput.error = null
        classInput.error = null
    }

    /** 用户重新输入时立即清除上一次的错误提示，避免旧提示一直停留在输入框上。 */
    private fun clearErrorsOnEdit() {
        listOf(studentIdInput, nameInput, classInput).forEach { layout ->
            layout.editText?.doAfterTextChanged { layout.error = null }
        }
    }

    private fun TextInputLayout.text(): String =
        editText?.text?.toString()?.trim().orEmpty()

    private fun TextInputLayout.showError(messageRes: Int) {
        error = getString(messageRes)
        requestFocus()
    }

    companion object {
        /** 回传学生对象的 Intent key。 */
        const val EXTRA_STUDENT = "extra_student"

        /** 传入已有学号列表的 Intent key。 */
        const val EXTRA_EXISTING_IDS = "extra_existing_ids"

        /** 学号格式：6~20 位数字。 */
        private val STUDENT_ID_PATTERN = Regex("[0-9]{6,20}")
    }
}
