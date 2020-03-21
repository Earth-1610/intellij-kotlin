package com.itangcent.intellij.jvm.scala.adaptor

import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList

/**
 * read only
 */
class ScalaPsiParameterListAdaptor(private val psiParameterList: PsiParameterList) :
    ScAdaptor<PsiParameterList>,
    PsiParameterList by psiParameterList {

    override fun adaptor(): PsiParameterList {
        return psiParameterList
    }

    override fun getParameters(): Array<PsiParameter> {
        return psiParameterList.parameters.map {
            ScalaPsiParameterAdaptor(it)
        }.toTypedArray()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScalaPsiParameterListAdaptor

        if (psiParameterList != other.psiParameterList) return false

        return true
    }

    override fun hashCode(): Int {
        return psiParameterList.hashCode()
    }

}