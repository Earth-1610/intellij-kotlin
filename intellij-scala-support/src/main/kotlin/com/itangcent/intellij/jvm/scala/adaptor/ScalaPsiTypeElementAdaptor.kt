package com.itangcent.intellij.jvm.scala.adaptor

import com.intellij.psi.*
import com.itangcent.intellij.jvm.scala.compatible.ScCompatibleAnnotationsHolder
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement

/**
 * read only
 */
class ScalaPsiTypeElementAdaptor(private val scTypeElement: ScTypeElement) : ScAdaptor<ScTypeElement>, PsiTypeElement,
    PsiElement by scTypeElement {
    override fun adaptor(): ScTypeElement {
        return scTypeElement
    }

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
            if (ScCompatibleAnnotationsHolder.isInstance(scTypeElement)) {
                val annotations = ScCompatibleAnnotationsHolder.annotations(scTypeElement)
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScalaPsiTypeElementAdaptor

        if (scTypeElement != other.scTypeElement) return false

        return true
    }

    override fun hashCode(): Int {
        return scTypeElement.hashCode()
    }

}