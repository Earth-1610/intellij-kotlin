package com.itangcent.intellij.jvm.scala

import com.intellij.psi.PsiAnnotationOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.itangcent.common.utils.GsonUtils
import com.itangcent.intellij.jvm.AnnotationHelper
import com.itangcent.intellij.jvm.scala.adaptor.ScAdaptor
import com.itangcent.intellij.jvm.scala.adaptor.tryCast
import com.itangcent.intellij.jvm.scala.compatible.ScCompatibleAnnotation
import com.itangcent.intellij.jvm.scala.compatible.ScCompatibleAnnotationExpr
import com.itangcent.intellij.jvm.scala.compatible.ScCompatibleAnnotationsHolder
import com.itangcent.intellij.jvm.scala.compatible.ScCompatibleAssignment
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
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
                val annotationParameters = ScCompatibleAnnotationExpr.getAnnotationParameters(scAnnotation)
                for (annotationParameter in annotationParameters) {
                    if (ScCompatibleAssignment.isInstance(annotationParameter)) {
                        val name: String? = ScCompatibleAssignment.referenceName(annotationParameter)
                            ?: (ScCompatibleAssignment.leftExpression(annotationParameter)?.let {
                                ScPsiUtils.valueOf(it)?.toString()
                            }
                                ?: "value")
                        val rExpression = ScCompatibleAssignment.rightExpression(annotationParameter)
                        if (rExpression != null) {
                            map[name!!] = ScPsiUtils.valueOf(rExpression)
                        }
                    } else if (annotationParameter is ScExpression) {
                        map["value"] = ScPsiUtils.valueOf(annotationParameter)
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
                val annotationParameters = ScCompatibleAnnotationExpr.getAnnotationParameters(scAnnotation)
                for (annotationParameter in annotationParameters) {
                    if (ScCompatibleAssignment.isInstance(annotationParameter)) {
                        val name: String? = ScCompatibleAssignment.referenceName(annotationParameter)
                            ?: (ScCompatibleAssignment.leftExpression(annotationParameter)?.let {
                                ScPsiUtils.valueOf(it)?.toString()
                            }
                                ?: "value")
                        if (!attrs.contains(name)) {
                            continue
                        }
                        val rExpression = ScCompatibleAssignment.rightExpression(annotationParameter)
                        if (rExpression != null) {
                            return ScPsiUtils.valueOf(rExpression)
                        }
                    } else if (annotationParameter is ScExpression) {
                        if (!attrs.contains("value")) {
                            return ScPsiUtils.valueOf(annotationParameter)
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

    /**
     * @return ScAnnotationExpr
     */
    private fun findScAnnotation(psiElement: PsiElement?, annName: String): Any? {

        if (psiElement == null) return null

        var annotation = (psiElement.tryCast(PsiAnnotationOwner::class)?.annotations
            ?: psiElement.tryCast(PsiModifierListOwner::class)?.annotations)
            ?.firstOrNull { it.qualifiedName == annName }

        if (annotation != null && ScCompatibleAnnotation.isInstance(annotation)) {
            return ScCompatibleAnnotation.annotationExpr(annotation)
        }

        val core = when (psiElement) {
            is ScAdaptor<*> -> psiElement.adaptor()!!
            else -> psiElement
        }
        if (ScCompatibleAnnotationsHolder.isInstance(core)) {
            annotation = ScCompatibleAnnotationsHolder.findAnnotation(core, annName)

            if (annotation != null && ScCompatibleAnnotation.isInstance(annotation)) {
                return ScCompatibleAnnotation.annotationExpr(annotation)
            }

            for (ann in ScCompatibleAnnotationsHolder.annotations(core)) {
                if (ann.qualifiedName == annName && ScCompatibleAnnotation.isInstance(ann)) {
                    return ScCompatibleAnnotation.annotationExpr(ann)
                }
            }
        }

        return null
    }
}