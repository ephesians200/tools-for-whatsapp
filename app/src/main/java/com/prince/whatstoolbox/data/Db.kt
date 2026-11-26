package com.prince.whatstoolbox.data
import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** kind: SNIPPET | DRAFT | NOTE | HANDLE | TEMPLATE */
@Entity(tableName = "vault_items") data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val kind: String, val title: String,
    val body: String, val extra: String = "", val createdAt: Long = System.currentTimeMillis())
@Entity(tableName = "crm_notes") data class CrmNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val phoneOrHandle: String, val name: String, val note: String)
@Entity(tableName = "favorites") data class Favorite(@PrimaryKey val toolId: String)

@Dao interface VaultDao {
    @Query("SELECT * FROM vault_items WHERE kind=:kind ORDER BY createdAt DESC") fun items(kind: String): Flow<List<VaultItem>>
    @Query("SELECT * FROM vault_items ORDER BY createdAt DESC") fun allItems(): Flow<List<VaultItem>>
    @Upsert suspend fun save(item: VaultItem)
    @Query("DELETE FROM vault_items WHERE id=:id") suspend fun deleteById(id: Long)
    @Delete suspend fun delete(item: VaultItem)
    @Query("SELECT * FROM crm_notes") fun crm(): Flow<List<CrmNote>>
    @Upsert suspend fun saveCrm(n: CrmNote)
    @Query("SELECT toolId FROM favorites") fun favorites(): Flow<List<String>>
    @Upsert suspend fun fav(f: Favorite)
    @Delete suspend fun unfav(f: Favorite)
}
@Database(entities = [VaultItem::class, CrmNote::class, Favorite::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() { abstract fun dao(): VaultDao
    companion object { fun build(c: Context) = Room.databaseBuilder(c, AppDb::class.java, "toolbox.db").build() } }
