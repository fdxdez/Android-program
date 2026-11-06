package cn.edu.sicnu.cs.heshuyu.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import cn.edu.sicnu.cs.heshuyu.fragment.data.GameFileStore
import cn.edu.sicnu.cs.heshuyu.fragment.data.Player
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText

/**
 * 个人信息 Fragment（Controller 层），实验六要求的第 1 个 Fragment。
 *
 * 负责姓名、学号、性别、电话的显示与修改：
 * - `onCreateView` 时用 [GameFileStore.load] 从 `player.dat` 读回上次保存的信息；
 * - 点击「保存」后用 [GameFileStore.save] 把 [Player] 对象序列化写回文件；
 * - 退出应用再打开时，界面显示的就是上次保存的内容。
 *
 * 状态栏顶部显示「姓名 + 学号」，对应提交要求「APP 应用标题为自己的姓名+学号」。
 */
class ProfileFragment : Fragment() {

    private lateinit var etName: TextInputEditText
    private lateinit var etStudentId: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var genderGroup: MaterialButtonToggleGroup
    private lateinit var tvAvatar: TextView

    /** 内存中的玩家信息，界面显示与保存都以它为准 */
    private var player: Player = Player()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etName = view.findViewById(R.id.etName)
        etStudentId = view.findViewById(R.id.etStudentId)
        etPhone = view.findViewById(R.id.etPhone)
        genderGroup = view.findViewById(R.id.genderGroup)
        tvAvatar = view.findViewById(R.id.tvAvatar)

        view.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { savePlayer() }
        view.findViewById<MaterialButton>(R.id.btnReload).setOnClickListener { loadPlayer(showToast = true) }

        loadPlayer(showToast = false)
    }

    /** 从文件读回个人信息并显示，对应实验六要求的「序列化存储 + 恢复」 */
    private fun loadPlayer(showToast: Boolean) {
        val context = requireContext()
        val saved = GameFileStore.load<Player>(context, GameFileStore.FILE_PLAYER)
        player = saved ?: Player()
        etName.setText(player.name)
        etStudentId.setText(player.studentId)
        etPhone.setText(player.phone)
        genderGroup.check(
            if (player.gender == Player.GENDER_FEMALE) R.id.btnGenderFemale else R.id.btnGenderMale
        )
        tvAvatar.text = player.name.firstOrNull()?.uppercaseChar()?.toString()
            ?: getString(R.string.profile_avatar_placeholder)
        if (showToast) {
            Toast.makeText(
                context,
                if (saved == null) getString(R.string.profile_no_record) else getString(R.string.profile_loaded),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /** 校验输入并保存个人信息（对象序列化写入 player.dat） */
    private fun savePlayer() {
        val name = etName.text?.toString()?.trim().orEmpty()
        val studentId = etStudentId.text?.toString()?.trim().orEmpty()
        val phone = etPhone.text?.toString()?.trim().orEmpty()

        if (name.isEmpty()) {
            etName.error = getString(R.string.error_name_empty)
            return
        }
        if (phone.isEmpty()) {
            etPhone.error = getString(R.string.error_phone_empty)
            return
        }
        if (phone.length != PHONE_LENGTH || phone.any { !it.isDigit() }) {
            etPhone.error = getString(R.string.error_phone_invalid)
            return
        }
        etName.error = null
        etPhone.error = null

        player = Player(
            name = name,
            studentId = studentId.ifEmpty { Player.DEFAULT_STUDENT_ID },
            gender = if (genderGroup.checkedButtonId == R.id.btnGenderFemale) {
                Player.GENDER_FEMALE
            } else {
                Player.GENDER_MALE
            },
            phone = phone,
            saved = true
        )

        val ok = GameFileStore.save(requireContext(), GameFileStore.FILE_PLAYER, player)
        Toast.makeText(
            requireContext(),
            if (ok) getString(R.string.profile_saved) else getString(R.string.profile_save_failed),
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Fragment 被隐藏或销毁前把用户当前输入的内容同步到内存对象，
     * 这样即使没有点「保存」，再次进入本 Fragment 也能看到刚才输入的内容。
     */
    override fun onPause() {
        super.onPause()
        player = player.copy(
            name = etName.text?.toString()?.trim().orEmpty(),
            studentId = etStudentId.text?.toString()?.trim().orEmpty(),
            phone = etPhone.text?.toString()?.trim().orEmpty()
        )
    }

    private companion object {
        /** 手机号长度 */
        const val PHONE_LENGTH = 11
    }
}
