package com.itangcent.intellij.config.resource

import com.itangcent.common.logger.Log
import com.itangcent.common.utils.SystemUtils
import com.itangcent.intellij.config.ConfigReader
import com.itangcent.intellij.context.ActionContext
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.regex.Pattern

class FileResource(val path: String) : Resource() {

    companion object : Log()

    private val currPath: String? =
        ActionContext.getContext()
            ?.instance(ConfigReader::class)
            ?.first("_curr_path")

    private var resolveFile: File? = null

    override val reachable: Boolean
        get() = asFile()?.takeIf { it.exists() && it.isFile } != null

    fun asFile(): File? {
        if (resolveFile != null) {
            return resolveFile
        }
        resolveFile = SysFileResolve.adaptive().resolveFile(path, currPath)
        LOG!!.debug("$path resolved as ${resolveFile?.path}")
        return (resolveFile as? File)?.takeIf { it.exists() }
    }

    override val url: URL
        get() {
            return File(path).toURI().toURL()
        }

    override val inputStream: InputStream?
        get() {
            return asFile()?.inputStream()
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


    companion object : Log() {

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