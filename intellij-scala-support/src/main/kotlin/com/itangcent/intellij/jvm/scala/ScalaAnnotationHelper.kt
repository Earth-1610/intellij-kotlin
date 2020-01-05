package com.itangcent.intellij.jvm.scala

import com.intellij.psi.PsiAnnotationOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.itangcent.common.utils.GsonUtils
import com.itangcent.common.utils.cast
import com.itangcent.intellij.jvm.AnnotationHelper
import com.itangcent.intellij.jvm.scala.compatible.*
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
                    } else if (ScCompatibleLiteral.isInstance(annotationParameter)) {
                        map["value"] = ScCompatibleLiteral.getValue(annotationParameter) ?:
                                ScCompatibleLiteral.getText(annotationParameter)
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
                    } else if (ScCompatibleLiteral.isInstance(annotationParameter)) {
                        if (!attrs.contains("value")) {
                            return ScCompatibleLiteral.getValue(annotationParameter)
                                ?: ScCompatibleLiteral.getText(annotationParameter)
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

        var annotation = (psiElement.cast(PsiAnnotationOwner::class)?.annotations
            ?: psiElement.cast(PsiModifierListOwner::class)?.annotations)
            ?.firstOrNull { it.qualifiedName == annName }

        if (annotation != null && ScCompatibleAnnotation.isInstance(annotation)) {
            return ScCompatibleAnnotation.annotationExpr(annotation)
        }

        if (ScCompatibleAnnotationsHolder.isInstance(psiElement)) {
            annotation = ScCompatibleAnnotationsHolder.findAnnotation(psiElement, annName)

            if (annotation != null && ScCompatibleAnnotation.isInstance(annotation)) {
                return ScCompatibleAnnotation.annotationExpr(annotation)
            }

            for (ann in ScCompatibleAnnotationsHolder.annotations(psiElement)) {
                if (ann.qualifiedName == annName && ScCompatibleAnnotation.isInstance(ann)) {
                    return ScCompatibleAnnotation.annotationExpr(ann)
                }
            }
        }

        return null
    }
}