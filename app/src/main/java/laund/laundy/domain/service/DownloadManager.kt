package laund.laundy.domain.service

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import laund.laundy.domain.model.LibrarySong
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class DownloadManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DownloadManager"
    }

    fun getFileName(song: LibrarySong): String {
        return song.path.substringAfterLast("/")
            .ifEmpty { "unknown.mp3" }
    }

    private fun contentResolver(): ContentResolver {
        return context.contentResolver
    }

    private fun getDownloadsDir(): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    }

    /**
     * Проверяет существование файла через MediaStore
     */
    fun isDownloaded(song: LibrarySong): Boolean {
        val fileName = getFileName(song)

        // Сначала проверяем через MediaStore (основной способ)
        val collection = getDownloadsCollection()

        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
            MediaStore.Downloads.RELATIVE_PATH
        )

        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        contentResolver().query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        ).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                val name = cursor.getString(1)
                val path = cursor.getString(2)
                Log.d(TAG, "✅ Найден в MediaStore: id=$id, name='$name', path='$path'")
                return true
            }
        }

        // Если не нашли в MediaStore, проверяем файловую систему
        val file = File(getDownloadsDir(), fileName)
        if (file.exists()) {
            Log.d(TAG, "✅ Найден в ФС: ${file.absolutePath}")
            return true
        }

        Log.d(TAG, "❌ Файл не найден: '$fileName'")
        return false
    }

    private fun getDownloadsCollection(): Uri {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Files.getContentUri("external")
        }
    }

    /**
     * Находит все URI в MediaStore для файла с указанным именем
     */
    private fun findMediaStoreUris(fileName: String): List<Uri> {
        val uris = mutableListOf<Uri>()
        val collection = getDownloadsCollection()

        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME
        )

        // Ищем точное совпадение
        contentResolver().query(
            collection,
            projection,
            "${MediaStore.Downloads.DISPLAY_NAME} = ?",
            arrayOf(fileName),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val name = cursor.getString(1)
                val uri = ContentUris.withAppendedId(collection, id)
                uris.add(uri)
                Log.d(TAG, "🔍 Найден URI: $uri (name='$name')")
            }
        }

        // Ищем дубликаты с суффиксами
        val baseName = fileName.substringBeforeLast(".")
        val extension = fileName.substringAfterLast(".")

        contentResolver().query(
            collection,
            projection,
            "${MediaStore.Downloads.DISPLAY_NAME} LIKE ? AND ${MediaStore.Downloads.DISPLAY_NAME} != ?",
            arrayOf("$baseName (%).$extension", fileName),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val name = cursor.getString(1)
                val uri = ContentUris.withAppendedId(collection, id)
                uris.add(uri)
                Log.d(TAG, "🔍 Найден дубликат URI: $uri (name='$name')")
            }
        }

        return uris
    }

    /**
     * Удаляет файл через MediaStore API
     */
    private fun deleteViaMediaStore(fileName: String): Boolean {
        var deleted = false
        val uris = findMediaStoreUris(fileName)

        Log.d(TAG, "🗑️ Найдено ${uris.size} записей в MediaStore для удаления")

        uris.forEach { uri ->
            try {
                val rowsDeleted = contentResolver().delete(uri, null, null)
                if (rowsDeleted > 0) {
                    deleted = true
                    Log.d(TAG, "🗑️ Успешно удалён через MediaStore: $uri")
                } else {
                    Log.e(TAG, "❌ MediaStore.delete() вернул 0 для: $uri")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "❌ SecurityException при удалении $uri: ${e.message}")

                // Пробуем удалить через альтернативный запрос
                try {
                    val id = ContentUris.parseId(uri)
                    val altDeleted = contentResolver().delete(
                        getDownloadsCollection(),
                        "${MediaStore.Downloads._ID} = ?",
                        arrayOf(id.toString())
                    )
                    if (altDeleted > 0) {
                        deleted = true
                        Log.d(TAG, "🗑️ Удалён альтернативным методом: id=$id")
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "❌ Альтернативное удаление не сработало", e2)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка при удалении $uri: ${e.message}")
            }
        }

        return deleted
    }

    /**
     * Удаляет файл напрямую из файловой системы (для старых Android)
     */
    private fun deleteViaFileSystem(fileName: String): Boolean {
        var deleted = false
        val downloadsDir = getDownloadsDir()

        // Удаляем основной файл
        val file = File(downloadsDir, fileName)
        if (file.exists()) {
            try {
                // На Android 10+ пробуем через MediaStore сначала
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // Не можем удалить напрямую, нужно использовать MediaStore
                    Log.d(TAG, "Android 10+, пропускаем прямое удаление")
                } else {
                    if (file.delete()) {
                        deleted = true
                        Log.d(TAG, "🗑️ Удалён через ФС: ${file.absolutePath}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка File.delete(): ${e.message}")
            }
        }

        // Удаляем дубликаты
        val baseName = fileName.substringBeforeLast(".")
        val extension = fileName.substringAfterLast(".")

        downloadsDir.listFiles()?.forEach { f ->
            val name = f.name
            if (name.startsWith(baseName) && name.endsWith(".$extension")) {
                val middle = name.removePrefix(baseName).removeSuffix(".$extension")
                if (middle.matches(Regex(" \\(\\d+\\)"))) {
                    try {
                        if (f.delete()) {
                            deleted = true
                            Log.d(TAG, "🗑️ Удалён дубликат: ${f.absolutePath}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Ошибка удаления дубликата: ${e.message}")
                    }
                }
            }
        }

        return deleted
    }

    /**
     * Комплексное удаление файла всеми доступными способами
     */
    private suspend fun deleteFileCompletely(fileName: String): Boolean {
        Log.d(TAG, "🗑️ Начинаю удаление: $fileName")

        // Способ 1: Удаление через MediaStore (основной для Android 10+)
        val mediaStoreDeleted = deleteViaMediaStore(fileName)

        // Способ 2: Удаление через файловую систему (для старых Android)
        val fileSystemDeleted = deleteViaFileSystem(fileName)

        val totalDeleted = mediaStoreDeleted || fileSystemDeleted

        // Проверяем результат
        delay(300.milliseconds) // Даём время на завершение операций

        val file = File(getDownloadsDir(), fileName)
        val stillExists = file.exists() || findMediaStoreUris(fileName).isNotEmpty()

        if (stillExists) {
            Log.e(TAG, "❌ Файл всё ещё существует после всех попыток удаления!")

            // Последняя попытка - помечаем на удаление при выходе
            if (file.exists()) {
                try {
                    file.deleteOnExit()
                    Log.d(TAG, "📝 Файл помечен на удаление при выходе: ${file.absolutePath}")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Не удалось пометить на удаление", e)
                }
            }
        }

        Log.d(TAG, if (totalDeleted) "✅ Удаление выполнено" else "❌ Удаление не выполнено")
        return totalDeleted && !stillExists
    }

    suspend fun downloadSong(song: LibrarySong): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(song)
            Log.d(TAG, "⬇️ Начинаю скачивание: $fileName")

            // Удаляем существующий файл если есть
            if (isDownloaded(song)) {
                Log.d(TAG, "📁 Файл существует, удаляю...")
                deleteFileCompletely(fileName)

                // Ждём завершения операций
                delay(500.milliseconds)

                // Проверяем, удалился ли
                if (isDownloaded(song)) {
                    Log.e(TAG, "❌ Не удалось удалить существующий файл")
                    return@withContext Result.failure(
                        Exception("Не удалось удалить существующий файл. Проверьте разрешения приложения.")
                    )
                }
            }

            // Создаём новый файл для скачивания
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val collection = getDownloadsCollection()
            val uri: Uri? = contentResolver().insert(collection, contentValues)

            if (uri == null) {
                Log.e(TAG, "❌ Не удалось создать запись в MediaStore")
                return@withContext Result.failure(Exception("Cannot create file entry"))
            }

            Log.d(TAG, "📝 Создана запись: $uri")

            try {
                val url = URL(song.streamUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    contentResolver().delete(uri, null, null)
                    Log.e(TAG, "❌ HTTP ошибка: ${connection.responseCode}")
                    return@withContext Result.failure(Exception("HTTP ${connection.responseCode}"))
                }

                contentResolver().openOutputStream(uri)?.use { output ->
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytes = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                        }
                        Log.d(TAG, "📥 Загружено $totalBytes байт")
                    }
                } ?: run {
                    contentResolver().delete(uri, null, null)
                    Log.e(TAG, "❌ Не удалось открыть поток для записи")
                    return@withContext Result.failure(Exception("Cannot open output stream"))
                }

                connection.disconnect()

                // Завершаем скачивание
                val updateValues = ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }
                contentResolver().update(uri, updateValues, null, null)

                Log.d(TAG, "✅ Скачивание успешно: $fileName")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка при скачивании", e)
                try {
                    contentResolver().delete(uri, null, null)
                } catch (deleteException: Exception) {
                    Log.e(TAG, "❌ Ошибка при очистке", deleteException)
                }
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Общая ошибка", e)
            Result.failure(e)
        }
    }

    suspend fun deleteSong(song: LibrarySong): Boolean {
        val fileName = getFileName(song)
        Log.d(TAG, "🗑️ Запрос на удаление: $fileName")
        return deleteFileCompletely(fileName)
    }

    /**
     * Получает список всех MP3 файлов через MediaStore
     */
    fun getDownloadedSongs(): Set<String> {
        val result = mutableSetOf<String>()
        val collection = getDownloadsCollection()

        val projection = arrayOf(MediaStore.Downloads.DISPLAY_NAME)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%.mp3")

        contentResolver().query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(MediaStore.Downloads.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val fileName = cursor.getString(nameIndex)
                result.add(fileName)
                Log.d(TAG, "📁 Найден: '$fileName'")
            }
        }

        // Дополнительно сканируем файловую систему
        val downloadsDir = getDownloadsDir()
        if (downloadsDir.exists() && downloadsDir.isDirectory) {
            downloadsDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".mp3", ignoreCase = true)) {
                    result.add(file.name)
                    Log.d(TAG, "📁 Найден в ФС: '${file.name}'")
                }
            }
        }

        Log.d(TAG, "📁 Всего найдено MP3: ${result.size}")
        return result
    }
}