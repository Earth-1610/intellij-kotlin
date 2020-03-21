package com.itangcent.intellij.jvm.scala.adaptor

import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeElement
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.DuckTypeHelper
import com.itangcent.intellij.jvm.scala.castToTypedList
import com.itangcent.intellij.jvm.scala.getOrNull
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScParameterizedTypeElement
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter
import org.jetbrains.plugins.scala.lang.psi.types.ScType

/**
 * read only
 */
class ScalaPsiParameterAdaptor(private val psiParameter: PsiParameter) : ScAdaptor<PsiParameter>,
    PsiParameter by psiParameter {

    override fun adaptor(): PsiParameter {
        return psiParameter
    }

    override fun getTypeElement(): PsiTypeElement? {
        if (psiParameter is ScParameter) {
            val typeElement = psiParameter.typeElement()
            if (typeElement.isDefined) {
                return ScalaPsiTypeElementAdaptor(typeElement.get())
            }
        }

        val typeElement = psiParameter.typeElement
        return if (typeElement is ScTypeElement) {
            ScalaPsiTypeElementAdaptor(typeElement)
        } else {
            typeElement
        }
    }

    override fun getType(): PsiType {

        if (psiParameter is ScParameter) {
            val typeElementOption = psiParameter.paramType()
            if (typeElementOption.isDefined) {
                val typeElement = typeElementOption.get().typeElement()
                val type = typeElement.type().getOrNull<ScType>()
                if (type != null) {
                    if (typeElement is ScParameterizedTypeElement) {
                        val typeArgs = typeElement.typeArgList().typeArgs().castToTypedList<ScTypeElement>()
                        if (typeArgs.isEmpty()) {
                            return ScalaPsiTypeAdaptor.build(type)
                        }
                        val args = typeArgs.map {
                            it.type.getOrNull<ScType>()?.let { ScalaPsiTypeAdaptor.build(it) } ?: PsiType.VOID
                        }
                        val psiType = ActionContext.getContext()?.instance(DuckTypeHelper::class)
                            ?.createType(ScalaPsiTypeAdaptor.build(type), null, *(args.toTypedArray()))
                        if (psiType != null) return psiType
                    } else {
                        return ScalaPsiTypeAdaptor.build(type)
                    }
                }
            }
        }

        val type = psiParameter.type
        return if (type is ScType) {
            ScalaPsiTypeAdaptor.build(type)
        } else {
            type
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScalaPsiParameterAdaptor

        if (psiParameter != other.psiParameter) return false

        return true
    }

    override fun hashCode(): Int {
        return psiParameter.hashCode()
    }

}