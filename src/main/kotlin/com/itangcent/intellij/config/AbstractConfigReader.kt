package com.itangcent.intellij.config

import com.itangcent.common.utils.FileUtils
import java.io.File
import java.util.regex.Pattern

abstract class AbstractConfigReader : ConfigReader {

    //维持key的顺序
    var keys: ArrayList<String> = ArrayList()
    var configInfo: HashMap<String, String> = HashMap()

    fun loadConfigInfo(): Map<String, String> {
        if (configInfo.isNotEmpty()) return configInfo

        val configFiles = findConfigFiles() ?: return configInfo
        if (configInfo.isEmpty()) {
            configFiles.forEach { path ->
                val configFile = File(path)
                if (configFile.exists() && configFile.isFile) {
                    val configInfoContent = FileUtils.read(configFile)
                    for (line in configInfoContent.lines()) {
                        if (line.isBlank() || line.startsWith("#")) continue
                        val name = resolveProperty(line.substringBefore("="))
                        if (name.isBlank()) continue
                        val value = resolveProperty(line.substringAfter("=", ""))
                        if (!configInfo.containsKey(name)) {
                            keys.add(name)
                            configInfo[name] = value
                        }
                    }
                }
            }
        }
        return configInfo
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
            val value = configInfo[key] ?: ""
            match.appendReplacement(sb, value)
        }
        match.appendTail(sb)
        return sb.toString()
    }

    override fun readConfigInfo(): Map<String, String> {
        return configInfo
    }

    override fun read(key: String): String? {
        return configInfo[key]
    }

    override fun foreach(action: (String, String) -> Unit) {
        keys.forEach { key -> action(key, configInfo[key]!!) }
    }

    override fun foreach(keyFilter: (String) -> Boolean, action: (String, String) -> Unit) {
        keys.filter(keyFilter).forEach { key -> action(key, configInfo[key]!!) }
    }

}