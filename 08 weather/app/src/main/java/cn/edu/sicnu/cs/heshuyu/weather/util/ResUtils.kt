package cn.edu.sicnu.cs.heshuyu.weather.util

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

/** 资源读取工具：从 res/raw 中一次性读出整段文本（用于城市列表 JSON） */
object ResUtils {

    fun readFileFromRaw(context: Context, rawName: Int): String {
        return try {
            context.resources.openRawResource(rawName).use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                    val builder = StringBuilder()
                    var line = reader.readLine()
                    while (line != null) {
                        builder.append(line)
                        line = reader.readLine()
                    }
                    builder.toString()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
