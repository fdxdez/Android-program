package cn.edu.sicnu.cs.heshuyu.mid_termassessment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cn.edu.sicnu.cs.heshuyu.mid_termassessment.R
import cn.edu.sicnu.cs.heshuyu.mid_termassessment.model.Student

/**
 * 学生列表适配器。
 *
 * 使用 [ListAdapter] + [DiffUtil]，提交新列表后自动计算差异并刷新列表项。
 */
class StudentAdapter : ListAdapter<Student, StudentAdapter.StudentViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /** 学生列表项视图持有者。 */
    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val avatarText: TextView = itemView.findViewById(R.id.tv_avatar)
        private val nameText: TextView = itemView.findViewById(R.id.tv_name)
        private val infoText: TextView = itemView.findViewById(R.id.tv_info)
        private val genderText: TextView = itemView.findViewById(R.id.tv_gender)

        fun bind(student: Student) {
            avatarText.text = student.avatarText
            nameText.text = student.name
            infoText.text = itemView.context.getString(
                R.string.item_student_info,
                student.studentId,
                student.className
            )
            genderText.text = student.gender
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Student>() {

        override fun areItemsTheSame(oldItem: Student, newItem: Student): Boolean =
            oldItem.studentId == newItem.studentId

        override fun areContentsTheSame(oldItem: Student, newItem: Student): Boolean =
            oldItem == newItem
    }
}
