package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.java.stubs.impl.PsiClassStubImpl
import com.intellij.util.containers.isNullOrEmpty
import com.itangcent.common.utils.KV
import com.itangcent.common.utils.safeComputeIfAbsent
import com.itangcent.intellij.jvm.SingleDuckType
import org.apache.commons.lang3.StringUtils
import java.util.*

@Singleton
open class DefaultPsiClassHelper : AbstractPsiClassHelper() {

    @Inject(optional = true)
    protected val classRuleConfig: ClassRuleConfig? = null

    @Inject
    protected val project: Project? = null

    override fun getAttrOfField(field: PsiField): String? {

        var result: String? = null

        val attrInDoc = docHelper!!.getAttrOfDocComment(field)
        if (StringUtils.isNotBlank(attrInDoc)) {
            result = (result ?: "") + attrInDoc
        }

        val docByRule = ruleComputer!!.computer(ClassRuleKeys.FIELD_DOC, field)

        if (StringUtils.isNotBlank(docByRule)) {
            result = (result ?: "") + docByRule
        }

        var fieldText = field.text
        if (fieldText.contains("//")) {
            fieldText = fieldText.trim()

            val lines = fieldText.split('\r', '\n')
            var innerDoc = ""
            for (line in lines) {
                if (StringUtils.isBlank(line)) {
                    continue
                }
                //ignore region/endregion
                if (!line.contains("//")
                    || line.startsWith("//region")
                    || line.startsWith("//endregion")
                ) {
                    continue
                }
                if (innerDoc.isNotEmpty()) innerDoc += ","
                innerDoc += line.substringAfter("//").trim()
            }
            if (StringUtils.isNotBlank(innerDoc)) {
                result = (result ?: "") + innerDoc
            }
        }

        return result
    }

    @Suppress("UNCHECKED_CAST")
    protected open fun resolveSeeDoc(field: PsiField, comment: HashMap<String, Any?>) {
        val sees = getSees(field)
        if (sees.isNullOrEmpty()) return

        resolveSeeDoc(field.name, field, sees!!, comment)
    }

    @Suppress("UNCHECKED_CAST")
    protected open fun resolveSeeDoc(
        fieldName: String,
        context: PsiMember,
        sees: List<String>,
        comment: HashMap<String, Any?>
    ) {

        val options: ArrayList<HashMap<String, Any?>> = ArrayList()

        sees.forEach { see ->
            resolveEnumOrStatic(see, context, fieldName)?.let { options.addAll(it) }
        }

        if (options.isNotEmpty()) {
            comment["$fieldName@options"] = options
        }
    }

    protected open fun getSees(field: PsiJavaDocumentedElement): List<String>? {
        val sees: List<String>? = docHelper!!.findDocsByTag(field, "see")
        if (sees.isNullOrEmpty()) {
            return null
        }
        return sees
    }

    override fun tryCastTo(psiType: PsiType, context: PsiElement): PsiType {
        return classRuleConfig?.tryConvert(psiType, context) ?: psiType
    }

    override fun isNormalType(psiType: PsiType): Boolean {
        return jvmClassHelper!!.isNormalType(classRuleConfig!!.tryConvert(psiType).canonicalText)
    }

    override fun isNormalType(psiClass: PsiClass): Boolean {
        return jvmClassHelper!!.isNormalType(classRuleConfig!!.tryConvert(psiClass).qualifiedName ?: return false)
    }

    override fun getDefaultValue(psiType: PsiType): Any? {
        return jvmClassHelper!!.getDefaultValue(classRuleConfig!!.tryConvert(psiType).canonicalText)
    }

    override fun getDefaultValue(psiClass: PsiClass): Any? {
        return jvmClassHelper!!.getDefaultValue(classRuleConfig!!.tryConvert(psiClass).qualifiedName ?: return null)
    }

    override fun getJsonFieldName(psiField: PsiField): String {
        val nameByRule = ruleComputer!!.computer(ClassRuleKeys.FIELD_NAME, psiField)
        if (!nameByRule.isNullOrBlank()) {
            return nameByRule
        }

        return psiField.name
    }

    override fun getResourceClass(psiClass: PsiClass): PsiClass {
        if (isLocalClass(psiClass)) {
            return psiClass
        }
        return actionContext!!.cacheOrCompute("SourceHelper::instance") {
            SourceHelper(project!!)
        }?.getSourceClass(psiClass) ?: psiClass
    }

    protected open fun isLocalClass(psiClass: PsiClass): Boolean {
        if (psiClass is StubBasedPsiElement<*>) {
            val stub = psiClass.stub
            return stub is PsiClassStubImpl<*> && stub.isLocalClassInner
        }
        return false
    }

    override fun beforeParseFieldOrMethod(
        fieldType: PsiType,
        fieldOrMethod: PsiElement,
        resourcePsiClass: PsiClass,
        option: Int,
        kv: KV<String, Any?>
    ): Boolean {
        if (fieldOrMethod is PsiField &&
            ruleComputer?.computer(ClassRuleKeys.FIELD_IGNORE, fieldOrMethod) == true
        ) {
            return false
        }

        return super.beforeParseFieldOrMethod(fieldType, fieldOrMethod, resourcePsiClass, option, kv)
    }

    override fun beforeParseFieldOrMethod(
        fieldType: PsiType,
        fieldOrMethod: PsiElement,
        resourcePsiClass: PsiClass,
        duckType: SingleDuckType,
        option: Int,
        kv: KV<String, Any?>
    ): Boolean {
        if (fieldOrMethod is PsiField &&
            ruleComputer?.computer(ClassRuleKeys.FIELD_IGNORE, fieldOrMethod) == true
        ) {
            return false
        }

        return super.beforeParseFieldOrMethod(fieldType, fieldOrMethod, resourcePsiClass, duckType, option, kv)
    }


    @Suppress("UNCHECKED_CAST")
    override fun parseFieldOrMethod(
        fieldName: String,
        fieldType: PsiType,
        fieldOrMethod: PsiElement,
        resourcePsiClass: PsiClass,
        option: Int,
        kv: KV<String, Any?>
    ) {
        if (jvmClassHelper!!.isEnum(fieldType)) {
            val convertTo = ruleComputer!!.computer(ClassRuleKeys.ENUM_CONVERT, fieldType, null)
            val enumClass = jvmClassHelper.resolveClassInType(fieldType)!!
            if (!convertTo.isNullOrBlank()) {
                if (convertTo.contains("#")) {
                    val classWithFieldOrMethod =
                        psiResolver!!.resolveClassWithPropertyOrMethod(convertTo, enumClass)
                    if (classWithFieldOrMethod == null) {

                    } else {
                        val convertFieldOrMethod = classWithFieldOrMethod.second!!
                        if (convertFieldOrMethod is PsiField) {
                            super.parseFieldOrMethod(
                                fieldName,
                                convertFieldOrMethod.type,
                                convertFieldOrMethod,
                                resourcePsiClass,
                                option,
                                kv
                            )
                        } else if (convertFieldOrMethod is PsiMethod) {
                            super.parseFieldOrMethod(
                                fieldName,
                                convertFieldOrMethod.returnType!!,
                                convertFieldOrMethod,
                                resourcePsiClass,
                                option,
                                kv
                            )
                        }
                        //doc comment
                        if (JsonOption.needComment(option)) {
                            val commentKV: KV<String, Any?> =
                                kv.safeComputeIfAbsent("@comment") { KV.create<String, Any?>() } as KV<String, Any?>
                            resolveSeeDoc(
                                fieldName, enumClass, Arrays.asList(
                                    PsiClassUtils.fullNameOfMemmber(
                                        classWithFieldOrMethod.first!!,
                                        convertFieldOrMethod
                                    )
                                ), commentKV
                            )
                        }
                        return
                    }
                } else {
                    val resolveClass = duckTypeHelper!!.findClass(convertTo, fieldOrMethod)
                    if (resolveClass == null) {
                        logger!!.error("failed to resolve class:$convertTo")
                    } else {
                        kv.safeComputeIfAbsent("@see") { KV.create<String, Any?>() }
                        val see: KV<String, Any?> = kv["@see"] as KV<String, Any?>
                        see[fieldName] = resolveClass.qualifiedName
                        super.parseFieldOrMethod(
                            fieldName,
                            jvmClassHelper.resolveClassToType(resolveClass)!!,
                            fieldOrMethod,
                            resourcePsiClass,
                            option,
                            kv
                        )
                        return
                    }
                }
            }

            super.parseFieldOrMethod(
                fieldName,
                duckTypeHelper!!.findType("java.lang.String", fieldOrMethod)!!,
                fieldOrMethod,
                resourcePsiClass,
                option,
                kv
            )

            //doc comment
            if (JsonOption.needComment(option)) {
                val commentKV: KV<String, Any?> =
                    kv.safeComputeIfAbsent("@comment") { KV.create<String, Any?>() } as KV<String, Any?>

                val options: ArrayList<HashMap<String, Any?>> = ArrayList()

                parseEnumConstant(enumClass).forEach { field ->
                    options.add(
                        KV.create<String, Any?>()
                            .set("value", field["name"])
                            .set("desc", field["desc"])
                    )
                }

                if (options.isNotEmpty()) {
                    commentKV["$fieldName@options"] = options
                }
            }
            return
        }
        super.parseFieldOrMethod(fieldName, fieldType, fieldOrMethod, resourcePsiClass, option, kv)
    }

    @Suppress("UNCHECKED_CAST")
    override fun parseFieldOrMethod(
        fieldName: String,
        fieldType: PsiType,
        fieldOrMethod: PsiElement,
        resourcePsiClass: PsiClass,
        duckType: SingleDuckType,
        option: Int,
        kv: KV<String, Any?>
    ) {

        if (jvmClassHelper!!.isEnum(fieldType)) {
            val convertTo = ruleComputer!!.computer(ClassRuleKeys.ENUM_CONVERT, fieldType, null)
            val enumClass = jvmClassHelper.resolveClassInType(fieldType)!!
            if (!convertTo.isNullOrBlank()) {
                if (convertTo.contains("#")) {
                    val classWithFieldOrMethod =
                        psiResolver!!.resolveClassWithPropertyOrMethod(convertTo, enumClass)
                    if (classWithFieldOrMethod == null) {

                    } else {
                        val convertFieldOrMethod = classWithFieldOrMethod.second!!
                        if (convertFieldOrMethod is PsiField) {
                            super.parseFieldOrMethod(
                                fieldName,
                                convertFieldOrMethod.type,
                                convertFieldOrMethod,
                                resourcePsiClass,
                                duckType,
                                option,
                                kv
                            )
                        } else if (convertFieldOrMethod is PsiMethod) {
                            super.parseFieldOrMethod(
                                fieldName,
                                convertFieldOrMethod.returnType!!,
                                convertFieldOrMethod,
                                resourcePsiClass,
                                duckType,
                                option,
                                kv
                            )
                        }
                        //doc comment
                        if (JsonOption.needComment(option)) {
                            val commentKV: KV<String, Any?> =
                                kv.safeComputeIfAbsent("@comment") { KV.create<String, Any?>() } as KV<String, Any?>
                            resolveSeeDoc(
                                fieldName, enumClass, Arrays.asList(
                                    PsiClassUtils.fullNameOfMemmber(
                                        classWithFieldOrMethod.first!!,
                                        convertFieldOrMethod
                                    )
                                ), commentKV
                            )
                        }
                        return
                    }
                } else {
                    val resolveClass = duckTypeHelper!!.findClass(convertTo, fieldOrMethod)
                    if (resolveClass == null) {
                        logger!!.error("failed to resolve class:$convertTo")
                    } else {
                        kv.safeComputeIfAbsent("@see") { KV.create<String, Any?>() }
                        val see: KV<String, Any?> = kv["@see"] as KV<String, Any?>
                        see[fieldName] = resolveClass.qualifiedName
                        super.parseFieldOrMethod(
                            fieldName,
                            jvmClassHelper.resolveClassToType(resolveClass)!!,
                            fieldOrMethod,
                            resourcePsiClass,
                            duckType,
                            option,
                            kv
                        )
                        return
                    }
                }
            }

            super.parseFieldOrMethod(
                fieldName,
                duckTypeHelper!!.findType("java.lang.String", fieldOrMethod)!!,
                fieldOrMethod,
                resourcePsiClass,
                duckType,
                option,
                kv
            )

            //doc comment
            if (JsonOption.needComment(option)) {
                val commentKV: KV<String, Any?> =
                    kv.safeComputeIfAbsent("@comment") { KV.create<String, Any?>() } as KV<String, Any?>

                val options: ArrayList<HashMap<String, Any?>> = ArrayList()

                parseEnumConstant(enumClass).forEach { field ->
                    options.add(
                        KV.create<String, Any?>()
                            .set("value", field["name"])
                            .set("desc", field["desc"])
                    )
                }

                if (options.isNotEmpty()) {
                    commentKV["$fieldName@options"] = options
                }
            }
            return
        }
        super.parseFieldOrMethod(fieldName, fieldType, fieldOrMethod, resourcePsiClass, duckType, option, kv)
    }

    @Suppress("UNCHECKED_CAST")
    override fun afterParseFieldOrMethod(
        fieldName: String,
        fieldType: PsiType,
        fieldOrMethod: PsiElement,
        resourcePsiClass: PsiClass,
        option: Int,
        kv: KV<String, Any?>
    ) {
        //doc comment
        if (JsonOption.needComment(option)) {
            var commentKV: KV<String, Any?>? = kv["@comment"] as KV<String, Any?>?
            if (commentKV == null) {
                commentKV = KV.create()
                kv["@comment"] = commentKV
            }
            if (fieldOrMethod is PsiField) {
                val field: PsiField = fieldOrMethod
                commentKV[fieldName] = getAttrOfField(field)
                resolveSeeDoc(field, commentKV)
            } else if (fieldOrMethod is PsiMethod) {
                val attrInDoc = docHelper!!.getAttrOfDocComment(fieldOrMethod)
                if (StringUtils.isNotBlank(attrInDoc)) {
                    commentKV[fieldName] = attrInDoc
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun afterParseFieldOrMethod(
        fieldName: String,
        fieldType: PsiType,
        fieldOrMethod: PsiElement,
        resourcePsiClass: PsiClass,
        duckType: SingleDuckType,
        option: Int,
        kv: KV<String, Any?>
    ) {
        //doc comment
        if (JsonOption.needComment(option)) {
            var commentKV: KV<String, Any?>? = kv["@comment"] as KV<String, Any?>?
            if (commentKV == null) {
                commentKV = KV.create()
                kv["@comment"] = commentKV
            }
            if (fieldOrMethod is PsiField) {
                val field: PsiField = fieldOrMethod
                commentKV[fieldName] = getAttrOfField(field)
                resolveSeeDoc(field, commentKV)
            } else if (fieldOrMethod is PsiMethod) {
                val attrInDoc = docHelper!!.getAttrOfDocComment(fieldOrMethod)
                if (StringUtils.isNotBlank(attrInDoc)) {
                    commentKV[fieldName] = attrInDoc
                }
            }
        }
    }
}
