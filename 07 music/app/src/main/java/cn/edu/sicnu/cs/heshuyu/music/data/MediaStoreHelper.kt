package cn.edu.sicnu.cs.heshuyu.music.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

/**
 * 用 ContentResolver 查询系统媒体库里的音乐文件（实验七要求(1)）。
 *
 * 查询的是 `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`，
 * 只保留 `IS_MUSIC != 0` 且时长大于 0 的记录，并按歌曲名排序。
 *
 * 注意：Android 13（API 33）之后读取音乐需要 `READ_MEDIA_AUDIO` 权限，
 * 权限由 Activity 申请，查询前必须已经授权，否则系统会抛出 SecurityException；
 * 这里把查询包在 try/catch 中并返回空列表，避免应用直接崩溃。
 */
object MediaStoreHelper {

    /** 查询的列 */
    private val PROJECTION = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION
    )

    /** 只查音乐文件，并按歌曲名排序 */
    private const val SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    private const val SORT_ORDER = "${MediaStore.Audio.Media.TITLE} ASC"

    /**
     * 查询音乐文件列表。
     *
     * @return 查询到的歌曲；没有权限或媒体库为空时返回空列表
     */
    fun queryAudioTracks(context: Context): List<Track> {
        val tracks = mutableListOf<Track>()
        val collection: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(collection, PROJECTION, SELECTION, null, SORT_ORDER)
            cursor?.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (c.moveToNext()) {
                    val id = c.getLong(idColumn)
                    val duration = c.getLong(durationColumn)
                    if (duration <= 0) {
                        continue
                    }
                    val title = c.getString(titleColumn).orEmpty()
                    val uri = ContentUris.withAppendedId(collection, id)
                    tracks.add(
                        Track(
                            id = id,
                            title = title.ifBlank { uri.lastPathSegment.orEmpty() },
                            artist = c.getString(artistColumn).orEmpty(),
                            album = c.getString(albumColumn).orEmpty(),
                            duration = duration,
                            uri = uri.toString()
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            // 没有读取音乐的权限，返回空列表由界面提示用户授权
            return emptyList()
        } finally {
            cursor?.close()
        }
        return tracks
    }
}
