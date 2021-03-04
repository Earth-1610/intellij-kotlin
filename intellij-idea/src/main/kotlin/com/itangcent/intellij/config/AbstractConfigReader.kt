package com.itangcent.intellij.config

import com.google.inject.Inject
import com.itangcent.common.logger.traceWarn
import com.itangcent.common.utils.*
import com.itangcent.intellij.config.resource.ResourceResolver
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.tip.OnlyOnceInContextTip
import com.itangcent.intellij.tip.TipsHelper
import org.yaml.snakeyaml.Yaml
import java.util.regex.Pattern

abstract class AbstractConfigReader : MutableConfigReader {

    protected var configInfo: MultiValuesMap<String, String> =
        MultiValuesMap(true)

    @Inject
    protected lateinit var logger: Logger

    @Inject
    protected lateinit var tipsHelper: TipsHelper

    @Inject
    protected lateinit var resourceResolver: ResourceResolver

    private var resolveProperty: Boolean = true

    private var ignoreUnresolved: Boolean = false

    private var ignoreNotFoundFile: Boolean = false

    private var resolveMulti: ResolveMultiType = ResolveMultiType.ERROR

    fun loadConfigInfo() {
//        if (configInfo.isNotEmpty()) return

        val configFiles = findConfigFiles() ?: return
        configFiles.forEach { path ->
            loadConfigFile(path)
        }
    }

    override fun reset() {
        configInfo = MultiValuesMap(true)
    }

    override fun loadConfigInfoContent(configInfoContent: String, type: String) {
        val configResolver: ConfigResolver = when (type) {
            "yml", "yaml" -> {//resolve .yaml&.yml as yamlConfig
                YamlConfigResolver()
            }
            "json" -> {
                //resolve .properties&.config as propertiesConfig
                JsonConfigResolver()
            }
            "properties", "config" -> {
                //resolve .properties&.config as propertiesConfig
                PropertiesConfigResolver()
            }
            else -> {
                PropertiesConfigResolver()
            }
        }
        configResolver.resolveConfig(configInfoContent) { k, v ->
            configInfo.put(k, v)
        }
    }

    protected open fun loadConfigFile(path: String) {
        try {
            val resource = resourceResolver.resolve(path)
            LOG.info("load config:$resource")
            val content = resource.content
            if (content.isNullOrBlank()) {
                if (!ignoreNotFoundFile) {
                    if (content == null) {
                        logger.debug("$path not be found.")
                    } else if (content.isBlank()) {
                        logger.debug("$path is an empty file")
                    }
                    return
                }
            }
            loadConfigInfoContent(content!!, path.substringAfterLast("."))
        } catch (e: Exception) {
            LOG.error("failed load config:$path")
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
            } else if (property == "ignoreNotFoundFile") {
                this.ignoreNotFoundFile = value.toBool()
                return
            } else if (property == "resolveMulti") {
                this.resolveMulti = ResolveMultiType.valueOf(value.toUpperCase())
                return
            }
        } else if (setting.startsWith("if")) {
            //todo:resolve  condition
        }
        logger.warn("unknown comment setting:$setting")

    }

    protected abstract fun findConfigFiles(): List<String>?

    override fun resolveProperty(property: String): String {
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
                    if (value == null || value == MULTI_UNRESOLVED) {
                        if (!ignoreUnresolved && value != MULTI_UNRESOLVED) {
                            logger.error("unable to resolve property:$key")
                            tipsHelper.showTips(UNRESOLVED_TIP)
                        }
                        match.appendReplacement(sb, "")
                        continue
                    }
                    match.appendReplacement(sb, value.toString())
                } catch (e: Exception) {
                    logger.error("unable to resolve $key")
                }
            }
        }
        match.appendTail(sb)
        return sb.toString()
    }

    open fun getPropertyStringValue(key: String): String? {
        val propertyValue = getPropertyValue(key)
        if (propertyValue != MULTI_UNRESOLVED) {
            return propertyValue?.toString()
        }
        return null
    }

    open fun getPropertyValue(key: String): Any? {

        when (this.resolveMulti) {
            ResolveMultiType.ERROR -> {
                try {
                    val value = configInfo.getOne(key)
                    if (!value.isNullOrBlank()) {
                        return value
                    }
                } catch (e: IllegalArgumentException) {
                    logger.error("error resolve property [$key] because [$key] has more than one value!")
                    tipsHelper.showTips(MULTI_UNRESOLVED_TIP)
                    return MULTI_UNRESOLVED
                }
            }
            ResolveMultiType.FIRST -> {
                val value = configInfo.getFirst(key)
                if (!value.isNullOrBlank()) {
                    return value
                }
            }
            ResolveMultiType.LAST -> {
                val value = configInfo[key]?.last()
                if (!value.isNullOrBlank()) {
                    return value
                }
            }
            ResolveMultiType.LONGEST -> {
                val value = configInfo[key]?.longest()
                if (!value.isNullOrBlank()) {
                    return value
                }
            }
            ResolveMultiType.SHORTEST -> {
                val value = configInfo[key]?.shortest()
                if (!value.isNullOrBlank()) {
                    return value
                }
            }
        }

        try {
            return System.getProperty(key)
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

    companion object {
        val MULTI_UNRESOLVED = Object()

        val UNRESOLVED_TIP = OnlyOnceInContextTip(
            "To ignore this error,surround with:\n" +
                    "###set ignoreUnresolved = true\n" +
                    "###set ignoreUnresolved = false"
        )
        val MULTI_UNRESOLVED_TIP = OnlyOnceInContextTip(
            "To resolve this error,please check configs\n" +
                    "Or change the resolveMultiType from default `ERROR` to [`FIRST`,`LAST`,`LONGEST`,`SHORTEST`]\n" +
                    "For Instance:###set resolveMulti = FIRST"
        )

        val KEY_FILTER_EQUAL_VALUE: Pattern =
            Pattern.compile("(\\S*?\\[.*?])\\s*=\\s*(.*?)", Pattern.MULTILINE.or(Pattern.DOTALL))!!

    }

    private abstract inner class MapConfigResolver : ConfigResolver {

        @Suppress("UNCHECKED_CAST")
        override fun resolveConfig(content: String, kvHandle: (String, String) -> Unit) {
            parseAsMaps(content) { loadConfigInMap(it) }
        }

        protected abstract fun parseAsMaps(content: String, handle: (Map<*, *>) -> Unit)

        @Suppress("UNCHECKED_CAST")
        private fun loadConfigInMap(map: Map<*, *>) {
            (map as Map<Any?, Any?>).flat { k, v ->
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

    private inner class JsonConfigResolver : MapConfigResolver() {
        override fun parseAsMaps(content: String, handle: (Map<*, *>) -> Unit) {
            handle(GsonUtils.fromJson(content, HashMap::class))
        }
    }

    private inner class YamlConfigResolver : MapConfigResolver() {

        @Suppress("UNCHECKED_CAST")
        override fun parseAsMaps(content: String, handle: (Map<*, *>) -> Unit) {
            val yaml = Yaml()
            try {
                val yamlProperties = yaml.loadAll(content)
                for (yamlProperty in yamlProperties) {
                    if (yamlProperty is Map<*, *>) {
                        handle(yamlProperty as Map<*, *>)
                    }
                }
            } catch (e: Exception) {
                logger.traceWarn("load yaml failed", e)
            }
        }
    }

    private inner class PropertiesConfigResolver : ConfigResolver {
        override fun resolveConfig(content: String, kvHandle: (String, String) -> Unit) {
            LineReader(content) { line ->
                //ignore blank line
                if (line.isBlank()) {
                    return@LineReader
                }

                //resolve comment setting
                if (line.startsWith("###")) {
                    resolveSetting(line.removePrefix("###").trim())
                    return@LineReader
                }

                //ignore comment
                if (line.startsWith("#")) {
                    return@LineReader
                }

                //resolve name&value
                parseEqualLine(line, kvHandle)
            }.lines()
        }

        private fun parseEqualLine(line: String, kvHandle: (String, String) -> Unit) {
            val name = resolveProperty(line.substringBefore("=")).trim()
            if (!name.isBlank()) {
                val value = resolveProperty(line.substringAfter("=", ""))
                if (name == "properties.additional") {
                    loadConfigFile(value)
                    return
                }
                if (name.contains("[") && value.contains("]")) {
                    val matcher = KEY_FILTER_EQUAL_VALUE.matcher(line)
                    if (matcher.matches()) {
                        val pName = matcher.group(1)
                        val pValue = matcher.group(2)
                        kvHandle(resolveProperty(pName.trim()), resolveProperty(pValue.trim()))
                        return
                    }
                    logger.warn("parse config maybe incorrect: $line")
                }
                kvHandle(name.trim(), value.trim())
            }
        }
    }
}

private interface ConfigResolver {
    fun resolveConfig(content: String, kvHandle: (String, String) -> Unit)
}

enum class ResolveMultiType {

    /**
     * Throw a exception and log it.
     */
    ERROR,

    /**
     * Select first value.
     */
    FIRST,

    /**
     * Select last value.
     */
    LAST,

    /**
     * Select longest value.
     */
    LONGEST,

    /**
     * Select shortest value.
     */
    SHORTEST
}

//background idea log
private val LOG = org.apache.log4j.Logger.getLogger(AbstractConfigReader::class.java)