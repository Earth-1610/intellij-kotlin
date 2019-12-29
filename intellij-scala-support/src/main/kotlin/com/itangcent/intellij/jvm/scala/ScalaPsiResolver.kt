package com.itangcent.intellij.jvm.scala

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.itangcent.intellij.jvm.standard.StandardPsiResolver


/**
 *
 * see https://docs.scala-lang.org/style/scaladoc.html
 * Create links to referenced Scala Library classes using the square-bracket syntax, e.g. [[scala.Option]]
 */
open class ScalaPsiResolver : StandardPsiResolver() {

    override fun resolveClassWithPropertyOrMethod(
        classNameWithProperty: String,
        psiElement: PsiElement
    ): Pair<PsiClass?, PsiElement?>? {
        if (!ScPsiUtils.isScPsiInst(psiElement)) {
            throw NotImplementedError()
        }

        val cwp: String = if (classNameWithProperty.startsWith('[')) {
            classNameWithProperty.trim('[', ']')
        } else {
            classNameWithProperty
        }

        //[scala.xxxClass]
        var linkClass = resolveClass(cwp, psiElement)
        if (linkClass != null) {
            return linkClass to null
        }

        //[scala.xxxClass.properties]
        if (cwp.contains('.')) {
            val linkClassName = cwp.substringBeforeLast(".")
            val linkMethodOrProperty = cwp.substringAfterLast(".", "").trim()
            linkClass = resolveClass(linkClassName, psiElement) ?: return null
            return linkClass to resolvePropertyOrMethodOfClass(linkClass, linkMethodOrProperty)
        }

        //[properties]
        linkClass = getContainingClass(psiElement) ?: return null
        resolvePropertyOrMethodOfClass(linkClass, cwp)?.let {
            return linkClass to it
        }

        return null
    }
}