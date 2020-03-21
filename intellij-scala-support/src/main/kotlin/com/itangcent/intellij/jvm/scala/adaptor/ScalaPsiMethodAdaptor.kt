package com.itangcent.intellij.jvm.scala.adaptor

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeElement
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement
import org.jetbrains.plugins.scala.lang.psi.types.ScType

/**
 * read only
 */
class ScalaPsiMethodAdaptor(private val psiMethod: PsiMethod) :
    ScAdaptor<PsiMethod>,
    PsiMethod by psiMethod {
    override fun adaptor(): PsiMethod {
        return psiMethod
    }

    override fun getParameterList(): PsiParameterList {
        return ScalaPsiParameterListAdaptor(psiMethod.parameterList)
    }

    override fun getReturnTypeElement(): PsiTypeElement? {
        val typeElement = psiMethod.returnTypeElement
        return if (typeElement is ScTypeElement) {
            ScalaPsiTypeElementAdaptor(typeElement)
        } else {
            typeElement
        }
    }

    override fun getReturnType(): PsiType? {
        val type = psiMethod.returnType
        return if (type is ScType) {
            ScalaPsiTypeAdaptor.build(type)
        } else {
            type
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScalaPsiMethodAdaptor

        if (psiMethod != other.psiMethod) return false

        return true
    }

    override fun hashCode(): Int {
        return psiMethod.hashCode()
    }
}