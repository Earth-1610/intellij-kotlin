package com.itangcent.intellij.config.rule

typealias RuleChain<T> = Sequence<RuleExecuteNode<T>>

typealias RuleExecuteNode<T> = () -> T?