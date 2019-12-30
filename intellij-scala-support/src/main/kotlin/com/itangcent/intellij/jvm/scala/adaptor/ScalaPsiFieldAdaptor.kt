package com.itangcent.intellij.jvm.scala.adaptor

import com.intellij.navigation.ItemPresentation
import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScVariable

/**
 * read only
 */
class ScalaPsiFieldAdaptor(private val scVariable: ScVariable) : PsiField, PsiModifierListOwner by scVariable {

    override fun setInitializer(initializer: PsiExpression?) {
        throw UnsupportedOperationException()
    }

    override fun getContainingClass(): PsiClass? {
        return scVariable.containingClass
    }

    override fun normalizeDeclaration() {
        //nothing to do
    }

    override fun getNameIdentifier(): PsiIdentifier {
        throw UnsupportedOperationException()
    }

    override fun getPresentation(): ItemPresentation? {
        return scVariable.presentation
    }

    override fun getName(): String {
        val name = scVariable.name
        if (!name.isNullOrBlank()) {
            return unescape(name)
        }
        try {
            return unescape(scVariable.declaredNames().head())
        } catch (e: Exception) {
        }
        return ""
    }

    private fun unescape(name: String): String {
        return name.removeSurrounding("`")
    }

    override fun getInitializer(): PsiExpression? {
        throw UnsupportedOperationException()
    }

    override fun getDocComment(): PsiDocComment? {
        return scVariable.docComment
    }

    override fun getTypeElement(): PsiTypeElement? {
        val typeElement = scVariable.typeElement()
        return if (typeElement.isDefined) {
            ScalaPsiTypeElementAdaptor(typeElement.get())
        } else null
    }

    override fun hasInitializer(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getType(): PsiType {
        val typeEither = scVariable.type()
        if (typeEither.isRight) {
            return ScalaPsiTypeAdaptor.build(typeEither.right().get())
        }

        throw IllegalArgumentException("failed to getType for:${this.scVariable}")
    }

    override fun isDeprecated(): Boolean {
        return this.scVariable.isDeprecated
    }

    override fun setName(name: String): PsiElement {
        throw UnsupportedOperationException("set name for:${this.scVariable} is unsupported")
    }

    override fun computeConstantValue(): Any? {
        return null
    }



}