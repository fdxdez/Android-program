package cn.edu.sicnu.cs.heshuyu.weather.net

import android.content.Context
import cn.edu.sicnu.cs.heshuyu.weather.model.WeatherResponse
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson

/**
 * Volley + Gson 的天气接口封装：
 * 1. Volley 负责发起 HTTP 请求并回调到主线程；
 * 2. Gson 负责把返回的 JSON 直接映射成 WeatherResponse 对象。
 */
object WeatherClient {

    private const val BASE_URL = "http://t.weather.itboy.net/api/weather/city/"

    private var queue: RequestQueue? = null

    private fun queue(context: Context): RequestQueue =
        queue ?: Volley.newRequestQueue(context.applicationContext).also { queue = it }

    fun weatherUrl(cityCode: String): String = BASE_URL + cityCode

    fun request(
        context: Context,
        cityCode: String,
        onSuccess: (WeatherResponse) -> Unit,
        onFailure: (String) -> Unit
    ): StringRequest {
        val request = StringRequest(
            Request.Method.GET, weatherUrl(cityCode),
            { result ->
                try {
                    val response = Gson().fromJson(result, WeatherResponse::class.java)
                    if (response?.data?.forecast != null) {
                        onSuccess(response)
                    } else {
                        onFailure("接口返回的数据为空")
                    }
                } catch (e: Exception) {
                    onFailure("JSON 解析失败：${e.message}")
                }
            },
            { error -> onFailure("网络请求失败：${error.message ?: "请检查网络"}") }
        )
        request.tag = cityCode
        request.retryPolicy = DefaultRetryPolicy(8000, 1, 1.0f)
        queue(context).add(request)
        return request
    }

    fun cancel(cityCode: String) {
        queue?.cancelAll(cityCode)
    }
}
