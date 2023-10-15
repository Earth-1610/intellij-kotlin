package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.*
import com.intellij.util.containers.isNullOrEmpty
import com.itangcent.common.logger.traceWarn
import com.itangcent.common.utils.notNullOrBlank
import com.itangcent.common.utils.notNullOrEmpty
import com.itangcent.common.utils.sub
import com.itangcent.intellij.jvm.*
import com.itangcent.intellij.jvm.JsonOption.has
import com.itangcent.intellij.jvm.duck.DuckType
import com.itangcent.intellij.jvm.duck.SingleDuckType
import com.itangcent.intellij.jvm.element.ExplicitClass

@Singleton
open class DefaultPsiClassHelper : AbstractPsiClassHelper() {

    @Inject(optional = true)
    protected val sourceHelper: SourceHelper? = null

    protected open fun resolveSeeDoc(field: PsiField, comment: HashMap<String, Any?>) {
        val sees = getSees(field).takeIf { it.notNullOrEmpty() } ?: return
        resolveSeeDoc(field, sees, comment)
    }

    protected open fun resolveSeeDoc(field: PsiField, fieldName: String, comment: MutableMap<String, Any?>) {
        val sees = getSees(field).takeIf { it.notNullOrEmpty() } ?: return
        devEnv?.dev {
            logger.debug("get sees from $fieldName: $sees")
        }
        resolveSeeDoc(field, fieldName, sees, comment)
    }

    protected open fun resolveSeeDoc(
        field: PsiField,
        sees: List<String>,
        comment: MutableMap<String, Any?>
    ) {
        resolveSeeDoc(field, field.name, sees, comment)
    }

    protected open fun resolveSeeDoc(
        field: PsiField,
        fieldName: String,
        sees: List<String>,
        comment: MutableMap<String, Any?>
    ) {
        val options: ArrayList<HashMap<String, Any?>> = ArrayList()
        sees.forEach { see ->
            resolveEnumOrStatic(see, field, fieldName)?.let {
                options.addAll(it)
            }
        }

        if (options.isNotEmpty()) {
            comment["$fieldName@options"] = options
        }
    }

    protected open fun resolveSeeDoc(
        fieldName: String,
        context: PsiMember,
        sees: List<String>,
        comment: MutableMap<String, Any?>
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

    override fun tryCastTo(duckType: DuckType, context: PsiElement): DuckType {
        return classRuleConfig?.tryConvert(duckType, context) ?: duckType
    }

    override fun isNormalType(psiType: PsiType): Boolean {
        return jvmClassHelper.isNormalType(classRuleConfig!!.tryConvert(psiType).canonicalText)
    }

    override fun isNormalType(psiClass: PsiClass): Boolean {
        return jvmClassHelper.isNormalType(classRuleConfig!!.tryConvert(psiClass).qualifiedName ?: return false)
    }

    override fun getDefaultValue(psiType: PsiType): Any? {
        return jvmClassHelper.getDefaultValue(classRuleConfig!!.tryConvert(psiType).canonicalText)
    }

    override fun getDefaultValue(psiClass: PsiClass): Any? {
        return jvmClassHelper.getDefaultValue(classRuleConfig!!.tryConvert(psiClass).qualifiedName ?: return null)
    }

    override fun getJsonFieldName(psiField: PsiField): String {
        try {
            val nameByRule = ruleComputer.computer(ClassRuleKeys.FIELD_NAME, psiField)
            var name = if (nameByRule.isNullOrBlank()) {
                psiField.name
            } else {
                nameByRule
            }
            ruleComputer.computer(ClassRuleKeys.FIELD_NAME_PREFIX, psiField)?.let {
                name = it + name
            }
            ruleComputer.computer(ClassRuleKeys.FIELD_NAME_SUFFIX, psiField)?.let {
                name += it
            }
            return name
        } catch (e: Exception) {
            logger.traceWarn("error to get field name:${PsiClassUtils.fullNameOfField(psiField)}", e)
        }

        return psiField.name
    }

    override fun getJsonFieldName(psiMethod: PsiMethod): String {
        try {
            val nameByRule = ruleComputer.computer(ClassRuleKeys.FIELD_NAME, psiMethod)
            var name = if (nameByRule.isNullOrBlank()) {
                super.getJsonFieldName(psiMethod)
            } else {
                nameByRule
            }
            ruleComputer.computer(ClassRuleKeys.FIELD_NAME_PREFIX, psiMethod)?.let {
                name = it + name
            }
            ruleComputer.computer(ClassRuleKeys.FIELD_NAME_SUFFIX, psiMethod)?.let {
                name += it
            }
        } catch (e: Exception) {
            logger.traceWarn("error to get field name:${PsiClassUtils.fullNameOfMethod(psiMethod)}", e)
        }

        return super.getJsonFieldName(psiMethod)
    }

    override fun getJsonFieldName(accessibleField: AccessibleField): String {
        try {
            val nameByRule = ruleComputer.computer(ClassRuleKeys.FIELD_NAME, accessibleField)
            var name = if (nameByRule.isNullOrBlank()) {
                accessibleField.name
            } else {
                nameByRule
            }
            ruleComputer.computer(ClassRuleKeys.FIELD_NAME_PREFIX, accessibleField)?.let {
                name = it + name
            }
            ruleComputer.computer(ClassRuleKeys.FIELD_NAME_SUFFIX, accessibleField)?.let {
                name += it
            }
            return name
        } catch (e: Exception) {
            logger.traceWarn("error to get field name:${PsiClassUtils.fullNameOfMember(accessibleField.psi)}", e)
        }

        return accessibleField.name
    }

    override fun getJsonFieldType(accessibleField: AccessibleField): DuckType {
        return ruleComputer.computer(ClassRuleKeys.FIELD_TYPE, accessibleField)?.let {
            duckTypeHelper!!.findDuckType(it, accessibleField.psi)
        } ?: accessibleField.type
    }

    override fun getResourceClass(psiClass: PsiClass): PsiClass {
        return sourceHelper?.getSourceClass(psiClass) ?: psiClass
    }

    override fun doGetTypeObject(
        duckType: SingleDuckType?,
        context: PsiElement,
        resolveContext: ResolveContext
    ): ObjectHolder? {
        if (duckType is SingleDuckType && jvmClassHelper.isEnum(duckType.psiClass())) {
            val convertTo = ruleComputer.computer(ClassRuleKeys.ENUM_CONVERT, duckType, null)
            val enumClass = duckType.psiClass()
            if (convertTo.notNullOrBlank()) {
                var typeObj: Any? = null
                if (convertTo!!.contains("#")) {
                    val classWithFieldOrMethod =
                        psiResolver!!.resolveClassWithPropertyOrMethod(convertTo, enumClass)
                    if (classWithFieldOrMethod == null) {
                        logger.error("failed to resolve:$convertTo")
                    } else {
                        val convertFieldOrMethod = classWithFieldOrMethod.second!!
                        if (convertFieldOrMethod is PsiField) {
                            duckTypeHelper!!.ensureType(convertFieldOrMethod.type)?.let {
                                typeObj = super.doGetTypeObject(it, context, resolveContext)
                            }
                        } else if (convertFieldOrMethod is PsiMethod) {
                            duckTypeHelper!!.ensureType(convertFieldOrMethod.returnType!!)?.let {
                                typeObj = super.doGetTypeObject(it, context, resolveContext)
                            }
                        }
                    }
                } else {
                    val resolveClass = duckTypeHelper!!.resolve(convertTo, context)
                    if (resolveClass == null) {
                        logger.error("failed to resolve class:$convertTo")
                    } else {
                        typeObj = super.doGetTypeObject(resolveClass, context, resolveContext)
                    }
                }
                if (typeObj == null) {
                    typeObj = super.doGetTypeObject(duckType, context, resolveContext)
                }
                val objectHolder = typeObj.asObjectHolder()
                //doc comment
                if (resolveContext.option.has(JsonOption.READ_COMMENT)) {
                    val options = resolveEnumOrStatic(convertTo, context, "")
                    return objectHolder.extend().set("@comment@options", options)
                }
                return objectHolder
            }


            //doc comment
            var typeObj: Any? = null

            val enumOptions = resolveEnumOrStatic(context, duckType.psiClass(), null, "") {
                typeObj = super.doGetTypeObject(it, context, resolveContext)
            }

            if (typeObj == null) {
                //use java.lang.String by default.
                typeObj =
                    super.doGetTypeObject(
                        duckTypeHelper!!.resolve("java.lang.String", context)!!,
                        context,
                        resolveContext
                    )
            }
            val objectHolder = typeObj.asObjectHolder()

            return if (enumOptions.isNullOrEmpty() || !resolveContext.option.has(JsonOption.READ_COMMENT)) {
                objectHolder
            } else {
                objectHolder.extend().set("@comment@options", enumOptions)
            }
        }

        return super.doGetTypeObject(duckType, context, resolveContext)
    }

    override fun beforeParseField(
        accessibleField: AccessibleField,
        resourcePsiClass: ExplicitClass,
        resolveContext: ResolveContext,
        fields: MutableMap<String, Any?>
    ): Boolean {
        if (ruleComputer.computer(ClassRuleKeys.FIELD_IGNORE, accessibleField) == true) {
            return false
        }

        return super.beforeParseField(
            accessibleField,
            resourcePsiClass,
            resolveContext,
            fields
        )
    }

    override fun parseField(
        accessibleField: AccessibleField,
        resourcePsiClass: ExplicitClass,
        resolveContext: ResolveContext,
        fields: MutableMap<String, Any?>
    ) {
        val fieldType = accessibleField.jsonFieldType()
        if (fieldType is SingleDuckType && jvmClassHelper.isEnum(fieldType.psiClass())) {
            val convertTo = ruleComputer.computer(ClassRuleKeys.ENUM_CONVERT, fieldType, null)
            val enumClass = fieldType.psiClass()
            if (convertTo.notNullOrBlank()) {
                if (convertTo!!.contains("#")) {
                    val classWithFieldOrMethod =
                        psiResolver!!.resolveClassWithPropertyOrMethod(convertTo, enumClass)
                    if (classWithFieldOrMethod == null) {
                        logger.error("failed to resolve:$convertTo")
                    } else {
                        val convertFieldOrMethod = classWithFieldOrMethod.second!!
                        if (convertFieldOrMethod is PsiField) {
                            duckTypeHelper!!.ensureType(convertFieldOrMethod.type)?.let {
                                super.parseField(
                                    ConvertedAccessibleField(accessibleField, it),
                                    resourcePsiClass,
                                    resolveContext,
                                    fields
                                )
                            }
                        } else if (convertFieldOrMethod is PsiMethod) {
                            duckTypeHelper!!.ensureType(convertFieldOrMethod.returnType!!)?.let {
                                super.parseField(
                                    ConvertedAccessibleField(accessibleField, it),
                                    resourcePsiClass,
                                    resolveContext,
                                    fields
                                )
                            }
                        }
                        //doc comment
                        if (resolveContext.option.has(JsonOption.READ_COMMENT)) {
                            val comments = fields.sub("@comment")
                            resolveSeeDoc(
                                accessibleField.jsonFieldName(), accessibleField.psi as? PsiMember ?: enumClass, listOf(
                                    PsiClassUtils.fullNameOfMember(
                                        classWithFieldOrMethod.first.asPsiClass(jvmClassHelper),
                                        convertFieldOrMethod
                                    )
                                ), comments
                            )
                        }
                        return
                    }
                } else {
                    val resolveClass = duckTypeHelper!!.resolve(convertTo, accessibleField.psi)
                    if (resolveClass == null) {
                        logger.error("failed to resolve class:$convertTo")
                    } else {
                        super.parseField(
                            accessibleField,
                            resourcePsiClass,
                            resolveContext,
                            fields
                        )
                        return
                    }
                }
            }
        }
        super.parseField(accessibleField, resourcePsiClass, resolveContext, fields)
    }

    override fun afterParseField(
        accessibleField: AccessibleField,
        resourcePsiClass: ExplicitClass,
        resolveContext: ResolveContext,
        fields: MutableMap<String, Any?>
    ) {
        //doc comment
        if (resolveContext.option.has(JsonOption.READ_COMMENT)) {
            val comments = fields.sub("@comment")
            val psiFieldOrMethod = accessibleField.psi
            if (psiFieldOrMethod is PsiField) {
                comments[accessibleField.jsonFieldName()] = docHelper!!.getAttrOfField(psiFieldOrMethod)?.trim()
                resolveSeeDoc(psiFieldOrMethod, accessibleField.jsonFieldName(), comments)
            } else if (psiFieldOrMethod is PsiMethod) {
                comments[accessibleField.jsonFieldName()] = docHelper!!.getAttrOfDocComment(psiFieldOrMethod)?.trim()
            }
        }
    }
}

class ConvertedAccessibleField(
    private val accessibleField: AccessibleField,
    override val type: DuckType
) : AccessibleField by accessibleField