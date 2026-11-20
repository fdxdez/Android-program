package cn.edu.sicnu.cs.heshuyu.weather.model

import com.google.gson.annotations.SerializedName

/** 天气接口 http://t.weather.itboy.net/api/weather/city/{city_code} 的响应体 */
data class WeatherResponse(
    val message: String? = null,
    val status: Int = 0,
    val date: String? = null,
    val time: String? = null,
    val cityInfo: CityInfo? = null,
    val data: WeatherData? = null
)

data class CityInfo(
    val city: String? = null,
    val citykey: String? = null,
    val parent: String? = null,
    val updateTime: String? = null
)

data class WeatherData(
    val shidu: String? = null,
    val pm25: Double? = null,
    val pm10: Double? = null,
    val quality: String? = null,
    val wendu: String? = null,
    val ganmao: String? = null,
    val yesterday: Forecast? = null,
    val forecast: List<Forecast>? = null
)

/** 每天一条预报：高低温、天气类型、风向风力、日出日落等 */
data class Forecast(
    val date: String? = null,
    val high: String? = null,
    val low: String? = null,
    val ymd: String? = null,
    val week: String? = null,
    val sunrise: String? = null,
    val sunset: String? = null,
    val aqi: Int? = null,
    val fx: String? = null,
    val fl: String? = null,
    val type: String? = null,
    val notice: String? = null
)
