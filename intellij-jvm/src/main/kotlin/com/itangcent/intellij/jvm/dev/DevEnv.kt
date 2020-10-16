package com.itangcent.intellij.jvm.dev

interface DevEnv {

    fun isDev(): Boolean

    fun dev(action: () -> Unit)

}