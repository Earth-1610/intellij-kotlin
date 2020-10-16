package com.itangcent.intellij.spi

import com.itangcent.common.spi.SetupAble
import com.itangcent.intellij.config.ConfigReaderDevEnvSupporter
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.with
import com.itangcent.intellij.jvm.ExtendProvider
import com.itangcent.intellij.jvm.SourceHelper
import com.itangcent.intellij.jvm.dev.DevEnv
import com.itangcent.intellij.jvm.spi.AutoInjectKit
import com.itangcent.intellij.psi.DefaultSourceHelper

class IdeaAutoInject : SetupAble {

    override fun init() {
        val classLoader = IdeaAutoInject::class.java.classLoader
        AutoInjectKit.tryLoadAndWrap(
            classLoader,
            ExtendProvider::class,
            "com.itangcent.intellij.config.RuleExtendProvider"
        )

        ActionContext.addDefaultInject { context ->
            context.bind(DevEnv::class) {
                it.with(ConfigReaderDevEnvSupporter::class)
            }
        }

        ActionContext.addDefaultInject { context ->
            context.bind(SourceHelper::class) {
                it.with(DefaultSourceHelper::class)
            }
        }
    }
}