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
            return SettingRepository()
        }
    }
}