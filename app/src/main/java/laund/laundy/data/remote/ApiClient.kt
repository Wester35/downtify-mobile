package laund.laundy.data.remote

import laund.laundy.domain.AppConstants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = AppConstants.API_URL

    val api: DowntifyApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DowntifyApi::class.java)
    }
}