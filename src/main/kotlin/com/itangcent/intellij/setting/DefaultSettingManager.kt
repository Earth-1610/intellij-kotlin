package com.itangcent.intellij.setting

import com.google.inject.Inject
import com.itangcent.common.utils.CollectionUtils
import com.itangcent.common.utils.GsonUtils
import com.itangcent.common.utils.SystemUtils
import com.itangcent.intellij.logger.Logger
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.*
import kotlin.streams.toList

open class DefaultSettingManager : SettingManager {

    @Inject
    private val logger: Logger? = null

    private var settingRepository: SettingRepository? = null

    private val repositoryFile: File
        @Synchronized get() {

            var home = SystemUtils.userHome
            if (home.endsWith("/")) {
                home = home.substring(0, home.length - 1)
            }
            val repositoryFile = "$home/.tm/tm.settings"
            val file = File(repositoryFile)

            if (!file.exists()) {
                try {
                    FileUtils.forceMkdirParent(file)
                    if (!file.createNewFile()) {
                        logger!!.error("error to create new setting file:$repositoryFile")
                    }
                } catch (e: Throwable) {
                    logger!!.error("error to create new setting file:$repositoryFile\n${ExceptionUtils.getStackTrace(e)}")
                    throw RuntimeException(e)
                }

            }
            return file
        }

    private val repository: SettingRepository
        get() {
            if (settingRepository == null) {
                init()
            }

            if (settingRepository == null) {
                settingRepository = SettingRepository()
                settingRepository!!.tokenSettings = ArrayList()
            }
            return settingRepository as SettingRepository
        }

    override val tokenSettings: Array<TokenSetting>?
        get() = repository.tokenSettings?.toTypedArray()

    @Synchronized
    open protected fun init() {
        if (settingRepository == null) {
            try {
                val str = FileUtils.readFileToString(repositoryFile, Charset.defaultCharset())
                settingRepository = if (str.isBlank()) {
                    SettingRepository()
                } else {
                    SettingRepository.fromJson(str)
                }
            } catch (e: Exception) {
                logger!!.error("error init settingRepository:" + ExceptionUtils.getStackTrace(e))
            }

        }
    }

    private fun saveRepository(settingRepository: SettingRepository) {
        try {
            FileUtils.write(repositoryFile, GsonUtils.toJson(settingRepository), Charset.defaultCharset())
        } catch (e: IOException) {
            logger!!.error("error save settingRepository:" + ExceptionUtils.getStackTrace(e))
        }

    }

    override fun getSetting(host: String?): TokenSetting? {
        val settingRepository = repository
        val tokenSettings = settingRepository.tokenSettings
        return if (CollectionUtils.isEmpty(tokenSettings)) {
            null
        } else tokenSettings!!
            .stream()
            .filter { gitSetting -> gitSetting.host == host }
            .findAny()
            .orElse(null)
    }

    override fun saveSetting(tokenSetting: TokenSetting) {
        if (StringUtils.isBlank(tokenSetting.host)) {
            return
        }
        val settingRepository = repository
        var tokenSettings: MutableList<TokenSetting>? = settingRepository.tokenSettings as MutableList<TokenSetting>?
        if (CollectionUtils.isEmpty(tokenSettings)) {
            tokenSettings = ArrayList()
        } else {
            tokenSettings = tokenSettings!!
                .stream()
                .filter { gs -> gs.host != tokenSetting.host }
                .toList() as MutableList<TokenSetting>?
        }
        if (StringUtils.isNotBlank(tokenSetting.privateToken)) {
            tokenSettings!!.add(tokenSetting)
        }
        settingRepository.tokenSettings = tokenSettings
        saveRepository(settingRepository)
    }
}
