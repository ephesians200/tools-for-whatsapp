package com.prince.whatstoolbox.domain.repository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class VaultEntry(val id: Long = 0, val kind: String, val title: String, val body: String, val extra: String = "", val createdAt: Long = 0)
/** themeMode: 0 = follow system, 1 = light, 2 = dark. [amoled] only applies when the resolved theme is dark. */
data class AppSettings(val amoled: Boolean = false, val dynamicColor: Boolean = false, val themeMode: Int = 0, val countryCode: String = "233", val recents: List<String> = emptyList())

interface VaultRepository { fun observe(kind: String): Flow<List<VaultEntry>>; suspend fun save(e: VaultEntry); suspend fun delete(id: Long) }
interface FavoritesRepository { val favorites: Flow<Set<String>>; suspend fun toggle(id: String) }
interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    fun setAmoled(v: Boolean); fun setDynamicColor(v: Boolean); fun setThemeMode(v: Int); fun setCountryCode(v: String); fun addRecent(id: String)
}
interface SecureStore { fun get(key: String): String; fun put(key: String, value: String) }
