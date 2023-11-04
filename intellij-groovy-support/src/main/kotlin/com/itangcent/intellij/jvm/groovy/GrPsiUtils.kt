package com.itangcent.intellij.jvm.groovy

object GrPsiUtils {

    /**
     * bean which class qualifiedName contains groovy may be a instance
     * of class that in groovy-plugin
     * e.g.
     * [org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression]
     * [org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement]
     */
    fun isGrPsiInst(any: Any): Boolean {
        return any::class.qualifiedName?.contains("groovy") ?: false
    }

    /**
     * Assert that the bean is an instance of class that in groovy-plugin
     */
    fun assertGrPsiInst(any: Any) {
        if (!isGrPsiInst(any)) {
            throw NotImplementedError()
        }
    }
}