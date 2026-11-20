package cn.edu.sicnu.cs.heshuyu.weather

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.edu.sicnu.cs.heshuyu.weather.adapter.CityAdapter
import cn.edu.sicnu.cs.heshuyu.weather.model.CityBean
import cn.edu.sicnu.cs.heshuyu.weather.model.CityItem
import cn.edu.sicnu.cs.heshuyu.weather.util.ResUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.Executors

/**
 * 主界面：多线程读取 res/raw/citycode.json → Gson 解析 → RecyclerView 展示城市列表。
 * 点击某个城市后跳转到 WeatherActivity 查看该城市的天气详情。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var rvCity: RecyclerView
    private lateinit var llyLoading: View
    private lateinit var tvEmpty: TextView
    private lateinit var tvCityCount: TextView
    private lateinit var etSearch: EditText
    private lateinit var adapter: CityAdapter

    /** 多线程：使用固定大小线程池在后台线程读取、解析本地 JSON 文件 */
    private val threadPool = Executors.newFixedThreadPool(2)

    /** Gson 解析得到的全部城市记录 */
    private val allCities = mutableListOf<CityBean>()

    /** 省份 id → 省份名称，便于在列表中显示城市归属并支持按省份搜索 */
    private val provinceNames = mutableMapOf<Int, String>()

    /** 主列表中实际展示的条目（省份标题 + 城市行） */
    private val allItems = mutableListOf<CityItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        initViews()
        loadCityList()
    }

    private fun initViews() {
        rvCity = findViewById(R.id.rvCity)
        llyLoading = findViewById(R.id.llyLoading)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvCityCount = findViewById(R.id.tvCityCount)
        etSearch = findViewById(R.id.etSearch)

        // 顶部渐变标题栏延伸到状态栏下方
        val header = findViewById<View>(R.id.header)
        val headerTop = header.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(header) { view, insets ->
            view.updatePadding(top = headerTop + insets.getInsets(WindowInsetsCompat.Type.statusBars()).top)
            insets
        }
        val listBottom = rvCity.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(rvCity) { view, insets ->
            view.updatePadding(bottom = listBottom + insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
            insets
        }

        adapter = CityAdapter { item -> openWeather(item) }
        rvCity.layoutManager = LinearLayoutManager(this)
        rvCity.adapter = adapter
        rvCity.setHasFixedSize(true)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = filter(s?.toString().orEmpty())
        })
    }

    /** 多线程读取本地城市列表 JSON，并用 Gson 解析 */
    private fun loadCityList() {
        llyLoading.visibility = View.VISIBLE
        rvCity.visibility = View.INVISIBLE
        threadPool.execute {
            val json = ResUtils.readFileFromRaw(this, R.raw.citycode)
            val cities = try {
                Gson().fromJson<List<CityBean>>(json, object : TypeToken<List<CityBean>>() {}.type)
                    .orEmpty()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
            runOnUiThread { onCityLoaded(cities) }
        }
    }

    private fun onCityLoaded(cities: List<CityBean>) {
        if (isFinishing || isDestroyed) return
        allCities.clear()
        allCities.addAll(cities)
        provinceNames.clear()
        cities.filter { it.pid == 0 }.forEach { provinceNames[it.id] = it.cityName.orEmpty() }

        allItems.clear()
        allItems.addAll(buildItems(allCities))

        val count = allCities.count { !it.cityCode.isNullOrBlank() }
        tvCityCount.text = "共 $count 个城市"
        llyLoading.visibility = View.GONE
        rvCity.visibility = View.VISIBLE
        adapter.submitList(allItems)
    }

    /** 把扁平的 JSON 记录整理成“省份标题 + 城市”的列表结构 */
    private fun buildItems(source: List<CityBean>): List<CityItem> {
        val items = mutableListOf<CityItem>()
        val provinces = source.filter { it.pid == 0 }
        provinces.forEach { province ->
            val children = source.filter { it.pid == province.id && !it.cityCode.isNullOrBlank() }
            val self = province.takeIf { !it.cityCode.isNullOrBlank() }
            if (self == null && children.isEmpty()) return@forEach
            val name = province.cityName.orEmpty()
            items.add(CityItem(header = name))
            self?.let { items.add(CityItem(city = it, province = name)) }
            children.forEach { items.add(CityItem(city = it, province = name)) }
        }
        return items
    }

    /** 搜索：按城市名或省份名过滤 */
    private fun filter(keyword: String) {
        if (allItems.isEmpty()) return
        val kw = keyword.trim()
        if (kw.isEmpty()) {
            adapter.submitList(allItems)
            tvEmpty.visibility = View.GONE
            return
        }
        val result = allCities.filter { city ->
            !city.cityCode.isNullOrBlank() &&
                ((city.cityName ?: "").contains(kw, true) ||
                    (provinceNames[city.pid] ?: "").contains(kw, true))
        }
        adapter.submitList(result.map { CityItem(city = it, province = provinceNames[it.pid].orEmpty()) })
        tvEmpty.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openWeather(item: CityItem) {
        val city = item.city ?: return
        val code = city.cityCode
        if (code.isNullOrBlank()) return
        startActivity(Intent(this, WeatherActivity::class.java).apply {
            putExtra(WeatherActivity.EXTRA_CITY_NAME, city.cityName.orEmpty())
            putExtra(WeatherActivity.EXTRA_CITY_CODE, code)
            putExtra(WeatherActivity.EXTRA_PROVINCE, item.province)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        threadPool.shutdownNow()
    }
}
