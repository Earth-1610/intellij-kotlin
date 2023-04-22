package com.itangcent.intellij.config.rule

interface RuleChain<T> {

    fun nextChain(): RuleChain<T>?

    fun compute(): T?
}

fun <T> RuleChain<T>.asSequence(): Sequence<RuleChain<T>> {
    return generateSequence(this) {
        it.nextChain()
    }
}