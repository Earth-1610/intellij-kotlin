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
import org.jetbrains.plugins.scala.lang.psi.light.PsiClassWrapper
import java.util.*

class ScalaJvmClassHelper(val jvmClassHelper: JvmClassHelper) : JvmClassHelper {

    //region not implemented
    override fun isStaticFinal(field: PsiField): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isMap(psiClass: PsiClass): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isMap(psiType: PsiType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isCollection(psiClass: PsiClass): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isCollection(psiType: PsiType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isPublicStaticFinal(field: PsiField): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isNormalType(typeName: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDefaultValue(typeName: String): Any? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isBasicMethod(methodName: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEnum(psiType: PsiType): Boolean {

        val cls = resolveClassInType(psiType)

        return cls?.let { isEnum(it) } ?: false
    }

    override fun isEnum(psiClass: PsiClass): Boolean {
        if (psiClass is PsiClassWrapper) {
            for (superClass in psiClass.definition().supers()) {
                if (superClass.isEnum || superClass.qualifiedName == "scala.Enumeration") {
                    return true
                }
            }
        }

        throw NotImplementedError()
    }

    override fun resolveClassToType(psiClass: PsiClass): PsiType? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isInheritor(psiClass: PsiClass, vararg baseClass: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isInheritor(psiType: PsiType, vararg baseClass: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    //endregion

    override fun isAccessibleField(field: PsiField): Boolean {
        if (field is ScalaPsiFieldAdaptor) {
            return true
        }
        return false
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

        return emptyArray()
    }

    override fun resolveClassInType(psiType: PsiType): PsiClass? {
        if (psiType is ScalaTypeParameterType2PsiTypeParameterAdaptor) {
            return psiType.getTypeParameterType()
        }
        return null
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
        return emptyArray()
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