package laund.laundy.domain.service

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val downloadDir: File
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    fun isDownloaded(song: laund.laundy.domain.model.LibrarySong): Boolean {
        val fileName = getFileName(song)
        val file = File(downloadDir, fileName)
        return file.exists()
    }

    fun getFileName(song: laund.laundy.domain.model.LibrarySong): String {
        return "${sanitizeFileName(song.artist)} - ${sanitizeFileName(song.title)}.mp3"
    }

    suspend fun downloadSong(song: laund.laundy.domain.model.LibrarySong): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = getFileName(song)
                val file = File(downloadDir, fileName)

                if (file.exists()) {
                    return@withContext Result.success(file)
                }

                val url = URL(song.streamUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext Result.failure(
                        Exception("Server returned HTTP ${connection.responseCode}")
                    )
                }

                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(file)

                val buffer = ByteArray(4096)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.close()
                inputStream.close()
                connection.disconnect()

                Result.success(file)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun deleteSong(song: laund.laundy.domain.model.LibrarySong): Boolean {
        val fileName = getFileName(song)
        val file = File(downloadDir, fileName)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9а-яА-Я\\-_.\\s]"), "")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}