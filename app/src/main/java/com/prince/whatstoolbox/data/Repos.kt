package com.prince.whatstoolbox.data
import android.content.Context
import com.prince.whatstoolbox.domain.repository.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class VaultRepositoryImpl @Inject constructor(private val dao: VaultDao) : VaultRepository {
    override fun observe(kind: String) = dao.items(kind).map { l ->
        l.map { VaultEntry(it.id, it.kind, Crypto.dec(it.title), Crypto.dec(it.body), Crypto.dec(it.extra), it.createdAt) } }.flowOn(Dispatchers.Default)
    override suspend fun save(e: VaultEntry) = dao.save(VaultItem(e.id, e.kind, Crypto.enc(e.title), Crypto.enc(e.body), Crypto.enc(e.extra), if (e.createdAt == 0L) System.currentTimeMillis() else e.createdAt))
    override suspend fun delete(id: Long) = dao.deleteById(id)
}
@Singleton class FavoritesRepositoryImpl @Inject constructor(private val dao: VaultDao) : FavoritesRepository {
    override val favorites = dao.favorites().map { it.toSet() }
    override suspend fun toggle(id: String) { if (id in dao.favorites().first()) dao.unfav(Favorite(id)) else dao.fav(Favorite(id)) }
}
@Singleton class SettingsRepositoryImpl @Inject constructor(@ApplicationContext c: Context) : SettingsRepository {
    private val prefs = c.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _s = MutableStateFlow(AppSettings(
        amoled = prefs.getBoolean("amoled", false), dynamicColor = prefs.getBoolean("dynamic", false), themeMode = prefs.getInt("theme_mode", 0),
        countryCode = prefs.getString("cc", "233").orEmpty().ifBlank { "233" },
        recents = prefs.getString("recents", "").orEmpty().split(",").filter { it.isNotBlank() }))
    override val settings: StateFlow<AppSettings> = _s.asStateFlow()
    override fun setAmoled(v: Boolean) { prefs.edit().putBoolean("amoled", v).apply(); _s.update { it.copy(amoled = v) } }
    override fun setDynamicColor(v: Boolean) { prefs.edit().putBoolean("dynamic", v).apply(); _s.update { it.copy(dynamicColor = v) } }
    override fun setThemeMode(v: Int) { prefs.edit().putInt("theme_mode", v).apply(); _s.update { it.copy(themeMode = v) } }
    override fun setCountryCode(v: String) { val d = v.filter(Char::isDigit).take(4); prefs.edit().putString("cc", d).apply(); _s.update { it.copy(countryCode = d.ifBlank { "233" }) } }
    override fun addRecent(id: String) { val l = (listOf(id) + _s.value.recents.filter { it != id }).take(6); prefs.edit().putString("recents", l.joinToString(",")).apply(); _s.update { it.copy(recents = l) } }
}
/** Small secrets (own number, recent numbers): values are encrypted with the same Keystore key as the vault. */
@Singleton class SecureStoreImpl @Inject constructor(@ApplicationContext c: Context) : SecureStore {
    private val prefs = c.getSharedPreferences("secure_v2", Context.MODE_PRIVATE)
    override fun get(key: String) = Crypto.dec(prefs.getString(key, "").orEmpty())
    override fun put(key: String, value: String) { prefs.edit().putString(key, Crypto.enc(value)).apply() }
}
