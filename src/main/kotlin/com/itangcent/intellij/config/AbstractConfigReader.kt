package com.itangcent.intellij.config

import com.itangcent.common.utils.FileUtils
import com.itangcent.intellij.util.MultiValuesMap
import java.io.File
import java.util.regex.Pattern

abstract class AbstractConfigReader : MutableConfigReader {

    var configInfo: MultiValuesMap<String, String> = MultiValuesMap(true)

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
            if (line.isBlank() || line.startsWith("#")) continue
            val name = resolveProperty(line.substringBefore("="))
            if (name.isBlank()) continue
            val value = resolveProperty(line.substringAfter("=", ""))
            configInfo.put(name, value)
        }
    }

    protected abstract fun findConfigFiles(): List<String>?

    private fun resolveProperty(property: String): String {
        if (property.isBlank()) return property
        if (!property.contains("$")) return property

        val pattern = Pattern.compile("\\$\\{(.*?)}")
        val match = pattern.matcher(property)
        val sb = StringBuffer()
        while (match.find()) {
            val key = match.group(1)
            val value = try {
                configInfo.getOne(key)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("unable to resolve $key")
            }
            match.appendReplacement(sb, value)
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