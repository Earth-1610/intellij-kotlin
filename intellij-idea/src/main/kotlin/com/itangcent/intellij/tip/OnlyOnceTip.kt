package com.itangcent.intellij.tip

class OnlyOnceTip(private val content: String) : Tip {
    var hasShow: Boolean = false

    override fun tipAble(): Boolean {
        return when {
            hasShow -> false
            else -> {
                hasShow = true
                true
            }
        }
    }

    override fun content(): String {
        return content
    }
}