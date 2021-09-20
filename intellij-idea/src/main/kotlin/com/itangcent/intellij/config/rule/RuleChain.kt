package com.itangcent.intellij.config.rule

interface RuleChain<T> : Iterable<RuleChain<T>> {

    fun nextChain(): RuleChain<T>?

    fun compute(): T?
}