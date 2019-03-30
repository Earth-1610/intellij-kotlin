package com.itangcent.intellij.setting

import com.intellij.util.containers.isNullOrEmpty
import com.itangcent.common.utils.GsonUtils

class SettingRepository {
    internal var tokenSettings: List<TokenSetting>? = null

    companion object {
        fun fromJson(json: String): SettingRepository {
            try {
                val settingRepository = GsonUtils.fromJson(json, SettingRepository::class)
                if (!settingRepository.tokenSettings.isNullOrEmpty()) {
                    return settingRepository
                }
            } catch (e: Exception) {
            }
            val oldSettingRepository = GsonUtils.fromJson(json, OldSettingRepository::class)
            val settingRepository = SettingRepository()
            settingRepository.tokenSettings = oldSettingRepository.gitSettings
            return settingRepository
        }
    }
}

/**
 * 兼容性代码
 */
class OldSettingRepository {
    internal var gitSettings: List<TokenSetting>? = null
}
