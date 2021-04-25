package com.itangcent.intellij.jvm.scala

import com.intellij.lang.jvm.JvmParameter
import com.intellij.psi.*
import com.itangcent.common.utils.getPropertyValue
import com.itangcent.common.utils.invokeMethod
import com.itangcent.intellij.jvm.ClassMateDataStorage
import com.itangcent.intellij.jvm.JvmClassHelper
import com.itangcent.intellij.jvm.duck.DuckType
import com.itangcent.intellij.jvm.scala.adaptor.ScPatternDefinitionPsiFieldAdaptor
import com.itangcent.intellij.jvm.scala.adaptor.ScalaPsiFieldAdaptor
import com.itangcent.intellij.jvm.scala.adaptor.ScalaTypeParameterType2PsiTypeParameterAdaptor
import com.itangcent.intellij.jvm.scala.compatible.ScCompatiblePhysicalMethodSignature
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

    override fun isInheritor(duckType: DuckType, vararg baseClass: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isMap(duckType: DuckType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isMap(psiClass: PsiClass): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isMap(psiType: PsiType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isCollection(duckType: DuckType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isCollection(psiClass: PsiClass): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isCollection(psiType: PsiType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isString(psiClass: PsiClass): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isString(psiType: PsiType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isString(duckType: DuckType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isPublicStaticFinal(field: PsiField): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isNormalType(typeName: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isPrimitive(typeName: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun isPrimitiveWrapper(typeName: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getDefaultValue(typeName: String): Any? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isBasicMethod(methodName: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEnum(duckType: DuckType): Boolean {
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

    override fun isInterface(psiType: PsiType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isInterface(psiClass: PsiClass): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isInterface(duckType: DuckType): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

    override fun getFields(psiClass: PsiClass): Array<PsiField> {
        return getAllFields(psiClass)
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

    /**
     * Returns the list of methods in the class and all its superclasses.
     *
     * @return the list of methods.
     */
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

    /**
     * Returns the list of methods in the class.
     *
     * @return the list of methods.
     */
    override fun getMethods(psiClass: PsiClass): Array<PsiMethod> {
        return getAllMethods(psiClass)
    }

    override fun extractModifiers(psiElement: PsiElement): List<String> {
        return jvmClassHelper.extractModifiers(psiElement)
    }

    override fun defineCode(psiElement: PsiElement): String {
        if (!ScPsiUtils.isScPsiInst(psiElement)) {
            throw NotImplementedError("not implemented")
        }
        return super.defineCode(psiElement)
    }

    override fun defineClassCode(psiClass: PsiClass): String {
        if (!ScPsiUtils.isScPsiInst(psiClass)) {
            throw NotImplementedError("not implemented")
        }
        val sb = StringBuilder()
        //modifiers
        extractModifiers(psiClass).forEach {
            sb.append(it).append(" ")
        }
        when {
            psiClass.isInterface -> sb.append("trait ")
            else -> sb.append("class ")
        }
        var appendExtends = false
        sb.append(psiClass.name)
        psiClass.extendsListTypes
            .takeIf { !it.isNullOrEmpty() }
            ?.let {
                if (!appendExtends) {
                    sb.append(" extends ")
                    appendExtends = true
                }
                sb.append(it.joinToString(separator = " ,") { type -> type.canonicalText })
            }
        psiClass.implementsListTypes
            .takeIf { !it.isNullOrEmpty() }
            ?.let {
                if (!appendExtends) {
                    sb.append(" extends ")
                    appendExtends = true
                } else {
                    sb.append(" ")
                }
                sb.append(it.joinToString(separator = " ,") { type -> type.canonicalText })
            }
        return sb.append(";").toString()
    }

    override fun defineMethodCode(psiMethod: PsiMethod): String {
        if (!ScPsiUtils.isScPsiInst(psiMethod)) {
            throw NotImplementedError("not implemented")
        }
        val sb = StringBuilder()
        //modifiers
        extractModifiers(psiMethod).forEach {
            sb.append(it).append(" ")
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
        if (!ScPsiUtils.isScPsiInst(psiField)) {
            throw NotImplementedError("not implemented")
        }
        val sb = StringBuilder()
        //modifiers
        extractModifiers(psiField).forEach {
            sb.append(it).append(" ")
        }
        if (psiField is PsiEnumConstant) {
            sb.append(psiField.name)
            psiField.argumentList?.expressions
                ?.takeIf { !it.isNullOrEmpty() }
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
        if (!ScPsiUtils.isScPsiInst(psiParameter)) {
            throw NotImplementedError("not implemented")
        }
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
        if (!ScPsiUtils.isScPsiInst(psiElement)) {
            throw NotImplementedError("not implemented")
        }
        return psiElement.text
    }

    companion object {

        init {
            ClassMateDataStorage.addTag(scala.Byte::class, "normal", "primitive")
            ClassMateDataStorage.setDefaultValue(scala.Byte::class, 0.toByte())

            ClassMateDataStorage.addTag(scala.Int::class, "normal", "primitive")
            ClassMateDataStorage.setDefaultValue(scala.Int::class, 0)

            ClassMateDataStorage.addTag(scala.Short::class, "normal", "primitive")
            ClassMateDataStorage.setDefaultValue(scala.Short::class, 0.toShort())

            ClassMateDataStorage.addTag(scala.Float::class, "normal", "primitive")
            ClassMateDataStorage.setDefaultValue(scala.Float::class, 0.0f)

            ClassMateDataStorage.addTag(scala.Double::class, "normal", "primitive")
            ClassMateDataStorage.setDefaultValue(scala.Double::class, 0.0)


            ClassMateDataStorage.addTag("_root_.scala.Predef.String", "string")
            ClassMateDataStorage.setDefaultValue("_root_.scala.Predef.String", "")

            ClassMateDataStorage.addTag("Any", "normal")
            ClassMateDataStorage.addTag("AnyRef", "normal")
            ClassMateDataStorage.addTag("Null", "normal")
            ClassMateDataStorage.addTag("Unit", "normal")
        }
    }

}