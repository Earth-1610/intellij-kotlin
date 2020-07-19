package com.itangcent.intellij.config

import com.google.inject.Inject
import com.itangcent.common.logger.traceWarn
import com.itangcent.common.utils.*
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.tip.OnlyOnceInContextTip
import com.itangcent.intellij.tip.TipsHelper
import com.itangcent.intellij.util.ActionUtils
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.util.regex.Pattern

abstract class AbstractConfigReader : MutableConfigReader {

    protected var configInfo: MultiValuesMap<String, String> =
        MultiValuesMap(true)

    @Inject
    protected val logger: Logger? = null

    @Inject
    protected val tipsHelper: TipsHelper? = null

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

        LineReader(configInfoContent) { line ->
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
            parseEqualLine(line)
        }.lines()
    }

    private fun parseEqualLine(line: String) {

        val name = resolveProperty(line.substringBefore("=")).trim()
        if (!name.isBlank()) {
            val value = resolveProperty(line.substringAfter("=", ""))
            if (name == "properties.additional") {
                loadConfigFile(value)
                return
            }
            if (name.contains("[") && value.contains("]")) {
                val matcher = KEY_FILTER_EQULA_VALUE.matcher(line)
                if (matcher.matches()) {
                    val pName = matcher.group(1)
                    val pValue = matcher.group(2)
                    configInfo.put(resolveProperty(pName.trim()), resolveProperty(pValue.trim()))
                    return
                }
                logger!!.warn("parse config maybe incorrect: $line")
            }
            configInfo.put(name.trim(), value.trim())
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadYamlConfig(configInfoContent: String) {
        val yaml = Yaml()
        val yamlProperties: Iterable<Any>
        try {
            yamlProperties = yaml.loadAll(configInfoContent)

            for (yamlProperty in yamlProperties) {
                if (yamlProperty is Map<*, *>) {
                    (yamlProperty as Map<Any?, Any?>).flat { k, v ->
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
        } catch (e: Exception) {
            logger!!.traceWarn("load yaml failed", e)
            return
        }

    }

    private fun loadConfigFile(path: String) {
        val currPath = getPropertyStringValue("curr_path")
        val configFile = SysFileResolve.adaptive().resolveFile(path, currPath)
        if (configFile != null && configFile.exists() && configFile.isFile) {
            val configInfoContent = FileUtils.read(configFile)
            if (!configInfoContent.isNullOrEmpty()) {
                configInfo.replace("curr_path", configFile.absolutePath.substringBeforeLast(File.separator))
                try {
                    loadConfigInfoContent(configInfoContent, path.substringAfterLast("."))
                } finally {
                    currPath?.let { configInfo.replace("curr_path", it) }
                }
                return
            } else if (!ignoreNotFoundFile) {
                logger!!.debug("$path is an empty file")
            }
        } else if (!ignoreNotFoundFile) {
            if (configFile != null) {
                val absolutePath = configFile.absolutePath
                if (absolutePath != path) {
                    logger!!.debug("$path not be found. It be resolved as $absolutePath")
                    return
                }
            }
            logger!!.debug("$path not be found")
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
        logger?.warn("unknown comment setting:$setting")

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
                            logger!!.error("unable to resolve property:$key")
                            tipsHelper!!.showTips(UNRESOLVED_TIP)
                        }
                        match.appendReplacement(sb, "")
                        continue
                    }
                    match.appendReplacement(sb, value.toString())
                } catch (e: Exception) {
                    logger!!.error("unable to resolve $key")
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
                    logger!!.error("error resolve property [$key] because [$key] has more than one value!")
                    tipsHelper!!.showTips(MULTI_UNRESOLVED_TIP)
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

        val KEY_FILTER_EQULA_VALUE = Pattern.compile("(\\S*?\\[.*?])\\s*=\\s*(.*?)")

    }
}

private enum class SysFileResolve {
    OS_SYS_FILE_RESOLVE {

        override fun separator(): String {
            return "/"
        }

        override fun isAbsolute(path: String): Boolean {
            return path.startsWith("/")
        }

        override fun asFile(path: String): File {
            return super.asFile(path).takeIf { it.exists() }
                ?: super.asFile(path.replace('\\', '/'))
        }
    },

    WIN_SYS_FILE_RESOLVE {

        override fun separator(): String {
            return "\\"
        }

        override fun isAbsolute(path: String): Boolean {
            return Pattern.matches("[a-zA-Z]+:.*?", path)
        }

        override fun asFile(path: String): File {
            return super.asFile(path).takeIf { it.exists() }
                ?: super.asFile(path.replace('/', '\\'))
        }
    };

    abstract fun isAbsolute(path: String): Boolean

    open fun separator(): String {
        return File.separator
    }

    fun resolveFile(path: String, currPath: String?): File? {
        if (isAbsolute(path)) {
            return asFile(path)
        }

        if (path.startsWith('~')) {
            val home = SystemUtils.userHome
            return asFile(home.removeSuffix(separator()) + separator() + path.substring(1))
        }

        var p = path
        var cp = currPath ?: ""
        while (true) {
            if (p.startsWith("../")) {//goto parent directory
                cp = cp.substringBeforeLast(separator())
                p = p.substring("../".length)
            } else if (p.startsWith("./")) {//current directory
                if (cp.isBlank()) return File(p).takeIf { it.exists() }
                p = p.substring("./".length)
            }
            break
        }

        return asFile(cp.removeSuffix(separator()) + separator() + p.removePrefix(separator()))
    }

    open fun asFile(path: String): File {
        return File(path)
    }

    companion object {

        private var adaptiveSysFileResolve: SysFileResolve? = null

        fun adaptive(): SysFileResolve {
            if (adaptiveSysFileResolve == null) {
                val separator = File.separator
                adaptiveSysFileResolve = values().firstOrNull { it.separator() == separator } ?: OS_SYS_FILE_RESOLVE
            }
            return adaptiveSysFileResolve!!
        }
    }
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