package com.itangcent.intellij.config

import com.google.inject.Inject
import com.itangcent.common.logger.Log
import com.itangcent.common.text.TemplateUtils
import com.itangcent.common.utils.*
import com.itangcent.intellij.config.resource.FileResource
import com.itangcent.intellij.config.resource.ResourceResolver
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.tip.OnlyOnceInContextTip
import com.itangcent.intellij.tip.TipsHelper
import org.yaml.snakeyaml.Yaml
import java.util.regex.Pattern

abstract class BaseConfigReader : MutableConfigReader {

    protected var configInfo: MultiValuesMap<String, String> =
        MultiValuesMap()

    @Inject
    protected lateinit var logger: Logger

    @Inject
    protected lateinit var tipsHelper: TipsHelper

    @Inject
    protected lateinit var resourceResolver: ResourceResolver

    private var resolveProperty: Boolean = true

    private var ignoreUnresolved: Boolean = false

    private var ignoreNotFoundFile: Boolean = false

    private var resolveMulti: ResolveMultiType = ResolveMultiType.FIRST

    override fun reset() {
        configInfo = MultiValuesMap()
    }

    override fun loadConfigInfoContent(configInfoContent: String, type: String) {
        val configResolver: ConfigResolver = when (type) {
            "yml", "yaml" -> {//resolve .yaml&.yml as yamlConfig
                YamlConfigResolver()
            }

            "json" -> {
                //resolve .json as jsonConfig
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
            LOG.info("read config file:$resource")
            val content = resource.content
            if (content.isNullOrBlank()) {
                if (!ignoreNotFoundFile) {
                    if (content == null) {
                        logger.debug("$path not be found.")
                    } else if (content.isBlank()) {
                        logger.debug("$path is an empty file")
                    }
                }
                return
            }
            configInfo.put("_configs", resource.url.toString())
            if (resource is FileResource) {
                val prePath = configInfo.getFirst("_curr_path")
                try {
                    resource.asFile()?.parent?.let { configInfo.replace("_curr_path", it) }
                    loadConfigInfoContent(content, path.substringAfterLast("."))
                } finally {
                    if (prePath == null) {
                        configInfo.removeAll("_curr_path")
                    } else {
                        configInfo.replace("_curr_path", prePath)
                    }
                }
            } else {
                loadConfigInfoContent(content, path.substringAfterLast("."))
            }
        } catch (e: Exception) {
            LOG.warn("Error reading config file:$path")
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
            when (property) {
                "resolveProperty" -> {
                    this.resolveProperty = value.toBool()
                    return
                }

                "ignoreUnresolved" -> {
                    this.ignoreUnresolved = value.toBool()
                    return
                }

                "ignoreNotFoundFile" -> {
                    this.ignoreNotFoundFile = value.toBool()
                    return
                }

                "resolveMulti" -> {
                    this.resolveMulti = ResolveMultiType.valueOf(value.uppercase())
                    return
                }
            }
        } else if (setting.startsWith("if")) {
            //todo:resolve  condition
        }
        logger.warn("unknown comment setting:$setting")

    }

    override fun resolveProperty(property: String): String {
        if (!resolveProperty) {
            return property
        }
        return TemplateUtils.render(property)
            .context { p ->
                getPropertyValue(p)?.takeIf { it != MULTI_UNRESOLVED }
            }
            .onEval { p, resolved ->
                if (resolved == null && !ignoreUnresolved) {
                    logger.error("unable to resolve property:$p")
                    tipsHelper.showTips(UNRESOLVED_TIP)
                }
            }.render() ?: ""
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
        } catch (_: Exception) {
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
        configInfo.flattenForEach(keyFilter) { key, value ->
            if (keyFilter(key)) {
                action(key, value)
            }
        }
    }

    companion object : Log() {
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

        override fun parseAsMaps(content: String, handle: (Map<*, *>) -> Unit) {
            val yaml = Yaml()
            try {
                val yamlProperties = yaml.loadAll(content)
                resolveProperties(yamlProperties, handle)
            } catch (e: Exception) {
                val yamlProperties = yaml.loadAll(content.replace(Regex("@(.*?)@")) {
                    "\${${it.groupValues[0]}}"
                })
                resolveProperties(yamlProperties, handle)
            }
        }

        private fun resolveProperties(
            yamlProperties: Iterable<Any>,
            handle: (Map<*, *>) -> Unit
        ) {
            for (yamlProperty in yamlProperties) {
                if (yamlProperty is Map<*, *>) {
                    handle(yamlProperty)
                }
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
            if (name.isNotBlank()) {
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