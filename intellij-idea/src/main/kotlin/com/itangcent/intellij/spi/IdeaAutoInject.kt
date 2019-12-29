package com.itangcent.intellij.spi

import com.itangcent.common.spi.SetupAble
import com.itangcent.intellij.jvm.ExtendProvider
import com.itangcent.intellij.jvm.spi.AutoInjectKit

class IdeaAutoInject : SetupAble {

    override fun init() {
        val classLoader = IdeaAutoInject::class.java.classLoader
        AutoInjectKit.tryLoadAndWrap(
            classLoader,
            ExtendProvider::class,
            "com.itangcent.intellij.config.RuleExtendProvider"
        )
    }
}