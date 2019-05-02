package com.itangcent.intellij.setting

/**
 * host and token
 */
class TokenSetting {

    constructor(privateToken: String?, host: String?) {
        this.privateToken = privateToken
        this.host = host
    }

    constructor()

    var privateToken: String? = null

    // host of git-
    var host: String? = null
}
