package com.itangcent.intellij.setting

/**
 * token 配置
 */
class TokenSetting {

    constructor(privateToken: String?, host: String?) {
        this.privateToken = privateToken
        this.host = host
    }

    constructor()

    //私钥
    var privateToken: String? = null

    // host of git-
    var host: String? = null
}
