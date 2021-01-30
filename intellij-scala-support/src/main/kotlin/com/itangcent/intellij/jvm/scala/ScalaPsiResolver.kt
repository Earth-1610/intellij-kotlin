package com.itangcent.intellij.jvm.scala

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.itangcent.common.utils.KV
import com.itangcent.common.utils.asInt
import com.itangcent.intellij.jvm.scala.adaptor.ScPatternDefinitionPsiFieldAdaptor
import com.itangcent.intellij.jvm.standard.StandardPsiResolver
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall


/**
 *
 * see https://docs.scala-lang.org/style/scaladoc.html
 * Create links to referenced Scala Library classes using the square-bracket syntax, e.g. [[scala.Option]]
 */
open class ScalaPsiResolver : StandardPsiResolver() {

    /**
     * ref: https://www.scala-lang.org/files/archive/spec/2.13/09-top-level-definitions.html
     */
    override fun defaultPackages(): Array<String> {
        return arrayOf("java.lang.", "java.util.", "scala.")
    }

    override fun resolveClassWithPropertyOrMethod(
        classNameWithProperty: String,
        context: PsiElement
    ): Pair<PsiClass?, PsiElement?>? {
        if (!ScPsiUtils.isScPsiInst(context)) {
            throw NotImplementedError()
        }

        val cwp: String = if (classNameWithProperty.startsWith('[')) {
            classNameWithProperty.trim('[', ']')
        } else {
            classNameWithProperty
        }

        //[scala.xxxClass]
        var linkClass = duckTypeHelper!!.resolveClass(cwp, context)
        if (linkClass != null) {
            return linkClass to null
        }

        //[scala.xxxClass.properties]
        if (cwp.contains('.')) {
            val linkClassName = cwp.substringBeforeLast(".")
            val linkMethodOrProperty = cwp.substringAfterLast(".", "").trim()
            linkClass = duckTypeHelper.resolveClass(linkClassName, context) ?: return null
            return linkClass to resolvePropertyOrMethodOfClass(linkClass, linkMethodOrProperty)
        }

        //[properties]
        linkClass = getContainingClass(context) ?: return null
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
                    val attrOfField = docHelper!!.getAttrOfField(psiField)?.trim()
                    val args = expr.args()
                    val exprs = args.exprs()

                    if (exprs.size() == 2) {
                        var id: Int = index
                        var name: String? = null
                        var i = 0
                        for (ex in exprs) {
                            val value = ScPsiUtils.valueOf(ex)
                            if (i == 0) {
                                id = value.asInt() ?: 0
                            } else {
                                name = value.toString()
                                break
                            }
                            i++
                        }

                        return KV.create<String, Any?>()
                            .set("name", name ?: psiField.name)
                            .set("ordinal", index)
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
                                .set("ordinal", index)
                                .set("desc", attrOfField)
                                .set(
                                    "params", KV.create<String, Any>()
                                        .set("id", value)
                                        .set("name", psiField.name)
                                )
                        } else if (value is String) {
                            return KV.create<String, Any?>()
                                .set("name", psiField.name)
                                .set("ordinal", index)
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