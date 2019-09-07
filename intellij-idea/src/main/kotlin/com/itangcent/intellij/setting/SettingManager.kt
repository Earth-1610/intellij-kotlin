package com.itangcent.intellij.setting

interface SettingManager {

    val tokenSettings: Array<TokenSetting>?

    fun getSetting(host: String?): TokenSetting?

    fun saveSetting(tokenSetting: TokenSetting)
}
