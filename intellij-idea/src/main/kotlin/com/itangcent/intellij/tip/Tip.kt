package com.itangcent.intellij.tip

interface Tip {
    fun tipable(): Boolean

    fun content(): String
}