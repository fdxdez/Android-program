package cn.edu.sicnu.cs.heshuyu.music.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.edu.sicnu.cs.heshuyu.music.R
import cn.edu.sicnu.cs.heshuyu.music.data.Track

/**
 * 歌曲列表适配器（View 层）。
 *
 * 只负责把歌曲数据显示成列表项、并把点击事件回调给 Activity（Controller），
 * 不参与任何播放逻辑。
 */
class TrackAdapter(
    private val onTrackClickListener: OnTrackClickListener
) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    /** 列表项点击回调，由 Activity 实现 */
    fun interface OnTrackClickListener {
        fun onTrackClick(position: Int)
    }

    /** 列表项的 ViewHolder */
    class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvTrackTitle)
        val subtitle: TextView = itemView.findViewById(R.id.tvTrackSubtitle)
        val indicator: TextView = itemView.findViewById(R.id.tvTrackIndicator)
    }

    private val items = mutableListOf<Track>()

    /** 当前正在播放（或选中）的下标，用于高亮列表项 */
    var playingIndex: Int = -1
        private set

    /** 替换整个歌曲列表（整表换数据，用 notifyDataSetChanged 更简单可靠） */
    @SuppressLint("NotifyDataSetChanged")
    fun submitTracks(tracks: List<Track>) {
        items.clear()
        items.addAll(tracks)
        notifyDataSetChanged()
    }

    /** 当前列表中的歌曲（Activity 绑定服务后要把它同步给服务） */
    fun currentTracks(): List<Track> = items.toList()

    /** 更新高亮的下标（换歌时调用） */
    fun setPlayingIndex(index: Int) {
        if (index == playingIndex) {
            return
        }
        val previous = playingIndex
        playingIndex = index
        if (previous in items.indices) {
            notifyItemChanged(previous)
        }
        if (index in items.indices) {
            notifyItemChanged(index)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_track, parent, false)
        val holder = TrackViewHolder(view)
        view.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onTrackClickListener.onTrackClick(position)
            }
        }
        return holder
    }

    @Suppress("SetTextI18n")
    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = items.getOrNull(position) ?: return
        val context = holder.itemView.context
        val playing = position == playingIndex

        holder.title.text = track.title
        holder.subtitle.text = context.getString(
            R.string.track_subtitle_format,
            track.artist.ifBlank { context.getString(R.string.unknown_artist) },
            formatDuration(track.duration)
        )
        holder.indicator.text = if (playing) context.getString(R.string.track_playing) else ""
        holder.itemView.isActivated = playing

        // 无障碍：告诉读屏用户这一项是第几首、是否正在播放
        holder.itemView.contentDescription = context.getString(
            if (playing) R.string.track_desc_playing else R.string.track_desc,
            position + 1,
            track.displayName
        )
    }

    companion object {
        /** 把毫秒格式化成 mm:ss */
        fun formatDuration(durationMs: Long): String {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
    }
}
