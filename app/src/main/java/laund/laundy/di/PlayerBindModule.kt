package laund.laundy.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import laund.laundy.domain.service.ExoPlayerService
import laund.laundy.domain.service.PlayerService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerBindModule {
    @Binds
    @Singleton
    abstract fun bindPlayerService(
        impl: ExoPlayerService
    ): PlayerService
}