package cn.edu.sicnu.cs.heshuyu.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/** 二级 Activity 3：隐式 Intent 调系统相机，拍完在本页显示照片 */
class Activity3 : AppCompatActivity() {

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1

        /** MediaStore 返回缩略图时固定的 Bundle key，框架约定值，不是界面文案 */
        private const val EXTRA_THUMBNAIL = "data"
    }

    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_3)

        imageView = findViewById(R.id.image_view)
        findViewById<Button>(R.id.btn_take_photo).setOnClickListener {
            dispatchTakePictureIntent()
        }
    }

    /** 隐式 Intent：只声明"我要拍照"，由系统挑一个能拍照的 App */
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(this, R.string.camera_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    /** 相机 App 拍完照片后回调这里 */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get(EXTRA_THUMBNAIL) as? Bitmap
            if (imageBitmap != null) {
                imageView.setImageBitmap(imageBitmap)
            } else {
                Toast.makeText(this, R.string.photo_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}