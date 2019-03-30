package com.itangcent.intellij.setting

import com.itangcent.common.utils.CollectionUtils
import java.util.*
import java.util.regex.Pattern

class ReadOnlySettingManager : DefaultSettingManager() {

    var resolvedSettings: ArrayList<TokenSetting>? = null

    override fun init() {
        super.init()
        if (super.tokenSettings == null) {
            return
        }

        val unresolvedSettings: ArrayList<TokenSetting> = ArrayList(super.tokenSettings!!.size)
        unresolvedSettings.addAll(super.tokenSettings!!)

        val resolvedSettings: ArrayList<TokenSetting> = ArrayList(super.tokenSettings!!.size)
        val resolvedMap: HashMap<String, String> = HashMap()

        var times = 0
        while (times++ < 10) {
            val iterator = unresolvedSettings.iterator()
            while (iterator.hasNext()) {
                val unresolvedSetting = iterator.next()
                try {
                    unresolvedSetting.host = resolveProperty(unresolvedSetting.host, resolvedMap)
                    unresolvedSetting.privateToken = resolveProperty(unresolvedSetting.privateToken, resolvedMap)
                    iterator.remove()
                    resolvedSettings.add(unresolvedSetting)
                    if (unresolvedSetting.host != null) {
                        resolvedMap[unresolvedSetting.host!!] = unresolvedSetting.privateToken ?: ""
                    }
                } catch (e: Exception) {
                }
            }
        }

        this.resolvedSettings = resolvedSettings
    }

    override fun getSetting(host: String?): TokenSetting? {
        if (resolvedSettings == null) {
            init()
        }
        return if (CollectionUtils.isEmpty(resolvedSettings)) {
            null
        } else resolvedSettings!!
            .stream()
            .filter { tokenSetting -> tokenSetting.host == host }
            .findAny()
            .orElse(null)
    }

    private fun resolveProperty(property: String?, resolvedMap: HashMap<String, String>): String? {
        if (property.isNullOrBlank()) return property
        if (!property!!.contains("$")) return property

        val pattern = Pattern.compile("\\$\\{(.*?)}")
        val match = pattern.matcher(property)
        val sb = StringBuffer()
        while (match.find()) {
            val key = match.group(1)
            val value = resolvedMap[key] ?: throw IllegalArgumentException("unresolved key:$key")
            match.appendReplacement(sb, value)
        }
        match.appendTail(sb)
        return sb.toString()
    }

    override val tokenSettings: Array<TokenSetting>?
        get() = throw UnsupportedOperationException("read only!")

    override fun saveSetting(tokenSetting: TokenSetting) {
        throw UnsupportedOperationException("read only!")
    }
}
