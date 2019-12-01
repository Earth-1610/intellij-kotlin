package com.itangcent.intellij.tip

import com.google.inject.Inject
import com.google.inject.Singleton
import com.itangcent.intellij.logger.Logger

@Singleton
class DefaultTipsHelper : TipsHelper {

    @Inject
    private val logger: Logger? = null

    override fun showTips(tip: Tip) {
        if (tip.tipable()) {
            showTip(tip.content())
        }
    }

    open protected fun showTip(tipContent: String) {
        logger?.info(tipContent)
    }
}