package cn.edu.sicnu.cs.heshuyu.mid_termassessment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.edu.sicnu.cs.heshuyu.mid_termassessment.adapter.StudentAdapter
import cn.edu.sicnu.cs.heshuyu.mid_termassessment.model.Student
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar

/**
 * 第 1 个 Activity：学生信息列表。
 *
 * 1. 标题显示"姓名 + 学号"；
 * 2. 用 RecyclerView 以列表形式展示已有学生；
 * 3. 点击"添加学生"按钮启动第 2 个 Activity，通过 ActivityResult 回调接收返回的学生数据，
 *    并刷新当前学生列表；
 * 4. 横竖屏切换时用 onSaveInstanceState 保存并恢复列表数据。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var rootView: View
    private lateinit var appBar: AppBarLayout
    private lateinit var listContainer: View
    private lateinit var studentsView: RecyclerView
    private lateinit var countView: TextView
    private lateinit var emptyView: View
    private lateinit var addButton: ExtendedFloatingActionButton

    private val adapter = StudentAdapter()
    private val students = mutableListOf<Student>()

    /** 启动"添加学生"界面并等待其返回结果。 */
    private val addStudentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val student = IntentCompat.getParcelableExtra(
            data,
            AddStudentActivity.EXTRA_STUDENT,
            Student::class.java
        ) ?: return@registerForActivityResult
        onStudentAdded(student)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        bindViews()
        configureSystemBars()
        applyWindowInsets()
        setupStudentList(savedInstanceState)

        addButton.setOnClickListener { openAddStudentScreen() }
    }

    private fun bindViews() {
        rootView = findViewById(R.id.root)
        appBar = findViewById(R.id.app_bar)
        listContainer = findViewById(R.id.list_container)
        studentsView = findViewById(R.id.rv_students)
        countView = findViewById(R.id.tv_count)
        emptyView = findViewById(R.id.empty_view)
        addButton = findViewById(R.id.fab_add)
    }

    /** 蓝色标题栏使用浅色状态栏图标，底部导航栏区域使用深色图标。 */
    private fun configureSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = true
        }
    }

    /** 处理沉浸式布局的系统栏间距，避免标题栏与列表被状态栏、导航栏遮挡。 */
    private fun applyWindowInsets() {
        val fabMargin = resources.getDimensionPixelSize(R.dimen.fab_margin)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            appBar.updatePadding(top = systemBars.top)
            listContainer.updatePadding(bottom = systemBars.bottom)
            addButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = fabMargin + systemBars.bottom
            }
            insets
        }
    }

    private fun setupStudentList(savedInstanceState: Bundle?) {
        studentsView.layoutManager = LinearLayoutManager(this)
        studentsView.adapter = adapter

        val savedStudents = savedInstanceState?.let {
            BundleCompat.getParcelableArrayList(it, STATE_STUDENTS, Student::class.java)
        }
        if (savedStudents != null) {
            students.addAll(savedStudents)
        } else if (savedInstanceState == null) {
            students.addAll(createSampleStudents())
        }
        refreshStudentList()
    }

    /** 提交新数据给适配器并刷新人数统计、空列表提示。 */
    private fun refreshStudentList() {
        adapter.submitList(students.toList())
        countView.text = getString(R.string.student_count, students.size)
        emptyView.isVisible = students.isEmpty()
    }

    /** 第 2 个 Activity 返回结果后的处理：校验学号唯一性并追加到列表。 */
    private fun onStudentAdded(student: Student) {
        if (students.any { it.studentId == student.studentId }) {
            Snackbar.make(
                rootView,
                getString(R.string.student_duplicated, student.studentId),
                Snackbar.LENGTH_LONG
            ).show()
            return
        }
        students.add(student)
        refreshStudentList()
        Snackbar.make(
            rootView,
            getString(R.string.student_added, student.name),
            Snackbar.LENGTH_SHORT
        ).show()
        studentsView.post { studentsView.scrollToPosition(students.lastIndex) }
    }

    /** 启动第 2 个 Activity，并把已有学号传过去用于重复校验。 */
    private fun openAddStudentScreen() {
        val intent = Intent(this, AddStudentActivity::class.java).apply {
            putStringArrayListExtra(
                AddStudentActivity.EXTRA_EXISTING_IDS,
                ArrayList(students.map { it.studentId })
            )
        }
        addStudentLauncher.launch(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(STATE_STUDENTS, ArrayList(students))
    }

    /** 初始演示数据，便于打开应用即可看到学生列表。 */
    private fun createSampleStudents(): List<Student> = listOf(
        Student("2024180411", "何姝玙", "女", "计算机2024级3班"),
        Student("2024180405", "李思远", "男", "计算机2024级3班"),
        Student("2024180418", "王一涵", "女", "计算机2024级2班"),
        Student("2024180422", "陈子航", "男", "计算机2024级1班")
    )

    private companion object {
        const val STATE_STUDENTS = "state_students"
    }
}
