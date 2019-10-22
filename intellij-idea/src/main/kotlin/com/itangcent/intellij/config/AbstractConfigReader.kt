package com.itangcent.intellij.config

import com.google.inject.Inject
import com.itangcent.common.utils.FileUtils
import com.itangcent.common.utils.toBool
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.util.MultiValuesMap
import java.io.File
import java.util.regex.Pattern

abstract class AbstractConfigReader : MutableConfigReader {

    protected var configInfo: MultiValuesMap<String, String> = MultiValuesMap(true)

    @Inject(optional = true)
    protected val logger: Logger? = null

    private var resolveProperty: Boolean = true


    fun loadConfigInfo() {
        if (configInfo.isNotEmpty()) return

        val configFiles = findConfigFiles() ?: return
        if (configInfo.isEmpty()) {
            configFiles.forEach { path ->
                val configFile = File(path)
                if (configFile.exists() && configFile.isFile) {
                    val configInfoContent = FileUtils.read(configFile)
                    loadConfigInfoContent(configInfoContent)
                }
            }
        }
    }

    override fun loadConfigInfoContent(configInfoContent: String) {
        for (line in configInfoContent.lines()) {
            val trimLine = line.trim()

            //ignore blank line
            if (trimLine.isBlank()) {
                continue
            }

            //resolve comment setting
            if (trimLine.startsWith("###")) {
                resolveSetting(trimLine.removePrefix("###").trim())
            }

            //ignore comment
            if (trimLine.startsWith("#")) {
                continue
            }

            //resolve name&value
            val name = resolveProperty(trimLine.substringBefore("="))
            if (!name.isBlank()) {
                val value = resolveProperty(trimLine.substringAfter("=", ""))
                configInfo.put(name.trim(), value.trim())
            }
        }
    }


    /**
     * todo:support more property
     */
    private fun resolveSetting(setting: String) {
        if (setting.startsWith("set")) {
            val propertyAndValue = setting.removePrefix("set").trim()
            val property = propertyAndValue.substringBefore('=').trim()
            val value = propertyAndValue.substringAfter('=', "").trim()
            if (property == "resolveProperty") {
                this.resolveProperty = value.toBool()
                return
            }
        }
        logger?.warn("unknown comment setting:$setting")

    }

    protected abstract fun findConfigFiles(): List<String>?

    private fun resolveProperty(property: String): String {
        if (!resolveProperty) {
            return property
        }

        if (property.isBlank()) return property
        if (!property.contains("$")) return property

        val pattern = Pattern.compile("\\$\\{(.*?)}")
        val match = pattern.matcher(property)
        val sb = StringBuffer()
        while (match.find()) {
            val key = match.group(1)
            if (key.startsWith('\'') && key.endsWith('\'')) {
                match.appendReplacement(sb, key.removeSurrounding("'"))
            } else if (key.startsWith('\"') && key.endsWith('\"')) {
                match.appendReplacement(sb, key.removeSurrounding("\""))
            } else {
                try {
                    val value = configInfo.getOne(key)
                    match.appendReplacement(sb, value)
                } catch (e: Exception) {
                    logger!!.error("unable to resolve $key")
                }
            }
        }
        match.appendTail(sb)
        return sb.toString()
    }

    override fun put(key: String, vararg value: String) {
        configInfo.putAll(key, *value)
    }

    override fun remove(key: String) {
        configInfo.removeAll(key)
    }

    override fun remove(key: String, value: String) {
        configInfo.remove(key, value)
    }

    override fun first(key: String): String? {
        return configInfo.getFirst(key)
    }

    override fun read(key: String): Collection<String>? {
        return configInfo[key]
    }

    override fun foreach(action: (String, String) -> Unit) {
        configInfo.flattenForEach(action)
    }

    override fun foreach(keyFilter: (String) -> Boolean, action: (String, String) -> Unit) {
        configInfo.flattenForEach(keyFilter, { key, value ->
            if (keyFilter(key)) {
                action(key, value)
            }
        })
    }

}