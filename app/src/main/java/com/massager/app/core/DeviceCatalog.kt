package com.massager.app.core

// 文件说明：从资源或默认值加载设备类型定义，用于解析产品信息并给出内部设备类型。
import android.content.Context
import androidx.annotation.RawRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class DeviceCatalog @Inject constructor(
    @ApplicationContext context: Context
) {

    private val definitions: List<DeviceTypeDefinition>

    init {
        definitions = loadDefinitions(
            context = context,
            rawRes = context.resources.getIdentifier(
                DEVICE_TYPES_RESOURCE_NAME,
                "raw",
                context.packageName
            )
        ).ifEmpty { DEFAULT_DEFINITIONS }
    }

    val reservedDeviceTypes: List<Int> =
        definitions.map(DeviceTypeDefinition::typeId).ifEmpty { DEFAULT_RESERVED_TYPES }

    fun resolveType(productId: Int?, name: String?): Int {
        val byProduct = productId?.let { id ->
            definitions.firstOrNull { definition -> definition.productIds.contains(id) }
        }
        if (byProduct != null) return byProduct.typeId

        val normalized = name.orEmpty().lowercase()
        val byName = definitions.firstOrNull { definition ->
            definition.nameKeywords.any { keyword -> normalized.contains(keyword.lowercase()) }
        }
        return byName?.typeId ?: reservedDeviceTypes.first()
    }

    private fun loadDefinitions(context: Context, @RawRes rawRes: Int): List<DeviceTypeDefinition> {
        if (rawRes == 0) return emptyList()
        return runCatching {
            context.resources.openRawResource(rawRes).use { stream ->
                val json = stream.bufferedReader().readText()
                val array = JSONArray(json)
                buildList {
                    for (index in 0 until array.length()) {
                        val item = array.getJSONObject(index)
                        add(item.toDefinition())
                    }
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun JSONObject.toDefinition(): DeviceTypeDefinition =
        DeviceTypeDefinition(
            typeId = getInt("typeId"),
            productIds = optJSONArray("productIds")?.toIntList().orEmpty(),
            nameKeywords = optJSONArray("nameKeywords")?.toStringList().orEmpty()
        )

    private fun JSONArray.toIntList(): List<Int> =
        buildList {
            for (index in 0 until length()) {
                add(getInt(index))
            }
        }

    private fun JSONArray.toStringList(): List<String> =
        buildList {
            for (index in 0 until length()) {
                add(getString(index))
            }
        }

    data class DeviceTypeDefinition(
        val typeId: Int,
        val productIds: List<Int>,
        val nameKeywords: List<String>
    )

    companion object {
        private const val DEVICE_TYPES_RESOURCE_NAME = "device_types"
        private val DEFAULT_RESERVED_TYPES = (10..19).toList()
        private val DEFAULT_DEFINITIONS = DEFAULT_RESERVED_TYPES.map { typeId ->
            DeviceTypeDefinition(
                typeId = typeId,
                productIds = emptyList(),
                nameKeywords = emptyList()
            )
        }
    }
}
