package com.prince.whatstoolbox
import android.content.*
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

/** Implements WhatsApp's sticker content-provider contract. Packs live in filesDir/stickers/<id>/ (name, emoji, version, tray.png, *.webp). */
class StickerProvider : ContentProvider() {
    companion object {
        fun root(c: Context) = File(c.filesDir, "stickers").apply { mkdirs() }
        fun authority(c: Context) = "${c.packageName}.stickercontentprovider"
        private fun safe(s: String) = s.isNotEmpty() && !s.contains("..") && !s.contains('/') && !s.contains('\\')
    }
    override fun onCreate() = true
    override fun query(uri: Uri, p: Array<String>?, s: String?, a: Array<String>?, o: String?): Cursor? {
        val seg = uri.pathSegments; val root = root(context!!)
        if (seg.size > 1 && !safe(seg[1])) return null
        return when (seg.firstOrNull()) {
            "metadata" -> MatrixCursor(arrayOf("sticker_pack_identifier", "sticker_pack_name", "sticker_pack_publisher", "sticker_pack_icon", "android_play_store_link", "ios_app_download_link",
                "sticker_pack_publisher_email", "sticker_pack_publisher_website", "sticker_pack_privacy_policy_website", "sticker_pack_license_agreement_website",
                "image_data_version", "whatsapp_will_not_cache_stickers", "animated_sticker_pack", "avoid_cache")).also { c ->
                (if (seg.size > 1) listOf(seg[1]) else root.list()?.toList().orEmpty()).forEach { id -> val d = File(root, id)
                    // image_data_version changes whenever the pack is rebuilt, so WhatsApp refreshes its cache
                    if (d.isDirectory && File(d, "name").exists()) c.addRow(arrayOf<Any>(id, File(d, "name").readText(), (context?.getString(R.string.app_name) ?: "Sticker pack"), "tray.png", "", "", "", "", "", "",
                        runCatching { File(d, "version").readText().trim() }.getOrDefault("1").ifBlank { "1" }, 0, 0, 0)) } }
            "stickers" -> if (seg.size < 2) null else MatrixCursor(arrayOf("sticker_file_name", "sticker_emoji", "sticker_accessibility_text")).also { c ->
                val emoji = runCatching { File(File(root, seg[1]), "emoji").readText().trim() }.getOrDefault("").ifBlank { "😀" }
                File(root, seg[1]).listFiles { f -> f.extension == "webp" }?.sortedBy { it.name }?.forEach { c.addRow(arrayOf<Any>(it.name, emoji, "sticker")) } }
            else -> null
        }
    }
    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val seg = uri.pathSegments; if (seg.size != 3 || seg.any { !safe(it) }) return null
        val f = File(File(root(context!!), seg[1]), seg[2]); if (!f.exists()) return null
        return AssetFileDescriptor(ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY), 0, AssetFileDescriptor.UNKNOWN_LENGTH)
    }
    override fun getType(uri: Uri) = when (uri.pathSegments.firstOrNull()) {
        "metadata" -> "vnd.android.cursor.${if (uri.pathSegments.size > 1) "item" else "dir"}/vnd.${authority(context!!)}.metadata"
        "stickers" -> "vnd.android.cursor.dir/vnd.${authority(context!!)}.stickers"
        else -> if (uri.lastPathSegment?.endsWith(".png") == true) "image/png" else "image/webp" }
    override fun insert(u: Uri, v: ContentValues?): Uri? = null
    override fun delete(u: Uri, s: String?, a: Array<String>?) = 0
    override fun update(u: Uri, v: ContentValues?, s: String?, a: Array<String>?) = 0
}
