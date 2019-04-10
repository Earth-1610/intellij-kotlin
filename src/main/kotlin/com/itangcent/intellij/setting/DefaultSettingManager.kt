package com.itangcent.intellij.setting

import com.google.inject.Inject
import com.google.inject.name.Named
import com.itangcent.common.utils.CollectionUtils
import com.itangcent.intellij.file.FileBeanBinder
import com.itangcent.intellij.file.LocalFileRepository
import org.apache.commons.lang3.StringUtils
import java.util.*
import kotlin.streams.toList

open class DefaultSettingManager : SettingManager {

    @Inject
    private val localFileRepository: LocalFileRepository? = null

    private var settingRepository: SettingRepository? = null

    private var settingRepositoryBinder: FileBeanBinder<SettingRepository>? = null

    @Inject(optional = true)
    @Named("setting.file")
    protected val settingFileName: String = ".settings"

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
    protected open fun init() {
        if (settingRepositoryBinder == null) {
            settingRepositoryBinder = FileBeanBinder(
                localFileRepository!!.getOrCreateFile(settingFileName),
                SettingRepository::class
            )
        }
        if (settingRepository == null) {
            settingRepository = settingRepositoryBinder!!.read()

        }
    }

    private fun saveRepository(settingRepository: SettingRepository) {
        settingRepositoryBinder!!.save(settingRepository)
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
