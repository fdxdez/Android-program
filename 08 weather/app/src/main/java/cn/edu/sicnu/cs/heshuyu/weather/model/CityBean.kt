package cn.edu.sicnu.cs.heshuyu.weather.model

import com.google.gson.annotations.SerializedName

/**
 * 本地城市列表（res/raw/citycode.json）中的一条记录。
 * 字段名与 JSON 中的下划线命名通过 @SerializedName 一一对应，
 * 这是 Gson 解析本地 JSON 的关键。
 */
data class CityBean(
    val id: Int = 0,
    val pid: Int = 0,
    @SerializedName("city_code") val cityCode: String? = null,
    @SerializedName("city_name") val cityName: String? = null,
    @SerializedName("post_code") val postCode: String? = null,
    @SerializedName("area_code") val areaCode: String? = null,
    val ctime: String? = null
)

/** 列表中的一行：header 不为空表示省份分组标题，否则是一行城市。 */
data class CityItem(
    val header: String? = null,
    val city: CityBean? = null,
    val province: String = ""
)
