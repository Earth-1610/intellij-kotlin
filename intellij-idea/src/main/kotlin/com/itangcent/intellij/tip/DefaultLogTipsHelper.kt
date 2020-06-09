package com.itangcent.intellij.tip

import com.google.inject.Inject
import com.google.inject.Singleton
import com.itangcent.intellij.logger.Logger

@Singleton
open class DefaultLogTipsHelper : AbstractTipsHelper() {

    @Inject
    private val logger: Logger? = null

    override fun showTip(tipContent: String) {
        logger?.info(tipContent)
    }
}