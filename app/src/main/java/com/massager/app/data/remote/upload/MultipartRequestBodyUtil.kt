package com.massager.app.data.remote.upload

// 文件说明：工具方法，构建带文件的 Multipart 请求体。
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class FilePart(
    val fileName: String,
    val bytes: ByteArray,
    val mimeType: String = "image/jpeg"
)

object MultipartRequestBodyUtil {
    fun fromBytes(partName: String, part: FilePart): MultipartBody.Part {
        val requestBody = part.bytes.toRequestBody(part.mimeType.toMediaType())
        return MultipartBody.Part.createFormData(
            partName,
            part.fileName,
            requestBody
        )
    }
}
