package cn.edu.sicnu.cs.heshuyu.weather.util

import cn.edu.sicnu.cs.heshuyu.weather.R

/** 根据天气类型文字选择附件里的天气图标 */
object WeatherIcon {

    fun of(type: String?): Int {
        val text = type.orEmpty()
        return when {
            text.contains("雷") -> R.drawable.thunder
            text.contains("雪") -> R.drawable.snow
            text.contains("雨") -> R.drawable.rain
            text.contains("阴") -> R.drawable.mcloud
            text.contains("多云") -> R.drawable.cloud
            text.contains("晴") -> R.drawable.sun
            else -> R.drawable.cloud
        }
    }
}
