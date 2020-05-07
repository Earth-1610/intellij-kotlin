package com.itangcent.intellij.tip

class SimpleTip(private val content: String) : Tip {
    override fun tipAble(): Boolean {
        return true
    }

    override fun content(): String {
        return content
    }
}