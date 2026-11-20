package cn.edu.sicnu.cs.heshuyu.mid_termassessment.model

import android.os.Parcel
import android.os.Parcelable

/**
 * 学生实体类：包含学号、姓名、性别、班级四个属性。
 *
 * 实现 [Parcelable] 接口，使其实例可以通过 Intent 在 Activity 之间传递。
 */
data class Student(
    /** 学号 */
    val studentId: String,
    /** 姓名 */
    val name: String,
    /** 性别（男 / 女） */
    val gender: String,
    /** 班级 */
    val className: String
) : Parcelable {

    /** 列表头像中显示的文字，取姓名的第一个字。 */
    val avatarText: String
        get() = name.take(1)

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(studentId)
        dest.writeString(name)
        dest.writeString(gender)
        dest.writeString(className)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Student> = object : Parcelable.Creator<Student> {
            override fun createFromParcel(source: Parcel): Student = Student(
                studentId = source.readString().orEmpty(),
                name = source.readString().orEmpty(),
                gender = source.readString().orEmpty(),
                className = source.readString().orEmpty()
            )

            override fun newArray(size: Int): Array<Student?> = arrayOfNulls(size)
        }
    }
}
