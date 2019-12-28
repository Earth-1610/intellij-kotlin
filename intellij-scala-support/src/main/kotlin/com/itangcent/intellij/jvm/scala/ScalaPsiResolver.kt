package com.itangcent.intellij.jvm.scala

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.itangcent.intellij.jvm.PsiResolver


/**
 *
 * see https://docs.scala-lang.org/style/scaladoc.html
 * Create links to referenced Scala Library classes using the square-bracket syntax, e.g. [[scala.Option]]
 */
open class ScalaPsiResolver : PsiResolver {

    //region not implemented
    override fun resolveClass(className: String, psiElement: PsiElement): PsiClass? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun resolvePropertyOrMethodOfClass(psiClass: PsiClass, propertyOrMethod: String): PsiElement? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getContainingClass(psiElement: PsiElement): PsiClass? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun resolveRefText(psiExpression: PsiElement?): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    //endregion  not implemented

    override fun resolveClassWithPropertyOrMethod(
        classNameWithProperty: String,
        psiElement: PsiElement
    ): Pair<PsiClass?, PsiElement?>? {
        if (!ScPsiUtils.isScPsiInst(psiElement)) {
            throw NotImplementedError()
        }

        //[kotlin.reflect.KClass]
        var linkClass = resolveClass(classNameWithProperty, psiElement)
        if (linkClass != null) {
            return linkClass to null
        }

        //[kotlin.reflect.KClass.properties]
        if (classNameWithProperty.contains('.')) {
            val linkClassName = classNameWithProperty.substringBeforeLast(".")
            val linkMethodOrProperty = classNameWithProperty.substringAfterLast(".", "").trim()
            linkClass = resolveClass(linkClassName, psiElement) ?: return null
            return linkClass to resolvePropertyOrMethodOfClass(linkClass, linkMethodOrProperty)
        }

        //[properties]
        linkClass = getContainingClass(psiElement) ?: return null
        resolvePropertyOrMethodOfClass(linkClass, classNameWithProperty)?.let {
            return linkClass to it
        }
        return null
    }
}