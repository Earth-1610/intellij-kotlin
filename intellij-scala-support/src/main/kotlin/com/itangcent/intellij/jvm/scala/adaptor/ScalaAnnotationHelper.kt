package com.itangcent.intellij.jvm.scala.adaptor

import com.intellij.psi.PsiAnnotationOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.itangcent.common.utils.GsonUtils
import com.itangcent.common.utils.cast
import com.itangcent.intellij.jvm.AnnotationHelper
import com.itangcent.intellij.jvm.scala.ScPsiUtils
import org.jetbrains.plugins.scala.lang.psi.api.base.ScLiteral
import org.jetbrains.plugins.scala.lang.psi.api.expr.*
import java.util.*

class ScalaAnnotationHelper(private val annotationHelper: AnnotationHelper) : AnnotationHelper by annotationHelper {

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
                            valueOf(annotationParameter.lExpression)?.toString() ?: "value"
                        }
                        val rExpression = annotationParameter.rExpression
                        if (rExpression.isDefined) {
                            map[name!!] = valueOf(rExpression.get())
                        }
                    } else if (annotationParameter is ScLiteral) {
                        map["value"] = annotationParameter.value ?: annotationParameter.text
                    }
                }
                return map
            }
        }

        return annotationHelper.findAnnMap(psiElement, annName)
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
                            valueOf(annotationParameter.lExpression)?.toString() ?: "value"
                        }
                        if (!attrs.contains(name)) {
                            continue
                        }
                        val rExpression = annotationParameter.rExpression
                        if (rExpression.isDefined) {
                            return valueOf(rExpression.get())
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

        return annotationHelper.findAnnMap(psiElement, annName)
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

    private fun valueOf(scExpr: ScExpression): Any? {
        if (scExpr is ScLiteral) {
            return scExpr.value
        }
        if (scExpr is ScMethodCall) {
            if (scExpr.invokedExpr.text == "Array") {
                val list: LinkedList<Any> = LinkedList()
                for (argumentExpression in scExpr.argumentExpressions()) {
                    valueOf(argumentExpression)?.let { list.add(it) }
                }
                return list
            }
        }
        return scExpr.text
    }

    private fun findScAnnotation(psiElement: PsiElement?, annName: String): ScAnnotationExpr? {

        val annotation = (psiElement.cast(PsiAnnotationOwner::class)?.annotations
            ?: psiElement.cast(PsiModifierListOwner::class)?.annotations)
            ?.firstOrNull { it.qualifiedName == annName } ?: return null
        if (annotation is ScAnnotation) {
            return annotation.annotationExpr()
        }

        return null
    }
}