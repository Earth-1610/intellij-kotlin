package com.itangcent.intellij.jvm.scala.adaptor

import com.intellij.psi.*
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScAnnotationsHolder

/**
 * read only
 */
class ScalaPsiTypeElementAdaptor(private val scTypeElement: ScTypeElement) : PsiTypeElement,
    PsiElement by scTypeElement {
    override fun findAnnotation(qualifiedName: String): PsiAnnotation? {
        return null
    }

    override fun getAnnotations(): Array<PsiAnnotation> {
        return emptyArray()
    }

    override fun getInnermostComponentReferenceElement(): PsiJavaCodeReferenceElement? {
        return null
    }

    override fun addAnnotation(qualifiedName: String): PsiAnnotation {
        throw UnsupportedOperationException()
    }

    override fun getType(): PsiType {
        val type = scTypeElement.type
        if (type.isRight) {
            val right = type.right()
            if (scTypeElement is ScAnnotationsHolder) {
                val annotations = scTypeElement.annotations()
                val toList = annotations.toList()
                val annotationList: ArrayList<PsiAnnotation> = ArrayList()
                for (scAnnotation in toList) {
                    annotationList.add(scAnnotation)
                }
                return ScalaPsiTypeAdaptor.build(
                    right.get(),
                    TypeAnnotationProvider.Static.create(annotationList.toTypedArray())
                )
            }
            return ScalaPsiTypeAdaptor.build(right.get())
        }
        throw IllegalArgumentException("error to find ")
    }

    override fun getApplicableAnnotations(): Array<PsiAnnotation> {
        return emptyArray()
    }

}