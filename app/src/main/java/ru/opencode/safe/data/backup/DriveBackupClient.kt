package ru.opencode.safe.data.backup

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class DriveFile(val id: String, val name: String, val modifiedTime: String?)

/**
 * Резервная копия в Google Drive через REST API. Файл лежит в скрытой папке приложения
 * (appDataFolder) — он не виден в «Моём диске» и доступен только этому приложению.
 */
class DriveBackupClient(private val http: OkHttpClient) {

    fun findBackup(token: String): DriveFile? {
        val json = JSONObject(get(token, listUrl()))
        val files = json.optJSONArray("files") ?: JSONArray()
        // Копий с этим именем быть не должно больше одной, но если есть — берём свежую.
        // Время в формате RFC 3339, поэтому сравниваем как строки и не просим сортировку у Google.
        return (0 until files.length())
            .mapNotNull { files.optJSONObject(it) }
            .map { toFile(it) }
            .filter { it.name == FILE_NAME }
            .maxByOrNull { it.modifiedTime ?: "" }
    }

    fun upload(token: String, bytes: ByteArray, existingId: String?): DriveFile {
        val media = bytes.toRequestBody(OCTET_STREAM)
        return if (existingId == null) {
            val metadata = JSONObject()
                .put("name", FILE_NAME)
                .put("parents", JSONArray().put("appDataFolder"))
                .toString()
                .toRequestBody(JSON)
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("metadata", null, metadata)
                .addFormDataPart("file", FILE_NAME, media)
                .build()
            val request = Request.Builder()
                .url("$UPLOAD?uploadType=multipart&fields=id,name,modifiedTime")
                .header("Authorization", "Bearer $token")
                .post(multipart)
                .build()
            toFile(JSONObject(execute(request, "не удалось сохранить копию в Google Drive")))
        } else {
            val request = Request.Builder()
                .url("$UPLOAD/$existingId?uploadType=media&fields=id,name,modifiedTime")
                .header("Authorization", "Bearer $token")
                .patch(media)
                .build()
            toFile(JSONObject(execute(request, "не удалось обновить копию в Google Drive")))
        }
    }

    fun download(token: String, id: String): ByteArray {
        val request = Request.Builder()
            .url("$FILES/$id?alt=media")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException(DriveErrors.describe(response.code, response.body?.string().orEmpty(), "не удалось скачать копию"))
            }
            return response.body?.bytes() ?: throw IOException("Google Drive вернул пустой файл")
        }
    }

    private fun get(token: String, url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        return execute(request, "не удалось получить список копий")
    }

    private fun execute(request: Request, what: String): String =
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(DriveErrors.describe(response.code, text, what))
            }
            text
        }

    private fun toFile(json: JSONObject) = DriveFile(
        id = json.optString("id"),
        name = json.optString("name"),
        modifiedTime = json.optString("modifiedTime").ifBlank { null },
    )

    companion object {
        const val FILE_NAME = "personal-safe-backup.bin"

        /**
         * Список файлов в скрытой папке приложения. В Drive API v3 она называется
         * `appDataFolder`: старые написания `appdata` и `appfolder` из v2 отвергаются
         * с ошибкой 400 «Invalid Value».
         */
        fun listUrl(): String = "$FILES?spaces=appDataFolder&fields=files(id,name,modifiedTime)"

        private const val FILES = "https://www.googleapis.com/drive/v3/files"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3/files"
        private val JSON = "application/json".toMediaType()
        private val OCTET_STREAM = "application/octet-stream".toMediaType()
    }
}
