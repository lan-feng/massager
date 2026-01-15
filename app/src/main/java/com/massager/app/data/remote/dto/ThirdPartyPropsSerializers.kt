package com.massager.app.data.remote.dto

// 文件说明：兼容字符串或对象形式的 userSettings 反序列化。
import kotlinx.serialization.KSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.json.JSONObject

@OptIn(ExperimentalSerializationApi::class)
object ThirdPartyPropsMapSerializer : KSerializer<Map<String, ThirdPartyProps?>?> {
    private val delegate = MapSerializer(String.serializer(), ThirdPartyProps.serializer().nullable)

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): Map<String, ThirdPartyProps?>? {
        if (decoder !is JsonDecoder) {
            return decoder.decodeNullableSerializableValue(delegate)
        }
        val element: JsonElement = decoder.decodeJsonElement()
        return when (element) {
            is JsonNull -> null
            is JsonObject -> decoder.json.decodeFromJsonElement(delegate, element)
            is JsonPrimitive -> decodeFromStringSafely(decoder, element)
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, ThirdPartyProps?>?) {
        encoder.encodeNullableSerializableValue(delegate, value)
    }

    private fun decodeFromStringSafely(
        decoder: JsonDecoder,
        primitive: JsonPrimitive
    ): Map<String, ThirdPartyProps?>? {
        if (!primitive.isString) return null
        val raw = primitive.content
        return runCatching {
            val jsonObject = JSONObject(raw)
            jsonObject.keys().asSequence().associateWith { key ->
                jsonObject.optJSONObject(key)?.let { obj ->
                    ThirdPartyProps(
                        name = obj.optString("name", null)?.takeIf { it.isNotBlank() },
                        email = obj.optString("email", null)?.takeIf { it.isNotBlank() }
                    )
                }
            }
        }.getOrNull()
    }
}
