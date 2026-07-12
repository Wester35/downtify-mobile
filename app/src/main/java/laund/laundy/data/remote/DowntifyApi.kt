package laund.laundy.data.remote

import laund.laundy.data.remote.dto.DownloadJobDto
import laund.laundy.data.remote.dto.SettingsDto
import laund.laundy.data.remote.dto.SongDto
import laund.laundy.data.remote.request.DownloadBatchRequest
import laund.laundy.data.remote.response.BatchDownloadResponse
import laund.laundy.data.remote.response.DeleteFileResponse
import laund.laundy.data.remote.response.QueueClearResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface DowntifyApi {
    @GET("api/version")
    suspend fun getVersion(): String

    @GET("list")
    suspend fun getLibrary(): List<String>

    @GET("api/songs/search")
    suspend fun searchSongs(
        @Query("query") query: String
    ): List<SongDto>

    @GET("api/song/url")
    suspend fun resolveSpotifyUrl(
        @Query("url") url: String
    ): Any

    @POST("api/download/url")
    suspend fun downloadSong(
        @Query("url") url: String,
        @Query("client_id") clientId: String? = null
    ): String

    @POST("api/download/batch")
    suspend fun downloadBatch(
        @Body request: DownloadBatchRequest
    ): BatchDownloadResponse

    @GET("api/queue")
    suspend fun getQueue(): List<DownloadJobDto>

    @DELETE("api/queue")
    suspend fun clearQueue(): QueueClearResponse

    @DELETE("api/queue/item")
    suspend fun removeQueueItem(
        @Query("song_id") songId: String
    ): QueueClearResponse

    @GET("api/settings")
    suspend fun getSettings(): SettingsDto

//    @POST("api/settings/update")
//    suspend fun updateSettings(
//        @Body request: SettingsUpdateRequest
//    ): SettingsDto

    @GET("cover")
    suspend fun getCover(
        @Query("file") file: String
    ): okhttp3.ResponseBody

    @DELETE("delete")
    suspend fun deleteFile(
        @Query("file") file: String
    ): DeleteFileResponse

}