package com.itangcent.intellij.jvm.scala.adaptor

import com.intellij.psi.PsiAnnotationOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.itangcent.common.utils.cast
import com.itangcent.intellij.jvm.AnnotationHelper
import com.itangcent.intellij.jvm.scala.ScPsiUtils
import org.jetbrains.plugins.scala.lang.psi.api.base.ScLiteral
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScAnnotation
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScAnnotationExpr
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScAssignStmt
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression

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
                    }
                }
                return map
            }
        }

        return annotationHelper.findAnnMap(psiElement, annName)
    }

    private fun valueOf(scExpr: ScExpression): Any? {
        if (scExpr is ScLiteral) {
            return scExpr.value
        }
        return scExpr.text
    }

//(method.annotations[0] as ScAnnotationImpl).annotationExpr().annotationParameters.head()


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