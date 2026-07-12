package laund.laundy.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import laund.laundy.data.remote.DowntifyApi
import laund.laundy.data.repository.LibraryRepositoryImpl
import laund.laundy.domain.repository.LibraryRepository

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideLibraryRepository(
        api: DowntifyApi
    ): LibraryRepository =
        LibraryRepositoryImpl(api)

}