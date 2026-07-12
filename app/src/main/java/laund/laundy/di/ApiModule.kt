package laund.laundy.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import laund.laundy.data.remote.ApiClient
import laund.laundy.data.remote.DowntifyApi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides
    @Singleton
    fun provideApiService(): DowntifyApi {
        return ApiClient.api
    }
}