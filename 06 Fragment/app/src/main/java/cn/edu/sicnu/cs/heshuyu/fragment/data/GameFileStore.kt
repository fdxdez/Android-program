package cn.edu.sicnu.cs.heshuyu.fragment.data

import android.content.Context
import android.util.Log
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * 对象序列化的文件存储工具（实验六的核心知识点）。
 *
 * 写文件：`context.openFileOutput(文件名, MODE_PRIVATE)` → [ObjectOutputStream] → `writeObject`
 * 读文件：`context.openFileInput(文件名)` → [ObjectInputStream] → `readObject`
 *
 * 文件保存在应用私有的 `filesDir` 目录下，退出程序后不会丢失；
 * 因此个人信息与两个翻牌游戏的「历史信息」都能在下次打开应用时恢复。
 */
object GameFileStore {

    private const val TAG = "GameFileStore"

    /** 个人信息文件名 */
    const val FILE_PLAYER = "player.dat"

    /** 两张牌游戏文件名 */
    const val FILE_TWO_CARD_GAME = "two_card_game.dat"

    /** 三张牌游戏文件名 */
    const val FILE_THREE_CARD_GAME = "three_card_game.dat"

    /**
     * 把可序列化对象写入应用私有文件。
     *
     * @return 写入成功返回 true，失败返回 false（同时打印日志，便于排查）
     */
    fun <T : Serializable> save(context: Context, fileName: String, data: T): Boolean = try {
        ObjectOutputStream(context.openFileOutput(fileName, Context.MODE_PRIVATE)).use { output ->
            output.writeObject(data)
            output.flush()
        }
        true
    } catch (e: IOException) {
        Log.e(TAG, "保存 $fileName 失败", e)
        false
    }

    /**
     * 从应用私有文件读回可序列化对象。
     *
     * 文件不存在（第一次运行）、内容损坏或类型不匹配时统一返回 null，
     * 调用方据此走「首次进入」的默认逻辑。
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Serializable> load(context: Context, fileName: String): T? = try {
        ObjectInputStream(context.openFileInput(fileName)).use { input ->
            input.readObject() as? T
        }
    } catch (e: FileNotFoundException) {
        // 第一次运行，还没有保存过，属于正常情况
        null
    } catch (e: IOException) {
        Log.e(TAG, "读取 $fileName 失败", e)
        null
    } catch (e: ClassNotFoundException) {
        Log.e(TAG, "读取 $fileName 时找不到类", e)
        null
    }
}
