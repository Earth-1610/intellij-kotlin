package com.itangcent.intellij.jvm.scala.adaptor

import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScTypeParam
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.api.TypeParameterType

/**
 * read only
 */
open class ScalaPsiTypeAdaptor :
    PsiType {
    private val scType: ScType

    private constructor(scType: ScType) : super(emptyTypeAnnotationProvider) {
        this.scType = scType
    }

    constructor(scType: ScType, annotationProvider: TypeAnnotationProvider) : super(annotationProvider) {
        this.scType = scType
    }

    override fun equalsToText(text: String): Boolean {
        return scType.canonicalText() == text
    }

    override fun getResolveScope(): GlobalSearchScope? {
        return null
    }

    override fun getSuperTypes(): Array<PsiType> {
        return emptyArray()
    }

    override fun <A : Any?> accept(visitor: PsiTypeVisitor<A>): A {
        throw UnsupportedOperationException()
    }

    override fun getCanonicalText(): String {
        return scType.canonicalText()
    }

    override fun getPresentableText(): String {
        return scType.presentableText()
    }

    override fun isValid(): Boolean {
        return true
    }

    companion object {
        val emptyTypeAnnotationProvider = TypeAnnotationProvider.Static.create(emptyArray())

        fun build(scType: ScType): PsiType {
            return build(scType, emptyTypeAnnotationProvider)
        }

        fun build(scType: ScType, annotationProvider: TypeAnnotationProvider): PsiType {
            if (scType is ScTypeParam) {
                return ScalaPsiTypeParameterAdaptor(scType, scType, annotationProvider)
            } else if (scType is TypeParameterType) {
                return ScalaTypeParameterType2PsiTypeParameterAdaptor(scType, scType, annotationProvider)
            } else {
                return ScalaPsiTypeAdaptor(scType, annotationProvider)
            }
        }
    }
}

class ScalaPsiTypeParameterAdaptor(
    private val scTypeParam: ScTypeParam,
    private var scType: ScType,
    annotations: TypeAnnotationProvider
) :
    PsiType(annotations), PsiTypeParameter by scTypeParam {

    /**
     * @return annotations for this type. Uses [.getAnnotationProvider] to retrieve the annotations.
     */
    override fun getAnnotations(): Array<PsiAnnotation> {
        return super<PsiType>.getAnnotations()
    }

    override fun equalsToText(text: String): Boolean {
        return scType.canonicalText() == text
    }

    override fun <A : Any?> accept(visitor: PsiTypeVisitor<A>): A {
        throw UnsupportedOperationException()
    }

    override fun getCanonicalText(): String {
        return scType.canonicalText()
    }

    override fun getPresentableText(): String {
        return scType.presentableText()
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun addAnnotation(qualifiedName: String): PsiAnnotation {
        return scTypeParam.addAnnotation(qualifiedName)
    }

    override fun findAnnotation(qualifiedName: String): PsiAnnotation? {
        return scTypeParam.findAnnotation(qualifiedName)
    }

    override fun getApplicableAnnotations(): Array<PsiAnnotation> {
        return scTypeParam.applicableAnnotations
    }

}

class ScalaTypeParameterType2PsiTypeParameterAdaptor(
    private val scTypeParam: TypeParameterType,
    private var scType: ScType,
    annotations: TypeAnnotationProvider
) : PsiType(annotations), PsiTypeParameter by scTypeParam.psiTypeParameter() {
    override fun <A : Any?> accept(visitor: PsiTypeVisitor<A>): A {
        throw UnsupportedOperationException()
    }

    override fun equalsToText(text: String): Boolean {
        return scType.canonicalText() == text
    }

    override fun getResolveScope(): GlobalSearchScope {
        return GlobalSearchScope.EMPTY_SCOPE
    }

    override fun getSuperTypes(): Array<PsiClassType> {
        return emptyArray()
    }

    override fun accept(visitor: PsiElementVisitor) {
        throw UnsupportedOperationException()
    }

    override fun getCanonicalText(): String {
        return scType.canonicalText()
    }

    override fun getPresentableText(): String {
        return scType.presentableText()
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun getAnnotations(): Array<PsiAnnotation> {
        return super<PsiType>.getAnnotations()
    }
}