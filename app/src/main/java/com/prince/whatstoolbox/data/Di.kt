package com.prince.whatstoolbox.data
import android.content.Context
import com.prince.whatstoolbox.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class) object Di {
    @Provides @Singleton fun db(@ApplicationContext c: Context) = AppDb.build(c)
    @Provides fun dao(db: AppDb) = db.dao()
}
@Module @InstallIn(SingletonComponent::class) abstract class RepoModule {
    @Binds abstract fun vault(i: VaultRepositoryImpl): VaultRepository
    @Binds abstract fun favorites(i: FavoritesRepositoryImpl): FavoritesRepository
    @Binds abstract fun settings(i: SettingsRepositoryImpl): SettingsRepository
    @Binds abstract fun secure(i: SecureStoreImpl): SecureStore
}
