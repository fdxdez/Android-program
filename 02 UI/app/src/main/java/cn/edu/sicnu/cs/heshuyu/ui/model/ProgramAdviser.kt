package cn.edu.sicnu.cs.heshuyu.ui.model

import android.content.Context
import cn.edu.sicnu.cs.heshuyu.ui.R

/**
 * MVC 中的 Model（模型层）：ProgramAdviser（程序学习建议器）。
 * 只负责“数据 + 查询逻辑”，不引用任何控件，也不直接写字符串，
 * 数据全部来自 strings.xml 中的字符串数组资源。
 */
class ProgramAdviser(private val context: Context) {

    /** 下拉框选项：可选方向列表 */
    val programs: Array<String>
        get() = context.resources.getStringArray(R.array.program_entries)

    /**
     * 查询模型层：根据用户选中的下标返回对应的语言建议。
     * @param position Spinner 当前选中项下标
     */
    fun getAdvice(position: Int): String {
        val advice = context.resources.getStringArray(R.array.program_advice)
        return advice.getOrNull(position) ?: context.getString(R.string.advice_unknown)
    }
}