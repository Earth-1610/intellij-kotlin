package com.itangcent.intellij.jvm.scala

import com.intellij.lang.jvm.JvmParameter
import com.intellij.psi.*
import com.itangcent.common.utils.getPropertyValue
import com.itangcent.common.utils.invokeMethod
import com.itangcent.common.utils.isNullOrBlank
import com.itangcent.intellij.jvm.JvmClassHelper
import com.itangcent.intellij.jvm.scala.adaptor.ScPatternDefinitionPsiFieldAdaptor
import com.itangcent.intellij.jvm.scala.adaptor.ScalaPsiFieldAdaptor
import com.itangcent.intellij.jvm.scala.adaptor.ScalaTypeParameterType2PsiTypeParameterAdaptor
import com.itangcent.intellij.jvm.scala.compatible.ScCompatiblePhysicalMethodSignature
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper.Companion.normalTypes
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScPatternDefinition
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScVariable
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

        if (psiClass is PsiClassWrapper) {
            collectFields(psiClass.definition())
                .takeIf { it.isNotEmpty() }
                ?.let { return it }
        }

        if (psiClass is ScTemplateDefinition) {
            return collectFields(psiClass)
        }

        return emptyArray()
    }

    private fun collectFields(scTemplateDefinition: ScTemplateDefinition): Array<PsiField> {
        val members = scTemplateDefinition.members()
        val fields: LinkedList<PsiField> = LinkedList()
        for (member in members) {
            if (member is ScVariable) {
                fields.add(ScalaPsiFieldAdaptor(member))
            } else if (member is ScPatternDefinition) {
                fields.add(ScPatternDefinitionPsiFieldAdaptor(member))
            }
        }
        return fields.toTypedArray()
    }

    override fun resolveClassInType(psiType: PsiType): PsiClass? {
        if (psiType is ScalaTypeParameterType2PsiTypeParameterAdaptor) {
            return psiType.getTypeParameterType()
        }
        return null
    }

    override fun getAllMethods(psiClass: PsiClass): Array<PsiMethod> {
        if (psiClass is ScTemplateDefinition) {
            val allMethods = psiClass.invokeMethod("allMethods")?.castToList()
            val methods: LinkedList<PsiMethod> = LinkedList()
            allMethods?.forEach { method ->
                ScCompatiblePhysicalMethodSignature.method(method)?.let { methods.add(it) }
            }
            return methods.toTypedArray()
        }
        return emptyArray()
    }

    override fun defineClassCode(psiClass: PsiClass): String {
        val sb = StringBuilder()
        //modifiers
        psiClass.modifiers.forEach {
            sb.append(it.name.toLowerCase()).append(" ")
        }
        when {
            psiClass.isInterface -> sb.append("trait ")
            else -> sb.append("class ")
        }
        var appendExtends = false
        sb.append(psiClass.name)
        psiClass.extendsListTypes
            .takeIf { !it.isNullOrBlank() }
            ?.let {
                if (!appendExtends) {
                    sb.append("extends ")
                    appendExtends = true
                }
                sb.append(it.joinToString(separator = " ,") { type -> type.canonicalText })
                    .append(" ")
            }
        psiClass.implementsListTypes
            .takeIf { !it.isNullOrBlank() }
            ?.let {
                if (!appendExtends) {
                    sb.append("extends ")
                    appendExtends = true
                }
                sb.append(it.joinToString(separator = " ,") { type -> type.canonicalText })
                    .append(" ")
            }
        return sb.append(";").toString()
    }

    override fun defineMethodCode(psiMethod: PsiMethod): String {
        val sb = StringBuilder()
        //modifiers
        psiMethod.modifiers.forEach {
            sb.append(it.name.toLowerCase()).append(" ")
        }
        if (psiMethod.isConstructor) {
            sb.append("def this ")
                .append("(")
        } else {
            sb.append("def ")
                .append(psiMethod.name)
                .append("(")
        }
        for ((index, parameter) in psiMethod.parameters.withIndex()) {
            if (index > 0) {
                sb.append(", ")
            }
            if (parameter is PsiParameter) {
                sb.append(defineParamCode(parameter as PsiParameter))
            } else {
                sb.append(defineParamCode(parameter))
            }
        }
        sb.append(")")
        if (!psiMethod.isConstructor) {
            psiMethod.returnType?.let {
                sb.append(": ")
                    .append(it.canonicalText).append(" ")
            }
        }
        return sb.append(";").toString()
    }

    override fun defineFieldCode(psiField: PsiField): String {
        val sb = StringBuilder()
        //modifiers
        psiField.modifiers.forEach {
            sb.append(it.name.toLowerCase()).append(" ")
        }
        if (psiField is PsiEnumConstant) {
            sb.append(psiField.name)
            psiField.argumentList?.expressions
                ?.takeIf { !it.isNullOrBlank() }
                ?.joinToString(separator = ", ") { it.text }
                ?.let {
                    sb.append("(")
                        .append(it)
                        .append(")")
                }
        } else {
            sb.append(psiField.name)
                .append(": ")
                .append(psiField.type.canonicalText)
        }
        return sb.append(";").toString()
    }

    override fun defineParamCode(psiParameter: PsiParameter): String {
        val sb = StringBuilder()
        sb.append(psiParameter.type.canonicalText)
            .append(" ")
            .append(psiParameter.name)
        return sb.toString()
    }

    private fun defineParamCode(psiParameter: JvmParameter): String {
        val sb = StringBuilder()
        sb.append(psiParameter.name)
            .append(": ")
            .append(psiParameter.type.getPropertyValue("text"))
        return sb.toString()
    }

    override fun defineOtherCode(psiElement: PsiElement): String {
        return psiElement.text
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