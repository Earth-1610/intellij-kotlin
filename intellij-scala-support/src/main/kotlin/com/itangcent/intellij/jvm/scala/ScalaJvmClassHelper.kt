package com.itangcent.intellij.jvm.scala

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.itangcent.intellij.jvm.JvmClassHelper
import com.itangcent.intellij.jvm.scala.adaptor.ScalaPsiFieldAdaptor
import com.itangcent.intellij.jvm.scala.adaptor.ScalaTypeParameterType2PsiTypeParameterAdaptor
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper.Companion.normalTypes
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScVariable
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition
import java.util.*

class ScalaJvmClassHelper(private val jvmClassHelper: JvmClassHelper) : JvmClassHelper by jvmClassHelper {

    override fun isAccessibleField(field: PsiField): Boolean {
        if (field is ScalaPsiFieldAdaptor) {
            return true
        }
        return jvmClassHelper.isAccessibleField(field)
    }

    override fun getAllFields(psiClass: PsiClass): Array<PsiField> {
        if (psiClass is ScClass) {
//            val allVals = psiClass.allVals()
//            val valList = allVals.toList()
//            for (tuple2 in valList) {
//                var element = tuple2._1()
//                val scSubstitutor = tuple2._2()
//            }
            val members = psiClass.members()
            val fields: LinkedList<PsiField> = LinkedList()
            for (member in members) {
                if (member is ScVariable) {
                    fields.add(ScalaPsiFieldAdaptor(member))
                }
            }
            return fields.toTypedArray()
        }

        return jvmClassHelper.getAllFields(psiClass)
    }

    override fun resolveClassInType(psiType: PsiType): PsiClass? {
        if (psiType is ScalaTypeParameterType2PsiTypeParameterAdaptor) {
            return psiType.getTypeParameterType()
        }
        return jvmClassHelper.resolveClassInType(psiType)
    }

    override fun getAllMethods(psiClass: PsiClass): Array<PsiMethod> {
        if (psiClass is ScTemplateDefinition) {
            val allMethods = psiClass.allMethods()
            val methods: LinkedList<PsiMethod> = LinkedList()
            for (method in allMethods) {
                methods.add(method.method())
            }
            return methods.toTypedArray()
        }
        return jvmClassHelper.getAllMethods(psiClass)
    }

    companion object {

        init {
            normalTypes["Int"] = 0
            normalTypes["Byte"] = 0
            normalTypes["Short"] = 0
            normalTypes["Long"] = 0
            normalTypes["Float"] = 0.0
            normalTypes["Double"] = 0.0
            normalTypes["Any"] = Object()
            normalTypes["AnyRef"] = Object()
            normalTypes["Null"] = null
            normalTypes["Unit"] = null
            normalTypes["_root_.scala.Predef.String"] = ""
        }
    }

}