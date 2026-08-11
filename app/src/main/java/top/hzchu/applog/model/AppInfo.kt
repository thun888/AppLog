package top.hzchu.applog.model

/**
 * 应用元数据模型
 * 格式: packageName|appName|versionName|versionCode|firstInstallTime|lastUpdateTime|installerPackageName|appType|signatureSha256|note|tags
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val installerPackageName: String = "",
    val appType: AppType = AppType.THIRD_PARTY,
    val signatureSha256: String = "",
    val note: String = "",
    val tags: String = ""
) {
    enum class AppType {
        SYSTEM,
        THIRD_PARTY
    }

    companion object {
        const val FIELD_DELIMITER = "|"
        const val ESCAPE_CHAR = "\\"

        /**
         * 从序列化行反序列化
         */
        fun deserialize(line: String): AppInfo? {
            val parts = line.split(FIELD_DELIMITER)
            if (parts.size < 9) return null
            return try {
                AppInfo(
                    packageName = parts[0].replace("\\|", "|"),
                    appName = parts[1].replace("\\|", "|"),
                    versionName = parts[2],
                    versionCode = parts[3].toLong(),
                    firstInstallTime = parts[4].toLong(),
                    lastUpdateTime = parts[5].toLong(),
                    installerPackageName = parts[6],
                    appType = try { AppType.valueOf(parts[7]) } catch (_: Exception) { AppType.THIRD_PARTY },
                    signatureSha256 = parts[8],
                    note = parts.getOrElse(9) { "" }.replace("\\|", "|"),
                    tags = parts.getOrElse(10) { "" }.replace("\\|", "|")
                )
            } catch (e: Exception) {
                null
            }
        }

        /**
         * 序列化为一行
         */
        fun serialize(app: AppInfo): String {
            return listOf(
                app.packageName.replace("|", "\\|"),
                app.appName.replace("|", "\\|"),
                app.versionName,
                app.versionCode.toString(),
                app.firstInstallTime.toString(),
                app.lastUpdateTime.toString(),
                app.installerPackageName,
                app.appType.name,
                app.signatureSha256,
                app.note.replace("|", "\\|"),
                app.tags.replace("|", "\\|")
            ).joinToString(FIELD_DELIMITER)
        }
    }
}
