package top.hzchu.applog.utils

import com.github.promeg.pinyinhelper.Pinyin

object PinyinUtils {
    /**
     * 将字符串转换为拼音，用于排序
     */
    fun getPinyin(text: String): String {
        return Pinyin.toPinyin(text, "").lowercase()
    }

    /**
     * 获取字符串的首字母（如果是中文则取拼音首字母）
     */
    fun getFirstLetter(text: String): Char {
        if (text.isEmpty()) return '#'
        val firstChar = text[0]
        if (Pinyin.isChinese(firstChar)) {
            val pinyin = Pinyin.toPinyin(firstChar)
            return pinyin.firstOrNull()?.uppercaseChar() ?: '#'
        }
        return if (firstChar.isLetter()) firstChar.uppercaseChar() else '#'
    }
}
