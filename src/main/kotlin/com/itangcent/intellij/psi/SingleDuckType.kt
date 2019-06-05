package com.itangcent.intellij.psi

import com.intellij.psi.PsiClass

class SingleDuckType : DuckType {
    val psiCls: PsiClass

    val typeParams: Map<String, DuckType?>?//泛型类型

    constructor(psiCls: PsiClass) {
        this.psiCls = psiCls
        this.typeParams = null
    }

    constructor(psiCls: PsiClass, typeParams: Map<String, DuckType?>?) {
        this.psiCls = psiCls
        this.typeParams = typeParams
    }

    override fun toString(): String {
        if (typeParams == null) {
            return "$psiCls"
        } else {
            val sb = StringBuilder()
            typeParams.forEach { t, u ->
                if (sb.isNotEmpty()) sb.append(",")
                sb.append(t).append("->").append(u)
            }
            return "$psiCls with [$sb]"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SingleDuckType

        if (psiCls != other.psiCls) return false
        if (typeParams != other.typeParams) return false

        return true
    }

    override fun hashCode(): Int {
        var result = psiCls.hashCode()
        result = 31 * result + (typeParams?.hashCode() ?: 0)
        return result
    }


}