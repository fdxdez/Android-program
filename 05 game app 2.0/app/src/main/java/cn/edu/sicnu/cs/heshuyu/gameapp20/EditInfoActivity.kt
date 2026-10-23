package cn.edu.sicnu.cs.heshuyu.gameapp20

import android.os.Bundle
import android.view.MenuItem
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cn.edu.sicnu.cs.heshuyu.gameapp20.data.Gender
import cn.edu.sicnu.cs.heshuyu.gameapp20.data.PlayerRepository
import cn.edu.sicnu.cs.heshuyu.gameapp20.ui.EdgeToEdgeHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * 信息修改界面：修改姓名、性别和电话。
 *
 * 修改结果只保存在内存（[PlayerRepository]）中，不写入文件；保存后回到主界面即可看到新信息。
 */
class EditInfoActivity : AppCompatActivity() {

    private lateinit var tilName: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var rgGender: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EdgeToEdgeHelper.enable(this)
        setContentView(R.layout.activity_edit_info)
        EdgeToEdgeHelper.fitContent(this, findViewById(R.id.root))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tilName = findViewById(R.id.tilName)
        tilPhone = findViewById(R.id.tilPhone)
        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        rgGender = findViewById(R.id.rgGender)

        findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { savePlayerInfo() }

        bindCurrentPlayer()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    /** 把当前玩家信息填入表单 */
    private fun bindCurrentPlayer() {
        val player = PlayerRepository.player
        etName.setText(player.name)
        etPhone.setText(player.phone)
        rgGender.check(if (player.gender == Gender.MALE) R.id.rbMale else R.id.rbFemale)
    }

    /** 校验并保存修改后的信息 */
    private fun savePlayerInfo() {
        val name = etName.text?.toString()?.trim().orEmpty()
        val phone = etPhone.text?.toString()?.trim().orEmpty()

        if (name.isEmpty()) {
            tilName.error = getString(R.string.error_name_empty)
            etName.requestFocus()
            return
        }
        tilName.error = null

        if (phone.isEmpty()) {
            tilPhone.error = getString(R.string.error_phone_empty)
            etPhone.requestFocus()
            return
        }
        if (phone.length != PHONE_LENGTH || phone.any { !it.isDigit() }) {
            tilPhone.error = getString(R.string.error_phone_invalid)
            etPhone.requestFocus()
            return
        }
        tilPhone.error = null

        val gender = if (rgGender.checkedRadioButtonId == R.id.rbFemale) Gender.FEMALE else Gender.MALE
        PlayerRepository.updatePlayer(name = name, gender = gender, phone = phone)
        Toast.makeText(this, R.string.toast_info_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private companion object {
        const val PHONE_LENGTH = 11
    }
}
