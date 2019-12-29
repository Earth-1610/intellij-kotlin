package com.itangcent.intellij.jvm.scala

import com.intellij.psi.PsiAnnotationOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.itangcent.common.utils.GsonUtils
import com.itangcent.common.utils.cast
import com.itangcent.intellij.jvm.AnnotationHelper
import org.jetbrains.plugins.scala.lang.psi.api.base.ScLiteral
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScAnnotation
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScAnnotationExpr
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScAssignStmt
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScAnnotationsHolder
import java.util.*

class ScalaAnnotationHelper : AnnotationHelper {
    override fun hasAnn(psiElement: PsiElement?, annName: String): Boolean {
        return findScAnnotation(psiElement, annName) != null
    }

    override fun findAnnMap(psiElement: PsiElement?, annName: String): Map<String, Any?>? {
        if (psiElement == null) return null

        if (ScPsiUtils.isScPsiInst(psiElement)) {
            val scAnnotation = findScAnnotation(psiElement, annName)
            if (scAnnotation != null) {
                val map: HashMap<String, Any?> = HashMap()
                val annotationParameters = scAnnotation.annotationParameters
                for (annotationParameter in annotationParameters) {

                    if (annotationParameter is ScAssignStmt) {
                        val assignName = annotationParameter.assignName()
                        val name: String? = if (assignName.isDefined) {
                            assignName.get()
                        } else {
                            ScPsiUtils.valueOf(annotationParameter.lExpression)?.toString() ?: "value"
                        }
                        val rExpression = annotationParameter.rExpression
                        if (rExpression.isDefined) {
                            map[name!!] = ScPsiUtils.valueOf(rExpression.get())
                        }
                    } else if (annotationParameter is ScLiteral) {
                        map["value"] = annotationParameter.value ?: annotationParameter.text
                    }
                }
                return map
            }
        }

        return null
    }

    override fun findAttr(psiElement: PsiElement?, annName: String): Any? {
        return findAttr(psiElement, annName, "value")
    }

    override fun findAttr(psiElement: PsiElement?, annName: String, vararg attrs: String): Any? {

        if (psiElement == null) return null

        if (ScPsiUtils.isScPsiInst(psiElement)) {
            val scAnnotation = findScAnnotation(psiElement, annName)
            if (scAnnotation != null) {
                val annotationParameters = scAnnotation.annotationParameters
                for (annotationParameter in annotationParameters) {
                    if (annotationParameter is ScAssignStmt) {
                        val assignName = annotationParameter.assignName()
                        val name: String? = if (assignName.isDefined) {
                            assignName.get()
                        } else {
                            ScPsiUtils.valueOf(annotationParameter.lExpression)?.toString() ?: "value"
                        }
                        if (!attrs.contains(name)) {
                            continue
                        }
                        val rExpression = annotationParameter.rExpression
                        if (rExpression.isDefined) {
                            return ScPsiUtils.valueOf(rExpression.get())
                        }
                    } else if (annotationParameter is ScLiteral) {
                        if (!attrs.contains("value")) {
                            return annotationParameter.value ?: annotationParameter.text
                        }
                    }
                }
                return null
            }
        }

        return null
    }

    override fun findAttrAsString(psiElement: PsiElement?, annName: String): String? {
        return findAttrAsString(psiElement, annName, "value")
    }

    override fun findAttrAsString(psiElement: PsiElement?, annName: String, vararg attrs: String): String? {
        return tinyAnnStr(findAttr(psiElement, annName, *attrs))
    }

    fun tinyAnnStr(annStr: Any?): String? {
        return when (annStr) {
            null -> null
            is Array<*> -> annStr.joinToString(separator = "\n")
            is Collection<*> -> annStr.joinToString(separator = "\n")
            is String -> annStr
            else -> GsonUtils.toJson(annStr)
        }
    }

    private fun findScAnnotation(psiElement: PsiElement?, annName: String): ScAnnotationExpr? {

        var annotation = (psiElement.cast(PsiAnnotationOwner::class)?.annotations
            ?: psiElement.cast(PsiModifierListOwner::class)?.annotations)
            ?.firstOrNull { it.qualifiedName == annName }

        if (annotation != null && annotation is ScAnnotation) {
            return annotation.annotationExpr()
        }

        if (psiElement is ScAnnotationsHolder) {
            annotation = psiElement.findAnnotation(annName)

            if (annotation != null && annotation is ScAnnotation) {
                return annotation.annotationExpr()
            }

            for (ann in psiElement.annotations()) {
                if (ann.qualifiedName == annName) {
                    return ann.annotationExpr()
                }
            }
        }

        return null
    }
}