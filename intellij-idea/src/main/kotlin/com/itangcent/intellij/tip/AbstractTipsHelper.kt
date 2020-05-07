package com.itangcent.intellij.tip

abstract class AbstractTipsHelper : TipsHelper {
    override fun showTips(tip: Tip) {
        if (tip.tipAble()) {
            showTip(tip.content())
        }
    }

    protected abstract fun showTip(tipContent: String)
}