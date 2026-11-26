package com.prince.whatstoolbox.util
import android.content.*
import android.net.Uri
import android.os.PersistableBundle

object NumberCleaner {
    /**
     * Returns digits only, in international format.
     * "+233 (0)24 412 3456", "233244123456", "0244123456" and "00233244123456" all become "233244123456" (with defaultCc = 233).
     */
    fun clean(raw: String, defaultCc: String = "233"): String {
        val t = raw.trim(); var d = t.filter(Char::isDigit)
        val explicit = t.startsWith("+") || t.startsWith("00"); if (t.startsWith("00")) d = d.drop(2)
        if (explicit || (d.startsWith(defaultCc) && d.length >= defaultCc.length + 8)) return d.replaceFirst(Regex("^${Regex.escape(defaultCc)}0"), defaultCc)   // drops the "(0)" trunk digit
        return defaultCc + d.trimStart('0')
    }
    fun isValid(d: String) = d.length in 8..15
}
object Wa {
    val PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")
    /** Installed WhatsApp variants, regular first. */
    fun installed(c: Context): List<String> = PACKAGES.filter { p -> runCatching { c.packageManager.getPackageInfo(p, 0) }.isSuccess }
    fun link(number: String = "", text: String = ""): Uri =
        Uri.parse("https://wa.me/$number").buildUpon().apply { if (text.isNotBlank()) appendQueryParameter("text", text) }.build()
    /** Returns false (instead of crashing) when nothing can handle the link. [pkg] pins a specific WhatsApp variant. */
    fun openChat(c: Context, number: String, text: String = "", pkg: String? = null): Boolean {
        val i = Intent(Intent.ACTION_VIEW, link(number, text)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); pkg?.let { i.setPackage(it) }
        return runCatching { c.startActivity(i) }.isSuccess
    }
    fun shareText(c: Context, text: String) {
        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        val pkg = installed(c).firstOrNull()
        runCatching { c.startActivity((if (pkg != null) Intent(i).setPackage(pkg) else Intent.createChooser(i, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
    /** [sensitive] hides the clip preview in the Android 13+ clipboard overlay (use for keys / PINs). */
    fun copy(c: Context, text: String, sensitive: Boolean = false) {
        val clip = ClipData.newPlainText("toolbox", text)
        if (sensitive) clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
        (c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
    }
    fun clipboardHasText(c: Context): Boolean = (c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClipDescription?.hasMimeType("text/*") == true
    fun readClipboard(c: Context): String = (c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip?.getItemAt(0)?.coerceToText(c)?.toString().orEmpty()
}
object Username {
    private val rx = Regex("^[a-z][a-z0-9._]{2,29}$")
    fun normalize(s: String) = s.trim().lowercase().removePrefix("@")
    fun isValid(s: String) = normalize(s).let { rx.matches(it) && !it.contains("..") && !it.endsWith(".") }
    /** Copies handle, then opens WhatsApp (no public deep link exists for username search). Returns false if WhatsApp isn't installed. */
    fun findInWhatsApp(c: Context, handle: String): Boolean {
        Wa.copy(c, "@" + normalize(handle))
        val i = Wa.installed(c).firstNotNullOfOrNull { c.packageManager.getLaunchIntentForPackage(it) } ?: return false
        return runCatching { c.startActivity(i) }.isSuccess
    }
}
object TrackingCleaner {
    private val always = Regex("^(utm_.*|fbclid|gclid|dclid|igshid|igsh|mc_eid|mc_cid|msclkid|twclid|yclid|ref_src|_hsenc|_hsmi)$", RegexOption.IGNORE_CASE)
    /** Params that are tracking only on some sites (e.g. `ref=` is meaningful on code hosts, `si=` only on media share links). */
    private fun hostSpecific(name: String, host: String) =
        (name.equals("si", true) && (host.endsWith("youtube.com") || host == "youtu.be" || host.endsWith("spotify.com"))) ||
        (host.contains("amazon.") && Regex("^(ref|ref_|pf_rd_.*|pd_rd_.*|qid|sr)$", RegexOption.IGNORE_CASE).matches(name))
    /** Facebook / Instagram / Google "you are leaving" redirects carry the real URL in a parameter. */
    private fun unwrap(u: Uri): String? {
        val host = u.host?.lowercase().orEmpty(); val path = u.path.orEmpty()
        val v = when {
            host.endsWith("facebook.com") && path.startsWith("/l.php") -> u.getQueryParameter("u")
            host == "l.instagram.com" -> u.getQueryParameter("u")
            host.endsWith("google.com") && path == "/url" -> u.getQueryParameter("q") ?: u.getQueryParameter("url")
            else -> null }
        return v?.takeIf { it.startsWith("http", true) }
    }
    fun clean(url: String, depth: Int = 0): String = try {
        val u = Uri.parse(url.trim()); val inner = if (depth < 2) unwrap(u) else null
        if (inner != null) clean(inner, depth + 1) else {
            val host = u.host?.lowercase().orEmpty(); val b = u.buildUpon().clearQuery()
            u.queryParameterNames.filterNot { always.matches(it) || hostSpecific(it, host) }.forEach { k -> u.getQueryParameters(k).forEach { b.appendQueryParameter(k, it) } }
            b.build().toString() }
    } catch (e: Exception) { url }
}
