package cn.edu.sicnu.cs.heshuyu.weather.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.edu.sicnu.cs.heshuyu.weather.R
import cn.edu.sicnu.cs.heshuyu.weather.model.CityItem

/**
 * 城市列表适配器：支持两种条目类型（省份分组标题 / 城市行）。
 * 点击城市行时通过 onClick 回调把整条数据交给 Activity 处理。
 */
class CityAdapter(private val onClick: (CityItem) -> Unit) :
    RecyclerView.Adapter<CityAdapter.BaseHolder>() {

    private val items = mutableListOf<CityItem>()

    fun submitList(list: List<CityItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int =
        if (items[position].header != null) TYPE_PROVINCE else TYPE_CITY

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_PROVINCE) {
            ProvinceHolder(inflater.inflate(R.layout.item_province, parent, false))
        } else {
            CityHolder(inflater.inflate(R.layout.item_city, parent, false))
        }
    }

    override fun onBindViewHolder(holder: BaseHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is ProvinceHolder -> holder.province.text = item.header
            is CityHolder -> {
                val city = item.city ?: return
                holder.name.text = city.cityName
                holder.code.text =
                    if (item.province.isBlank()) city.cityCode.orEmpty()
                    else "${item.province} · ${city.cityCode}"
                holder.itemView.setOnClickListener { onClick(item) }
            }
        }
    }

    companion object {
        const val TYPE_PROVINCE = 0
        const val TYPE_CITY = 1
    }

    abstract class BaseHolder(view: View) : RecyclerView.ViewHolder(view)

    class ProvinceHolder(view: View) : BaseHolder(view) {
        val province: TextView = view.findViewById(R.id.tvProvince)
    }

    class CityHolder(view: View) : BaseHolder(view) {
        val name: TextView = view.findViewById(R.id.tvCityName)
        val code: TextView = view.findViewById(R.id.tvCityCode)
    }
}
