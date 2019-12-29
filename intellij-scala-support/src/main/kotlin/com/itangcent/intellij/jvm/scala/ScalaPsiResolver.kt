package com.itangcent.intellij.jvm.scala

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.itangcent.common.utils.KV
import com.itangcent.common.utils.toInt
import com.itangcent.intellij.jvm.scala.adaptor.ScPatternDefinitionPsiFieldAdaptor
import com.itangcent.intellij.jvm.standard.StandardPsiResolver
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall


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

    override fun resolveEnumFields(index: Int, psiField: PsiField): Map<String, Any?>? {

        if (!ScPsiUtils.isScPsiInst(psiField)) {
            throw NotImplementedError()
        }

        if (psiField is ScPatternDefinitionPsiFieldAdaptor) {
            val expr = psiField.expr()
            if (expr is ScMethodCall) {
                if (expr.invokedExpr.text == "Value") {
                    val attrOfField = docHelper!!.getAttrOfField(psiField)
                    val args = expr.args()
                    val exprs = args.exprs()

                    if (exprs.size() == 2) {
                        var id: Int = index
                        var name: String? = null
                        var index = 0
                        for (ex in exprs) {
                            val value = ScPsiUtils.valueOf(ex)
                            if (index == 0) {
                                id = value.toInt() ?: 0
                            } else {
                                name = value.toString()
                                break
                            }
                            index++
                        }

                        return KV.create<String, Any?>()
                            .set("name", name ?: psiField.name)
                            .set("desc", attrOfField)
                            .set(
                                "params", KV.create<String, Any>()
                                    .set("id", id)
                                    .set("name", name ?: psiField.name)
                            )
                    } else if (exprs.size() == 1) {
                        val head = exprs.head()
                        val value: Any? = ScPsiUtils.valueOf(head) ?: return null
                        if (value is Int) {
                            return KV.create<String, Any?>()
                                .set("name", psiField.name)
                                .set("desc", attrOfField)
                                .set(
                                    "params", KV.create<String, Any>()
                                        .set("id", value)
                                        .set("name", psiField.name)
                                )
                        } else if (value is String) {
                            return KV.create<String, Any?>()
                                .set("name", psiField.name)
                                .set("desc", attrOfField)
                                .set(
                                    "params", KV.create<String, Any>()
                                        .set("id", index)
                                        .set("name", value)
                                )
                        }
                    }
                }
            }

        }

        return null
    }

}