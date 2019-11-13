package com.itangcent.intellij.config

import com.google.inject.Inject
import com.itangcent.common.utils.*
import com.itangcent.intellij.logger.Logger
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.util.regex.Pattern

abstract class AbstractConfigReader : MutableConfigReader {

    protected var configInfo: MultiValuesMap<String, String> =
        MultiValuesMap(true)

    @Inject(optional = true)
    protected val logger: Logger? = null

    private var resolveProperty: Boolean = true

    private var ignoreUnresolved: Boolean = false

    fun loadConfigInfo() {
        if (configInfo.isNotEmpty()) return

        val configFiles = findConfigFiles() ?: return
        if (configInfo.isEmpty()) {
            configFiles.forEach { path ->
                loadConfigFile(path)
            }
        }
    }

    override fun reset() {
        configInfo = MultiValuesMap(true)
    }

    override fun loadConfigInfoContent(configInfoContent: String, type: String) {
        //wow,resolve .yaml&.yml as yamlConfig
        if (type == "yml" || type == "yaml") {
            loadYamlConfig(configInfoContent)
            return
        }

        //wow,resolve .properties&.config as propertiesConfig
        if (type == "properties" || type == "config") {
            loadPropertiesConfig(configInfoContent)
            return
        }

        //default
        loadPropertiesConfig(configInfoContent)
    }

    private fun loadPropertiesConfig(configInfoContent: String) {
        for (line in configInfoContent.lines()) {
            val trimLine = line.trim()

            //ignore blank line
            if (trimLine.isBlank()) {
                continue
            }

            //resolve comment setting
            if (trimLine.startsWith("###")) {
                resolveSetting(trimLine.removePrefix("###").trim())
                continue
            }

            //ignore comment
            if (trimLine.startsWith("#")) {
                continue
            }

            //resolve name&value
            val name = resolveProperty(trimLine.substringBefore("="))
            if (!name.isBlank()) {

                val value = resolveProperty(trimLine.substringAfter("=", ""))
                if (name == "properties.additional") {
                    loadConfigFile(value)
                    continue
                }
                configInfo.put(name.trim(), value.trim())
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadYamlConfig(configInfoContent: String) {
        val yaml = Yaml()
        val yamlProperties = yaml.load(configInfoContent)

        if (yamlProperties is Map<*, *>) {
            (yamlProperties as Map<Any?, Any?>).flat { k, v ->
                val name = resolveProperty(k)
                val value = resolveProperty(v)
                if (name == "properties.additional") {
                    loadConfigFile(value)
                } else {
                    configInfo.put(name.trim(), value.trim())
                }
            }
        }
    }

    private fun loadConfigFile(path: String) {
        val configFile = File(resolvePath(path))
        if (configFile.exists() && configFile.isFile) {
            put("curr_path", path)
            val configInfoContent = FileUtils.read(configFile)
            loadConfigInfoContent(configInfoContent, path.substringAfterLast("."))
        }
    }

    private fun resolvePath(path: String): String {
        if (path.startsWith(".")) {
            val currPath = getPropertyValue("curr_path")
            if (currPath.isNullOrBlank()) return path
            return currPath!!.substringBeforeLast(File.separator) + File.separator + path.removePrefix(".")
        } else if (path.startsWith("/") || path.startsWith("~")) {
            return path
        } else {
            val currPath = getPropertyValue("curr_path")
            if (currPath.isNullOrBlank()) return path
            return currPath + File.separator + path
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
            } else if (property == "ignoreUnresolved") {
                this.ignoreUnresolved = value.toBool()
                return
            }
        } else if (setting.startsWith("if")) {
            //todo:resolve  condition
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
                    val value = getPropertyValue(key)
                    if (value == null) {
                        if (ignoreUnresolved)
                            continue
                        logger!!.error("unable to resolve $key")
                        match.appendReplacement(sb, "")
                        continue
                    }
                    match.appendReplacement(sb, value)
                } catch (e: Exception) {
                    logger!!.error("unable to resolve $key")
                }
            }
        }
        match.appendTail(sb)
        return sb.toString()
    }

    open fun getPropertyValue(key: String): String? {

        val value = configInfo.getOne(key)
        if (!value.isNullOrBlank()) {
            return value
        }

        try {
            val property = System.getProperty(key)
            return property
        } catch (e: Exception) {
        }

        return null
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