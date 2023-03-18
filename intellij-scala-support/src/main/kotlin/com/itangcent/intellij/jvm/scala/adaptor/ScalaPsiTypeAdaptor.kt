package com.itangcent.intellij.jvm.scala.adaptor

import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.itangcent.common.utils.invokeMethod
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScTypeParam
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.api.TypeParameterType
import javax.swing.Icon

/**
 * read only
 */
open class ScalaPsiTypeAdaptor :
    ScAdaptor<ScType>,
    PsiType {

    private val scType: ScType

    private constructor(scType: ScType) : super(emptyTypeAnnotationProvider) {
        this.scType = scType
    }

    constructor(scType: ScType, annotationProvider: TypeAnnotationProvider) : super(annotationProvider) {
        this.scType = scType
    }

    override fun adaptor(): ScType {
        return scType
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
        return try {
            scType.invokeMethod("presentableText") as? String? ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScalaPsiTypeAdaptor

        if (scType != other.scType) return false

        return true
    }

    override fun hashCode(): Int {
        return scType.hashCode()
    }


    companion object {
        val emptyTypeAnnotationProvider = TypeAnnotationProvider.Static.create(emptyArray())

        fun build(scType: ScType): PsiType {
            return build(scType, emptyTypeAnnotationProvider)
        }

        fun build(scType: ScType, annotationProvider: TypeAnnotationProvider): PsiType {
            return if (scType is ScTypeParam) {
                ScalaPsiTypeParameterAdaptor(scType, scType, annotationProvider)
            } else if (scType is TypeParameterType) {
                ScalaTypeParameterType2PsiTypeParameterAdaptor(
                    TypeParameterTypeWrapper(scType),
                    scType,
                    annotationProvider
                )
            } else {
                ScalaPsiTypeAdaptor(scType, annotationProvider)
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
        return try {
            scType.invokeMethod("presentableText") as? String? ?: ""
        } catch (e: Exception) {
            ""
        }
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
    private val scTypeParam: TypeParameterTypeWrapper,
    private var scType: ScType,
    annotations: TypeAnnotationProvider
) : PsiType(annotations), PsiTypeParameter by scTypeParam.getTypeParameterType() {
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
        return try {
            scType.invokeMethod("presentableText") as? String? ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun getAnnotations(): Array<PsiAnnotation> {
        return super<PsiType>.getAnnotations()
    }

    fun getTypeParameterType(): PsiTypeParameter? {
        return scTypeParam.getTypeParameterType()
    }
}

class TypeParameterTypeWrapper(private val scTypeParam: TypeParameterType) {

    fun getTypeParameterType(): PsiTypeParameter {
        return scTypeParam.invokeMethod("psiTypeParameter") as? PsiTypeParameter ?: fakePsiTypeParameter
    }

    companion object {
        private val fakePsiTypeParameter = object : PsiTypeParameter {
            override fun hasModifierProperty(name: String): Boolean {
                return false
            }

            override fun getInnerClasses(): Array<PsiClass> {
                return emptyArray()
            }

            override fun findAnnotation(qualifiedName: String): PsiAnnotation? {
                return null
            }

            override fun findMethodBySignature(patternMethod: PsiMethod?, checkBases: Boolean): PsiMethod? {
                return null
            }

            override fun checkAdd(element: PsiElement) {
            }

            override fun getLanguage(): Language {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getUseScope(): SearchScope {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun findInnerClassByName(name: String?, checkBases: Boolean): PsiClass? {
                return null
            }

            override fun getExtendsListTypes(): Array<PsiClassType> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getProject(): Project {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun addRange(first: PsiElement?, last: PsiElement?): PsiElement {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getTypeParameterList(): PsiTypeParameterList? {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun isAnnotationType(): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun addAfter(element: PsiElement, anchor: PsiElement?): PsiElement {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun accept(visitor: PsiElementVisitor) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getTextRange(): TextRange {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun <T : Any?> putCopyableUserData(key: Key<T>, value: T?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getNameIdentifier(): PsiIdentifier? {
                return null
            }

            override fun getOriginalElement(): PsiElement {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun checkDelete() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getPresentation(): ItemPresentation? {
                return null
            }

            override fun getFields(): Array<PsiField> {
                return emptyArray()
            }

            override fun getSuperClass(): PsiClass? {
                return null
            }

            override fun getSupers(): Array<PsiClass> {
                return emptyArray()
            }

            override fun add(element: PsiElement): PsiElement {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun addRangeBefore(first: PsiElement, last: PsiElement, anchor: PsiElement?): PsiElement {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun isPhysical(): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getQualifiedName(): String? {
                return null
            }

            override fun findReferenceAt(offset: Int): PsiReference? {
                return null
            }

            override fun getNode(): ASTNode {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun findMethodsAndTheirSubstitutorsByName(
                name: String?,
                checkBases: Boolean
            ): MutableList<Pair<PsiMethod, PsiSubstitutor>> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getImplementsList(): PsiReferenceList? {
                return null
            }

            override fun getManager(): PsiManager {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun isValid(): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getIcon(flags: Int): Icon {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun deleteChildRange(first: PsiElement?, last: PsiElement?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getSuperTypes(): Array<PsiClassType> {
                return emptyArray()
            }

            override fun acceptChildren(visitor: PsiElementVisitor) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getMethods(): Array<PsiMethod> {
                return emptyArray()
            }

            override fun isWritable(): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getRBrace(): PsiElement? {
                return null
            }

            override fun navigate(requestFocus: Boolean) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getLastChild(): PsiElement {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun <T : Any?> getCopyableUserData(key: Key<T>): T? {
                return null
            }

            override fun getApplicableAnnotations(): Array<PsiAnnotation> {
                return emptyArray()
            }

            override fun getLBrace(): PsiElement? {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun canNavigate(): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getInitializers(): Array<PsiClassInitializer> {
                return emptyArray()
            }

            override fun getContainingClass(): PsiClass? {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun isEquivalentTo(another: PsiElement?): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun canNavigateToSource(): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun addBefore(element: PsiElement, anchor: PsiElement?): PsiElement {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun copy(): PsiElement {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getText(): String {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun isInheritorDeep(baseClass: PsiClass?, classToByPass: PsiClass?): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getAllMethodsAndTheirSubstitutors(): MutableList<Pair<PsiMethod, PsiSubstitutor>> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getStartOffsetInParent(): Int {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getPrevSibling(): PsiElement {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun isInterface(): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getTypeParameters(): Array<PsiTypeParameter> {
                return emptyArray()
            }

            override fun replace(newElement: PsiElement): PsiElement {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getInterfaces(): Array<PsiClass> {
                return emptyArray()
            }

            override fun getContainingFile(): PsiFile {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getReferences(): Array<PsiReference> {
                return emptyArray()
            }

            override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
                return false
            }

            override fun addRangeAfter(first: PsiElement?, last: PsiElement?, anchor: PsiElement?): PsiElement {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun findFieldByName(name: String?, checkBases: Boolean): PsiField? {
                return null
            }

            override fun getResolveScope(): GlobalSearchScope {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getAllFields(): Array<PsiField> {
                return emptyArray()
            }

            override fun hasTypeParameters(): Boolean {
                return false
            }

            override fun getAllInnerClasses(): Array<PsiClass> {
                return emptyArray()
            }

            override fun getContext(): PsiElement? {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getExtendsList(): PsiReferenceList {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun processDeclarations(
                processor: PsiScopeProcessor,
                state: ResolveState,
                lastParent: PsiElement?,
                place: PsiElement
            ): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getNextSibling(): PsiElement {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getFirstChild(): PsiElement {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getVisibleSignatures(): MutableCollection<HierarchicalMethodSignature> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun isEnum(): Boolean {
                return false
            }

            override fun getNavigationElement(): PsiElement {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun findMethodsByName(name: String?, checkBases: Boolean): Array<PsiMethod> {
                return emptyArray()
            }

            override fun findElementAt(offset: Int): PsiElement? {
                return null
            }

            override fun getReference(): PsiReference? {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getName(): String? {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getTextLength(): Int {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getDocComment(): PsiDocComment? {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun textMatches(text: CharSequence): Boolean {
                return false
            }

            override fun textMatches(element: PsiElement): Boolean {
                return false
            }

            override fun getTextOffset(): Int {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getAllMethods(): Array<PsiMethod> {
                return emptyArray()
            }

            override fun textToCharArray(): CharArray {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getModifierList(): PsiModifierList? {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getScope(): PsiElement {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun delete() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getOwner(): PsiTypeParameterListOwner? {
                return null
            }

            override fun getParent(): PsiElement {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun addAnnotation(qualifiedName: String): PsiAnnotation {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getChildren(): Array<PsiElement> {
                return emptyArray()
            }

            override fun getIndex(): Int {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getImplementsListTypes(): Array<PsiClassType> {
                return emptyArray()
            }

            override fun getConstructors(): Array<PsiMethod> {
                return emptyArray()
            }

            override fun isDeprecated(): Boolean {
                return false
            }

            override fun <T : Any?> getUserData(key: Key<T>): T? {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun setName(name: String): PsiElement {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun textContains(c: Char): Boolean {
                return false
            }

            override fun findMethodsBySignature(patternMethod: PsiMethod?, checkBases: Boolean): Array<PsiMethod> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }
    }
}
