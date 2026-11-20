package cn.edu.sicnu.cs.heshuyu.weather

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import cn.edu.sicnu.cs.heshuyu.weather.model.Forecast
import cn.edu.sicnu.cs.heshuyu.weather.model.WeatherResponse
import cn.edu.sicnu.cs.heshuyu.weather.net.WeatherClient
import cn.edu.sicnu.cs.heshuyu.weather.util.WeatherIcon

/**
 * 第二界面：显示某个城市的天气详情。
 * 数据通过 Volley 访问天气接口，拿到 JSON 后用 Gson 解析成 WeatherResponse。
 */
class WeatherActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CITY_NAME = "city_name"
        const val EXTRA_CITY_CODE = "city_code"
        const val EXTRA_PROVINCE = "province"
    }

    private lateinit var tvCityName: TextView
    private lateinit var tvCityProvince: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvWeatherType: TextView
    private lateinit var tvUpdateTime: TextView
    private lateinit var ivWeatherIcon: ImageView
    private lateinit var tvStatHumidity: TextView
    private lateinit var tvStatQuality: TextView
    private lateinit var tvStatPm25: TextView
    private lateinit var tvStatWind: TextView
    private lateinit var tvStatSunrise: TextView
    private lateinit var tvStatSunset: TextView
    private lateinit var tvGanmao: TextView
    private lateinit var tvForecastTitle: TextView
    private lateinit var llForecast: LinearLayout
    private lateinit var llyStatus: LinearLayout
    private lateinit var pbStatus: View
    private lateinit var tvError: TextView
    private lateinit var btnRetry: TextView

    private var cityCode = ""
    private var cityName = ""
    private var province = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_weather)

        cityCode = intent.getStringExtra(EXTRA_CITY_CODE).orEmpty()
        cityName = intent.getStringExtra(EXTRA_CITY_NAME).orEmpty()
        province = intent.getStringExtra(EXTRA_PROVINCE).orEmpty()

        bindViews()
        tvCityName.text = cityName
        tvCityProvince.text = province
        if (cityCode.isNotBlank()) {
            loadWeather()
        } else {
            showError("城市编码为空，无法请求天气接口")
        }
    }

    private fun bindViews() {
        tvCityName = findViewById(R.id.tvCityName)
        tvCityProvince = findViewById(R.id.tvCityProvince)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvWeatherType = findViewById(R.id.tvWeatherType)
        tvUpdateTime = findViewById(R.id.tvUpdateTime)
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon)
        tvStatHumidity = findViewById(R.id.tvStatHumidity)
        tvStatQuality = findViewById(R.id.tvStatQuality)
        tvStatPm25 = findViewById(R.id.tvStatPm25)
        tvStatWind = findViewById(R.id.tvStatWind)
        tvStatSunrise = findViewById(R.id.tvStatSunrise)
        tvStatSunset = findViewById(R.id.tvStatSunset)
        tvGanmao = findViewById(R.id.tvGanmao)
        tvForecastTitle = findViewById(R.id.tvForecastTitle)
        llForecast = findViewById(R.id.llForecast)
        llyStatus = findViewById(R.id.llyStatus)
        pbStatus = findViewById(R.id.pbStatus)
        tvError = findViewById(R.id.tvError)
        btnRetry = findViewById(R.id.btnRetry)

        val scrollView = findViewById<ScrollView>(R.id.svContent)
        val statusBar = findViewById<View>(R.id.vStatusBar)
        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = top)
            statusBar.layoutParams.height = top
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        btnRetry.setOnClickListener { loadWeather() }
    }

    /** Volley 异步请求天气接口，成功后用 Gson 自动解析成对象 */
    private fun loadWeather() {
        showLoading()
        WeatherClient.request(
            this, cityCode,
            onSuccess = { response -> bindWeather(response) },
            onFailure = { message -> showError(message) }
        )
    }

    private fun bindWeather(response: WeatherResponse) {
        val data = response.data ?: return
        val today = data.forecast?.firstOrNull()

        tvCityName.text = response.cityInfo?.city ?: cityName
        tvCityProvince.text = response.cityInfo?.parent ?: province
        tvTemperature.text = "${data.wendu ?: "--"}℃"
        val type = today?.type ?: "未知"
        tvWeatherType.text = type
        ivWeatherIcon.setImageResource(WeatherIcon.of(type))
        val updateTime = response.cityInfo?.updateTime ?: response.time.orEmpty()
        tvUpdateTime.text = "更新时间 $updateTime"

        tvStatHumidity.text = data.shidu ?: "--"
        tvStatQuality.text = data.quality ?: "--"
        tvStatPm25.text = data.pm25?.let { trimNumber(it) } ?: "--"
        val wind = listOfNotNull(today?.fx, today?.fl).joinToString(" ")
        tvStatWind.text = wind.ifEmpty { "--" }
        tvStatSunrise.text = today?.sunrise ?: "--"
        tvStatSunset.text = today?.sunset ?: "--"
        tvGanmao.text = data.ganmao ?: "暂无提示"

        val forecast = data.forecast.orEmpty()
        tvForecastTitle.text = "未来 ${forecast.size} 天预报"
        bindForecast(forecast)
        showContent()
    }

    /** 逐条把预报数据填充到未来天气列表 */
    private fun bindForecast(forecast: List<Forecast>) {
        llForecast.removeAllViews()
        forecast.forEachIndexed { index, item ->
            val row = layoutInflater.inflate(R.layout.item_forecast, llForecast, false)
            row.findViewById<TextView>(R.id.tvWeek).text =
                if (index == 0) "${item.week}（今天）" else item.week.orEmpty()
            row.findViewById<TextView>(R.id.tvDate).text = item.ymd?.takeLast(5) ?: item.date.orEmpty()
            row.findViewById<TextView>(R.id.tvType).text = item.type.orEmpty()
            val high = item.high?.replace("高温", "")?.trim().orEmpty()
            val low = item.low?.replace("低温", "")?.trim().orEmpty()
            row.findViewById<TextView>(R.id.tvHighLow).text = "$high / $low"
            row.findViewById<ImageView>(R.id.ivIcon).setImageResource(WeatherIcon.of(item.type))
            llForecast.addView(row)
        }
    }

    private fun trimNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

    private fun showLoading() {
        llyStatus.visibility = View.VISIBLE
        pbStatus.visibility = View.VISIBLE
        tvError.visibility = View.GONE
        btnRetry.visibility = View.GONE
    }

    private fun showContent() {
        llyStatus.visibility = View.GONE
    }

    private fun showError(message: String) {
        llyStatus.visibility = View.VISIBLE
        pbStatus.visibility = View.GONE
        tvError.visibility = View.VISIBLE
        tvError.text = "$message\n城市编码：$cityCode"
        btnRetry.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        WeatherClient.cancel(cityCode)
    }
}
