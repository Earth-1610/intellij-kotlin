package com.itangcent.intellij.tip

class OnlyOnceTip(private val content: String) : Tip {
    var hasShow: Boolean = false

    override fun tipable(): Boolean {
        if (hasShow) {
            return false
        }
        hasShow = true
        return true
    }

    override fun content(): String {
        return content
    }
}