package com.massager.app.data.remote.upload

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
