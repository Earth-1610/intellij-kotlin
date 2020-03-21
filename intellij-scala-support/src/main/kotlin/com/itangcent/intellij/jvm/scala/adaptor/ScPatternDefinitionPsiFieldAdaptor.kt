package com.itangcent.intellij.jvm.scala.adaptor

import com.intellij.navigation.ItemPresentation
import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScPatternDefinition

/**
 * read only
 */
class ScPatternDefinitionPsiFieldAdaptor(private val scPatternDefinition: ScPatternDefinition) :
    ScAdaptor<ScPatternDefinition>,
    PsiField,
    PsiModifierListOwner by scPatternDefinition {

    override fun adaptor(): ScPatternDefinition {
        return scPatternDefinition
    }

    override fun setInitializer(initializer: PsiExpression?) {
        throw UnsupportedOperationException()
    }

    override fun getContainingClass(): PsiClass? {
        return scPatternDefinition.containingClass
    }

    override fun normalizeDeclaration() {
        //nothing to do
    }

    override fun getNameIdentifier(): PsiIdentifier {
        throw UnsupportedOperationException()
    }

    override fun getPresentation(): ItemPresentation? {
        return scPatternDefinition.presentation
    }

    override fun getName(): String {
        val name = scPatternDefinition.name
        if (!name.isNullOrBlank()) {
            return unescape(name)
        }
        try {
            return unescape(scPatternDefinition.declaredNames().head())
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
        return scPatternDefinition.docComment
    }

    override fun getTypeElement(): PsiTypeElement? {
        val typeElement = scPatternDefinition.typeElement()
        return if (typeElement.isDefined) {
            ScalaPsiTypeElementAdaptor(typeElement.get())
        } else null
    }

    override fun hasInitializer(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getType(): PsiType {
        val typeEither = scPatternDefinition.type()
        if (typeEither.isRight) {
            return ScalaPsiTypeAdaptor.build(typeEither.right().get())
        }

        throw IllegalArgumentException("failed to getType for:${this.scPatternDefinition}")
    }

    override fun isDeprecated(): Boolean {
        return this.scPatternDefinition.isDeprecated
    }

    override fun setName(name: String): PsiElement {
        throw UnsupportedOperationException("set name for:${this.scPatternDefinition} is unsupported")
    }

    override fun computeConstantValue(): Any? {
        scPatternDefinition.expr()
        return null
    }

    fun expr(): ScExpression? {
        val expr = scPatternDefinition.expr()
        if (expr == null || expr.isEmpty) {
            return null
        }
        return expr.get()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScPatternDefinitionPsiFieldAdaptor

        if (scPatternDefinition != other.scPatternDefinition) return false

        return true
    }

    override fun hashCode(): Int {
        return scPatternDefinition.hashCode()
    }

}