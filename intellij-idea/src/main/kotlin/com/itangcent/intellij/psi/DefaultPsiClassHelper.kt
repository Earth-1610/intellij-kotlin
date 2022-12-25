package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.*
import com.intellij.util.containers.isNullOrEmpty
import com.itangcent.common.logger.traceWarn
import com.itangcent.common.utils.KV
import com.itangcent.common.utils.notNullOrBlank
import com.itangcent.common.utils.notNullOrEmpty
import com.itangcent.common.utils.sub
import com.itangcent.intellij.config.rule.computer
import com.itangcent.intellij.jvm.JsonOption
import com.itangcent.intellij.jvm.JsonOption.has
import com.itangcent.intellij.jvm.SourceHelper
import com.itangcent.intellij.jvm.asPsiClass
import com.itangcent.intellij.jvm.dev.DevEnv
import com.itangcent.intellij.jvm.duck.DuckType
import com.itangcent.intellij.jvm.duck.SingleDuckType
import com.itangcent.intellij.jvm.element.ExplicitClass
import com.itangcent.intellij.jvm.element.ExplicitElement
import org.apache.commons.lang3.StringUtils

@Singleton
open class DefaultPsiClassHelper : AbstractPsiClassHelper() {

    @Inject(optional = true)
    protected val sourceHelper: SourceHelper? = null

    @Suppress("UNCHECKED_CAST")
    protected open fun resolveSeeDoc(field: PsiField, comment: HashMap<String, Any?>) {
        val sees = getSees(field).takeIf { it.notNullOrEmpty() } ?: return
        resolveSeeDoc(field, sees, comment)
    }

    @Suppress("UNCHECKED_CAST")
    protected open fun resolveSeeDoc(field: PsiField, fieldName: String, comment: HashMap<String, Any?>) {
        val sees = getSees(field).takeIf { it.notNullOrEmpty() } ?: return
        devEnv?.dev {
            logger.debug("get sees from $fieldName: $sees")
        }
        resolveSeeDoc(field, fieldName, sees, comment)
    }

    @Suppress("UNCHECKED_CAST")
    protected open fun resolveSeeDoc(
        field: PsiField,
        sees: List<String>,
        comment: HashMap<String, Any?>
    ) {
        resolveSeeDoc(field, field.name, sees, comment)
    }

    @Suppress("UNCHECKED_CAST")
    protected open fun resolveSeeDoc(
        field: PsiField,
        fieldName: String,
        sees: List<String>,
        comment: HashMap<String, Any?>
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
            var name = if (!nameByRule.isNullOrBlank()) {
                nameByRule
            } else {
                psiField.name
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
            if (!nameByRule.isNullOrBlank()) {
                return nameByRule
            }
        } catch (e: Exception) {
            logger.traceWarn("error to get field name:${PsiClassUtils.fullNameOfMethod(psiMethod)}", e)
        }

        return super.getJsonFieldName(psiMethod)
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

    override fun beforeParseFieldOrMethod(
        fieldName: String,
        fieldType: DuckType,
        fieldOrMethod: ExplicitElement<*>,
        resourcePsiClass: ExplicitClass,
        resolveContext: ResolveContext,
        kv: KV<String, Any?>
    ): Boolean {
        if (ruleComputer.computer(ClassRuleKeys.FIELD_IGNORE, fieldOrMethod) == true) {
            return false
        }

        return super.beforeParseFieldOrMethod(fieldName, fieldType, fieldOrMethod, resourcePsiClass, resolveContext, kv)
    }

    @Suppress("UNCHECKED_CAST")
    override fun parseFieldOrMethod(
        fieldName: String,
        fieldType: DuckType,
        fieldOrMethod: ExplicitElement<*>,
        resourcePsiClass: ExplicitClass,
        resolveContext: ResolveContext,
        kv: KV<String, Any?>
    ) {
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
                                super.parseFieldOrMethod(
                                    fieldName,
                                    it,
                                    duckTypeHelper.explicit(convertFieldOrMethod)!!,
                                    resourcePsiClass,
                                    resolveContext,
                                    kv
                                )
                            }
                        } else if (convertFieldOrMethod is PsiMethod) {
                            duckTypeHelper!!.ensureType(convertFieldOrMethod.returnType!!)?.let {
                                super.parseFieldOrMethod(
                                    fieldName,
                                    it,
                                    duckTypeHelper.explicit(convertFieldOrMethod)!!,
                                    resourcePsiClass,
                                    resolveContext,
                                    kv
                                )
                            }
                        }
                        //doc comment
                        if (resolveContext.option.has(JsonOption.READ_COMMENT)) {
                            val commentKV: KV<String, Any?> = kv.sub("@comment")
                            resolveSeeDoc(
                                fieldName, fieldOrMethod.psi() as? PsiMember ?: enumClass, listOf(
                                    PsiClassUtils.fullNameOfMember(
                                        classWithFieldOrMethod.first.asPsiClass(jvmClassHelper),
                                        convertFieldOrMethod
                                    )
                                ), commentKV
                            )
                        }
                        return
                    }
                } else {
                    val resolveClass = duckTypeHelper!!.resolve(convertTo, fieldOrMethod.psi())
                    if (resolveClass == null) {
                        logger.error("failed to resolve class:$convertTo")
                    } else {
                        super.parseFieldOrMethod(
                            fieldName,
                            resolveClass,
                            fieldOrMethod,
                            resourcePsiClass,
                            resolveContext,
                            kv
                        )
                        return
                    }
                }
            }
        }
        super.parseFieldOrMethod(fieldName, fieldType, fieldOrMethod, resourcePsiClass, resolveContext, kv)
    }

    @Suppress("UNCHECKED_CAST")
    override fun afterParseFieldOrMethod(
        fieldName: String,
        fieldType: DuckType,
        fieldOrMethod: ExplicitElement<*>,
        resourcePsiClass: ExplicitClass,
        resolveContext: ResolveContext,
        kv: KV<String, Any?>
    ) {
        //doc comment
        if (resolveContext.option.has(JsonOption.READ_COMMENT)) {
            val commentKV: KV<String, Any?> = kv.sub("@comment")
            val psiFieldOrMethod = fieldOrMethod.psi()
            if (psiFieldOrMethod is PsiField) {
                val field: PsiField = psiFieldOrMethod
                commentKV[fieldName] = docHelper!!.getAttrOfField(psiFieldOrMethod)?.trim()
                resolveSeeDoc(field, fieldName, commentKV)
            } else if (psiFieldOrMethod is PsiMethod) {
                val attrInDoc = docHelper!!.getAttrOfDocComment(psiFieldOrMethod)
                if (StringUtils.isNotBlank(attrInDoc)) {
                    commentKV[fieldName] = attrInDoc
                }
            }
        }
    }

    override fun getAttrOfField(field: PsiField): String? {
        return docHelper!!.getAttrOfField(field)?.trim()
    }
}
