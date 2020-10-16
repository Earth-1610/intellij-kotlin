package com.itangcent.intellij.jvm.dev

abstract class AbstractDevEnv : DevEnv {
    override fun dev(action: () -> Unit) {
        if (isDev()) {
            action()
        }
    }
}