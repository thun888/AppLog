package top.hzchu.applog.serializer

import top.hzchu.applog.model.AppInfo

/**
 * 确定性序列化器
 * 将应用列表序列化为按 packageName 字典序排序的文本格式
 */
object AppListSerializer {

    /**
     * 序列化应用列表为文本
     */
    fun serialize(apps: List<AppInfo>): String {
        // 确定性排序: 按 packageName 字典序
        val sorted = apps.sortedBy { it.packageName.lowercase() }
        return sorted.joinToString("\n") { AppInfo.serialize(it) }
    }

    /**
     * 从文本反序列化为应用列表
     */
    fun deserialize(text: String): List<AppInfo> {
        if (text.isBlank()) return emptyList()
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { AppInfo.deserialize(it) }
    }

    /**
     * 构建两个列表的差异信息
     * oldList 是旧快照, newList 是新快照
     */
    fun buildAppMap(apps: List<AppInfo>): Map<String, AppInfo> {
        return apps.associateBy { it.packageName }
    }
}
