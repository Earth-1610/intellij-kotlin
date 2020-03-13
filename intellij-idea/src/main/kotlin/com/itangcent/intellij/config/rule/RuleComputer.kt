package com.itangcent.intellij.config.rule

import com.google.inject.ImplementedBy
import com.intellij.psi.PsiElement
import com.itangcent.intellij.jvm.element.ExplicitElement


@ImplementedBy(DefaultRuleComputer::class)
interface RuleComputer {

    fun <T> computer(ruleKey: RuleKey<T>, element: PsiElement): T? {
        return computer(ruleKey, element, element)
    }

    fun <T> computer(ruleKey: RuleKey<T>, target: Any, context: PsiElement?): T?
}

fun <T> RuleComputer.computer(ruleKey: RuleKey<T>, explicitElement: ExplicitElement<*>): T? {
    return this.computer(ruleKey, explicitElement, explicitElement.psi())
}

fun RuleParser.contextOf(context: ExplicitElement<*>): RuleContext {
    return this.contextOf(context, context.psi())
}