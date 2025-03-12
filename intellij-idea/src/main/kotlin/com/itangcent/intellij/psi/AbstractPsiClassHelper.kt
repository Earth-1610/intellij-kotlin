package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.intellij.psi.*
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil
import com.itangcent.common.logger.traceError
import com.itangcent.common.utils.*
import com.itangcent.intellij.config.rule.*
import com.itangcent.intellij.constant.RuleConstant
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.PostConstruct
import com.itangcent.intellij.jvm.*
import com.itangcent.intellij.jvm.JsonOption.has
import com.itangcent.intellij.jvm.adapt.*
import com.itangcent.intellij.jvm.dev.DevEnv
import com.itangcent.intellij.jvm.duck.ArrayDuckType
import com.itangcent.intellij.jvm.duck.DuckType
import com.itangcent.intellij.jvm.duck.SingleDuckType
import com.itangcent.intellij.jvm.duck.SingleUnresolvedDuckType
import com.itangcent.intellij.jvm.element.ExplicitClass
import com.itangcent.intellij.jvm.element.ExplicitElement
import com.itangcent.intellij.jvm.element.ExplicitField
import com.itangcent.intellij.jvm.element.ExplicitMethod
import com.itangcent.intellij.jvm.psi.PsiClassUtil
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper.Companion.ELEMENT_OF_COLLECTION
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.util.AppropriateStringMatcher
import com.itangcent.intellij.util.Magics
import org.apache.commons.lang3.exception.ExceptionUtils
import java.util.*

abstract class AbstractPsiClassHelper : PsiClassHelper {

    protected val resolvedInfo: HashMap<String, ObjectHolder?> = LinkedHashMap()

    private var resolveContext: ResolveContext? = null

    @Inject(optional = true)
    protected val classRuleConfig: ClassRuleConfig? = null

    @Inject
    protected lateinit var logger: Logger

    @Inject(optional = true)
    protected val devEnv: DevEnv? = null

    @Inject
    protected lateinit var duckTypeHelper: DuckTypeHelper

    @Inject
    protected lateinit var actionContext: ActionContext

    @Inject
    protected lateinit var ruleComputer: RuleComputer

    @Inject
    protected lateinit var ruleLookUp: RuleLookUp

    @Inject
    protected val docHelper: DocHelper? = null

    @Inject
    protected lateinit var jvmClassHelper: JvmClassHelper

    @Inject
    protected lateinit var contextSwitchListener: ContextSwitchListener

    @Inject
    protected val psiResolver: PsiResolver? = null

    @PostConstruct
    fun init() {
        contextSwitchListener.onModuleChange {
            resolvedInfo.clear()
            resolveContext = null
        }
    }

    protected open fun <T> getResolvedInfo(key: String): ObjectHolder? {
        return resolvedInfo[key]
    }

    protected open fun cacheResolvedInfo(key: String, value: ObjectHolder?) {
        resolvedInfo[key] = value
    }

    protected open fun groupKey(context: PsiElement?, option: Int): String {
        val group = context?.let { ruleComputer.computer(ClassRuleKeys.JSON_GROUP, context) }
        return if (group.isNullOrBlank()) {
            option.toString()
        } else {
            group + option.toString()
        }
    }

    protected fun getResolveContext(): ResolveContext {
        resolveContext?.let { return it }
        val psiClass = contextSwitchListener.getContext().asPsiClass(jvmClassHelper)
        val packageName = psiClass?.let { PsiClassUtil.getPackageNameOf(it) } ?: ""
        val basePackage = if (packageName.isEmpty()) {
            null
        } else {
            packageName.getNthIndex('.', 2).takeIf { it != -1 }
                ?.let { packageName.substring(0, it) } ?: packageName
        }
        val resolveContext = SimpleResolveContext(basePackage, 0, JsonOption.NONE)
        this.resolveContext = resolveContext
        return resolveContext
    }

    override fun getTypeObject(psiType: PsiType?, context: PsiElement): Any? {
        val resolveContext = getResolveContext().copy()
        val result = doGetTypeObject(psiType, context, resolveContext)?.getOrResolve()
        if (resolveContext.blocked()) {
            logger.error("The complexity of [${psiType?.canonicalText}] has exceeded the limit. It is blocked due to ${resolveContext.blockInfo()}.")
        }
        return result
    }

    override fun getTypeObject(psiType: PsiType?, context: PsiElement, option: Int): Any? {
        val resolveContext = getResolveContext().withOption(option)
        val result = doGetTypeObject(psiType, context, resolveContext).getOrResolve()
        if (resolveContext.blocked()) {
            logger.error("The complexity of [${psiType?.canonicalText}] has exceeded the limit. It is blocked due to ${resolveContext.blockInfo()}.")
        }
        return result
    }

    override fun getTypeObject(duckType: DuckType?, context: PsiElement): Any? {
        val resolveContext = getResolveContext().copy()
        val result = doGetTypeObject(duckType, context, resolveContext).getOrResolve()
        if (resolveContext.blocked()) {
            logger.error("The complexity of ${duckType?.canonicalText()} has exceeded the limit. It is blocked due to ${resolveContext.blockInfo()}")
        }
        return result
    }

    override fun getTypeObject(duckType: DuckType?, context: PsiElement, option: Int): Any? {
        val resolveContext = getResolveContext().withOption(option)
        val result = doGetTypeObject(duckType, context, resolveContext).getOrResolve()
        if (resolveContext.blocked()) {
            logger.error("The complexity of ${duckType?.canonicalText()} has exceeded the limit. It is blocked due to ${resolveContext.blockInfo()}")
        }
        return result
    }

    fun doGetTypeObject(
        psiType: PsiType?,
        context: PsiElement,
        resolveContext: ResolveContext
    ): ObjectHolder? {
        if (psiType == null || psiType == PsiType.NULL) return null
        actionContext.checkStatus()
        val cacheKey = psiType.canonicalText + "@" + groupKey(context, resolveContext.option)

        val resolvedInfo = getResolvedInfo<Any>(cacheKey)
        if (resolvedInfo != null) {
            return resolvedInfo
        }

        if (resolveContext.blockResolve(psiType.canonicalText)) {
            return NULL_OBJECT_HOLDER
        }

        val castTo = tryCastTo(psiType, context)
        when {
            castTo == PsiType.NULL -> return null
            castTo is PsiPrimitiveType -> return ResolvedObjectHolder(PsiTypesUtil.getDefaultValue(castTo))
            isNormalType(castTo) -> return ResolvedObjectHolder(getDefaultValue(castTo))
            castTo is PsiArrayType -> {   //array type
                resolveContext.incrementElement()
                val list = ArrayList<Any?>()
                val objectHolder = list.asObjectHolder()
                cacheResolvedInfo(cacheKey, objectHolder)
                val componentType = castTo.getDeepComponentType()
                doGetTypeObject(componentType, context, resolveContext.next())?.let { list.add(it) }
                return objectHolder
            }

            jvmClassHelper.isCollection(castTo) -> {   //list type
                resolveContext.incrementElement()
                val list = ArrayList<Any?>()
                val objectHolder = list.asObjectHolder()
                cacheResolvedInfo(cacheKey, objectHolder)
                val iterableType = PsiUtil.extractIterableTypeParameter(castTo, false)
                doGetTypeObject(iterableType, context, resolveContext.next())?.let { list.add(it) }
                return objectHolder
            }

            jvmClassHelper.isMap(castTo) -> {   //list type
                resolveContext.incrementElement()
                val map: HashMap<Any, Any?> = HashMap()
                val objectHolder = map.asObjectHolder()
                cacheResolvedInfo(cacheKey, objectHolder)
                val keyType = PsiUtil.substituteTypeParameter(castTo, CommonClassNames.JAVA_UTIL_MAP, 0, false)
                val valueType = PsiUtil.substituteTypeParameter(castTo, CommonClassNames.JAVA_UTIL_MAP, 1, false)

                var defaultKey: Any? = null
                if (keyType != null) {
                    defaultKey = if (keyType == psiType) {
                        "nested type"
                    } else {
                        doGetTypeObject(keyType, context, resolveContext.next())
                    }
                }
                if (defaultKey == null) defaultKey = ""

                var defaultValue: Any? = null
                if (valueType != null) {
                    defaultValue = if (valueType == psiType) {
                        Collections.emptyMap<Any, Any>()
                    } else {
                        doGetTypeObject(valueType, context, resolveContext.next())
                    }
                }
                if (defaultValue == null) defaultValue = null

                map[defaultKey] = defaultValue

                return objectHolder
            }

            jvmClassHelper.isEnum(psiType) -> {
                resolveContext.incrementElement()
                return ResolvedObjectHolder(parseEnum(psiType, context, resolveContext))
            }

            else -> {
                val typeCanonicalText = castTo.canonicalText
                if (typeCanonicalText.contains('<') && typeCanonicalText.endsWith('>')) {
                    val duckType = duckTypeHelper.resolve(castTo, context)
                    return when {
                        duckType != null -> {
                            val result = doGetTypeObject(duckType, context, resolveContext)
                            cacheResolvedInfo(cacheKey, result)
                            result
                        }

                        else -> null
                    }
                } else {
                    val paramCls = PsiUtil.resolveClassInClassTypeOnly(castTo) ?: return null
                    if (ruleComputer.computer(ClassRuleKeys.TYPE_IS_FILE, paramCls) == true) {
                        val objectHolder = ResolvedObjectHolder(Magics.FILE_STR)
                        cacheResolvedInfo(cacheKey, objectHolder)//cache
                        return objectHolder
                    }
                    return try {
                        val result = doGetFields(paramCls, resolveContext)
                        cacheResolvedInfo(cacheKey, result)
                        result
                    } catch (e: Throwable) {
                        logger.error("error to getTypeObject:$psiType")
                        logger.trace(ExceptionUtils.getStackTrace(e))
                        null
                    }
                }
            }
        }
    }

    fun doGetTypeObject(
        duckType: DuckType?,
        context: PsiElement,
        resolveContext: ResolveContext
    ): ObjectHolder? {
        duckType ?: return null
        actionContext.checkStatus()

        val cacheKey = duckType.canonicalText() + "@" + groupKey(context, resolveContext.option)

        val resolvedInfo = getResolvedInfo<Any>(cacheKey)
        if (resolvedInfo != null) {
            return resolvedInfo
        }

        if (resolveContext.blockResolve(duckType.canonicalText())) {
            return NULL_OBJECT_HOLDER
        }

        val type = tryCastTo(duckType, context)

        if (type is ArrayDuckType) {
            resolveContext.incrementElement()
            val list = ArrayList<Any?>()
            val objectHolder = list.asObjectHolder()
            cacheResolvedInfo(cacheKey, objectHolder)
            doGetTypeObject(type.componentType(), context, resolveContext.next())?.let { list.add(it) }
            return objectHolder
        }

        if (type is SingleDuckType) {
            return doGetTypeObject(type as SingleDuckType, context, resolveContext)
        }

        if (type is SingleUnresolvedDuckType) {
            return doGetTypeObject(type.psiType(), context, resolveContext)
        }

        return null
    }

    override fun getFields(psiClass: PsiClass?): Map<String, Any?> {
        return getFields(psiClass, psiClass)
    }

    override fun getFields(psiClass: PsiClass?, context: PsiElement?): Map<String, Any?> {
        val resolveContext = getResolveContext()
        val result = doGetFields(psiClass, context, resolveContext).getOrResolve().asMap()
        if (resolveContext.blocked()) {
            logger.error("The complexity of ${psiClass?.qualifiedName} has exceeded the limit. It is blocked due to ${resolveContext.blockInfo()}.")
        }
        return result
    }

    override fun getFields(psiClass: PsiClass?, option: Int): Map<String, Any?> {
        return getFields(psiClass, psiClass, option)
    }

    override fun getFields(psiClass: PsiClass?, context: PsiElement?, option: Int): Map<String, Any?> {
        val resolveContext = getResolveContext().withOption(option)
        val result = doGetFields(psiClass, context, resolveContext).getOrResolve().asMap()
        if (resolveContext.blocked()) {
            logger.error("The complexity of ${psiClass?.qualifiedName} has exceeded the limit. It is blocked due to ${resolveContext.blockInfo()}.")
        }
        return result
    }

    private fun doGetFields(psiClass: PsiClass?, resolveContext: ResolveContext): ObjectHolder {
        return doGetFields(psiClass, psiClass, resolveContext)
    }

    fun doGetFields(
        psiClass: PsiClass?,
        context: PsiElement?,
        resolveContext: ResolveContext
    ): ObjectHolder {
        psiClass ?: return NULL_OBJECT_HOLDER
        actionContext.checkStatus()

        val qualifiedName = psiClass.qualifiedName

        val cacheKey = qualifiedName + "@" + groupKey(context, resolveContext.option)

        val resolvedInfo = getResolvedInfo<Any>(cacheKey)
        if (resolvedInfo != null) {
            return resolvedInfo
        }

        val resourcePsiClass = if (resolveContext.option.has(JsonOption.READ_COMMENT)) {
            getResourceClass(psiClass)
        } else {
            psiClass
        }

        if (resolveContext.blockResolve(qualifiedName)) {
            return NULL_OBJECT_HOLDER
        }

        val fields = linkedMapOf<String, Any?>()
        val objectHolder = fields.asObjectHolder()
        cacheResolvedInfo(cacheKey, objectHolder)

        beforeParseClass(resourcePsiClass, resolveContext, fields)

        val explicitClass = duckTypeHelper.explicit(resourcePsiClass)
        collectFields(explicitClass, resolveContext.option).forEach { accessibleField ->
            if (!beforeParseField(
                    accessibleField,
                    explicitClass,
                    resolveContext,
                    fields
                )
            ) {
                onIgnoredParseField(
                    accessibleField,
                    explicitClass,
                    resolveContext,
                    fields
                )
                return@forEach
            }

            if (!fields.contains(accessibleField.jsonFieldName())) {
                resolveContext.incrementElement()

                parseField(accessibleField, explicitClass, resolveContext.next(), fields)

                afterParseField(
                    accessibleField,
                    explicitClass,
                    resolveContext,
                    fields
                )
            }
        }

        afterParseClass(resourcePsiClass, resolveContext, fields)

        return objectHolder
    }

    open fun parseEnum(psiType: PsiType?, context: PsiElement, resolveContext: ResolveContext): Any? {
        return ""//by default use enum name `String`
    }

    /**
     * Get typeObject of SingleDuckType
     */
    protected open fun doGetTypeObject(
        duckType: SingleDuckType?,
        context: PsiElement,
        resolveContext: ResolveContext
    ): ObjectHolder? {
        duckType ?: return null
        actionContext.checkStatus()

        val cacheKey = duckType.canonicalText() + "@" + groupKey(context, resolveContext.option)

        val resolvedInfo = getResolvedInfo<Any>(cacheKey)
        if (resolvedInfo != null) {
            return resolvedInfo
        }

        if (resolveContext.blockResolve(duckType.canonicalText())) {
            return NULL_OBJECT_HOLDER
        }

        val psiClass = if (resolveContext.option.has(JsonOption.READ_COMMENT)) {
            getResourceClass(duckType.psiClass())
        } else {
            duckType.psiClass()
        }

        when {
            isNormalType(duckType) -> //normal Type
                return ResolvedObjectHolder(getDefaultValue(duckType))

            jvmClassHelper.isCollection(duckType) -> {   //list type
                resolveContext.incrementElement()
                val list = ArrayList<Any>()
                val objectHolder = list.asObjectHolder()
                cacheResolvedInfo(cacheKey, objectHolder)

                val realIterableType = findRealIterableType(duckType)
                if (realIterableType != null) {
                    doGetTypeObject(realIterableType, context, resolveContext.next())?.let { list.add(it) }
                }

                return objectHolder
            }

            jvmClassHelper.isMap(duckType) -> {
                resolveContext.incrementElement()

                val typeOfCls = duckTypeHelper.buildPsiType(duckType, context)

                //map type
                val map: HashMap<Any, Any?> = HashMap()
                val objectHolder = map.asObjectHolder()
                cacheResolvedInfo(cacheKey, objectHolder)
                val keyType = PsiUtil.substituteTypeParameter(typeOfCls, CommonClassNames.JAVA_UTIL_MAP, 0, false)
                val valueType = PsiUtil.substituteTypeParameter(typeOfCls, CommonClassNames.JAVA_UTIL_MAP, 1, false)

                var defaultKey: Any? = null

                if (keyType == null) {
                    val realKeyType = duckType.genericInfo?.get(StandardJvmClassHelper.KEY_OF_MAP)
                    defaultKey = doGetTypeObject(realKeyType, context, resolveContext.next())
                }

                if (defaultKey == null) {
                    defaultKey = if (keyType != null) {
                        doGetTypeObject(
                            duckTypeHelper.ensureType(keyType, duckType.genericInfo),
                            context,
                            resolveContext.next()
                        )
                    } else {
                        ""
                    }
                }

                var defaultValue: Any? = null

                if (valueType == null) {
                    val realValueType = duckType.genericInfo?.get(StandardJvmClassHelper.VALUE_OF_MAP)
                    defaultValue = doGetTypeObject(realValueType, context, resolveContext.next())
                }
                if (defaultValue == null && valueType != null) {
                    defaultValue = doGetTypeObject(
                        duckTypeHelper.ensureType(valueType, duckType.genericInfo),
                        context,
                        resolveContext.next()
                    )
                }

                if (defaultKey != null) {
                    map[defaultKey] = defaultValue
                }

                return objectHolder
            }

            jvmClassHelper.isEnum(duckType) -> {
                return ResolvedObjectHolder(parseEnum(duckType, context, resolveContext))
            }

            else -> //class type
            {
                if (psiClass is PsiTypeParameter) {
                    val typeParams = duckType.genericInfo
                    if (typeParams != null) {
                        val realType = typeParams[psiClass.name]
                        if (realType != null) {
                            return doGetTypeObject(realType, context, resolveContext)
                        }
                    }
                }

                if (ruleComputer.computer(ClassRuleKeys.TYPE_IS_FILE, psiClass) == true) {
                    return ResolvedObjectHolder(Magics.FILE_STR)
                }
                return doGetFields(duckType, context, resolveContext)
            }
        }
    }

    protected open fun parseEnum(psiType: SingleDuckType, context: PsiElement, resolveContext: ResolveContext): Any? {
        return ""//by default use enum name `String`
    }

    protected open fun getFields(clsWithParam: SingleDuckType, context: PsiElement?, option: Int): Map<String, Any?> {
        return doGetFields(clsWithParam, context, getResolveContext().withOption(option)).getOrResolve().asMap()
    }

    protected open fun doGetFields(
        clsWithParam: SingleDuckType,
        context: PsiElement?,
        resolveContext: ResolveContext
    ): ObjectHolder? {
        actionContext.checkStatus()

        val cacheKey = clsWithParam.canonicalText() + "@" + groupKey(context, resolveContext.option)

        val resolvedInfo = getResolvedInfo<Any>(cacheKey)
        if (resolvedInfo != null) {
            return resolvedInfo
        }

        if (resolveContext.blockResolve(clsWithParam.canonicalText())) {
            return NULL_OBJECT_HOLDER
        }

        val psiClass = if (resolveContext.option.has(JsonOption.READ_COMMENT)) {
            getResourceClass(clsWithParam.psiClass())
        } else {
            clsWithParam.psiClass()
        }
        val fields = linkedMapOf<String, Any?>()
        val objectHolder = fields.asObjectHolder()
        cacheResolvedInfo(cacheKey, objectHolder)
        beforeParseType(psiClass, clsWithParam, resolveContext, fields)

        val explicitClass = duckTypeHelper.explicit(getResourceType(clsWithParam))
        collectFields(explicitClass, resolveContext.option).forEach { accessibleField ->
            if (!beforeParseField(
                    accessibleField,
                    explicitClass,
                    resolveContext,
                    fields
                )
            ) {
                onIgnoredParseField(accessibleField, explicitClass, resolveContext, fields)
                return@forEach
            }

            if (!fields.contains(accessibleField.jsonFieldName())) {
                resolveContext.incrementElement()

                parseField(accessibleField, explicitClass, resolveContext.next(), fields)

                afterParseField(accessibleField, explicitClass, resolveContext, fields)
            }
        }

        afterParseType(psiClass, clsWithParam, resolveContext, fields)

        return objectHolder
    }

    protected open fun collectFields(
        explicitClass: ExplicitClass,
        option: Int
    ): Collection<DefaultAccessibleField> {
        actionContext.checkStatus()


        val accessibleFieldMap = linkedMapOf<String, DefaultAccessibleField>()
        explicitClass.fields().forEach {
            if (ignoreField(it.psi())) {
                return@forEach
            }
            accessibleFieldMap[it.name()] = DefaultAccessibleField(field = it)
        }
        val readGetter = option.has(JsonOption.READ_GETTER)
        val readSetter = option.has(JsonOption.READ_SETTER)
        if (readGetter or readSetter) {
            for (explicitMethod in explicitClass.methods()) {
                val method = explicitMethod.psi()
                val methodName = method.name
                if (jvmClassHelper.isBasicMethod(methodName)) continue
                if (!methodName.maybeMethodPropertyName()) {
                    continue
                }
                if (method.isConstructor) continue
                if (method.hasModifierProperty(PsiModifier.STATIC)) continue
                if (!method.hasModifierProperty(PsiModifier.PUBLIC)) continue

                if (readGetter && methodName.maybeGetterMethodPropertyName()) {
                    if (method.parameters.isNotEmpty()) continue
                    val propertyName = methodName.getterPropertyName()
                    accessibleFieldMap.computeIfAbsent(propertyName) {
                        DefaultAccessibleField()
                    }.getter = explicitMethod
                }

                if (readSetter && methodName.maybeSetterMethodPropertyName()) {
                    if (method.parameters.isEmpty()) continue
                    val propertyName = methodName.setterPropertyName()
                    accessibleFieldMap.computeIfAbsent(propertyName) {
                        DefaultAccessibleField()
                    }.setter = explicitMethod
                }
            }
        }

        var fields: Collection<DefaultAccessibleField> = accessibleFieldMap.values
        if (ruleLookUp.lookUp(ClassRuleKeys.FIELD_ORDER).isNotEmpty()) {
            try {
                fields = fields.asSequence().withIndex().sortedBy { (index, field) ->
                    ruleComputer.computer(ClassRuleKeys.FIELD_ORDER, field) ?: (index + 1)
                }.map { it.value }.toList()
            } catch (e: Exception) {
                logger.traceError("Failed to sort fields. Please check the rule of ${ClassRuleKeys.FIELD_ORDER}", e)
            }
        }
        return fields
    }

    protected open fun tryCastTo(psiType: PsiType, context: PsiElement): PsiType {
        return psiType
    }

    protected open fun tryCastTo(duckType: DuckType, context: PsiElement): DuckType {
        return duckType
    }

    protected open fun findRealIterableType(duckType: SingleDuckType): DuckType? {
        if (duckType.genericInfo.notNullOrEmpty()) {
            val realIterableType =
                duckType.genericInfo?.get(ELEMENT_OF_COLLECTION) ?: duckType.genericInfo!!.entries.first().value
            if (realIterableType != null) {
                return realIterableType
            }
        }

        val psiClass = duckType.psiClass()
        val superTypes = psiClass.superTypes
        for (superType in superTypes) {
            if (jvmClassHelper.isCollection(superType)) {
                val parameters = superType.parameters
                if (parameters.isNullOrEmpty()) {
                    continue
                }
                val typeParameters = superType.resolve()?.typeParameters
                if (typeParameters.isNullOrEmpty()) {
                    continue
                }
                for ((index, typeParameter) in typeParameters.withIndex()) {
                    if (typeParameter.name == ELEMENT_OF_COLLECTION) {
                        if (index >= parameters.size) {
                            return null
                        }
                        return duckTypeHelper.ensureType(parameters[index])
                    }
                }
                return parameters.firstOrNull()?.let { duckTypeHelper.ensureType(it) }
            }
        }
        return null
    }

    override fun resolveEnumOrStatic(
        classNameWithProperty: String,
        context: PsiElement,
        defaultPropertyName: String
    ): ArrayList<HashMap<String, Any?>>? {
        actionContext.checkStatus()
        devEnv?.dev {
            logger.debug("resolveEnumOrStatic: $classNameWithProperty")
        }
        val clsName: String?
        var property: String? = null

        val classAndPropertyOrMethod =
            psiResolver!!.resolveClassWithPropertyOrMethod(classNameWithProperty, context)

        devEnv?.dev {
            logger.debug("resolve $classNameWithProperty -> $classAndPropertyOrMethod")
        }

        if (classAndPropertyOrMethod?.first != null) {
            val first = classAndPropertyOrMethod.first?.asPsiClass(jvmClassHelper)
            if (first != null) {
                return if (classAndPropertyOrMethod.second != null) {
                    resolveEnumOrStatic(
                        context,
                        first,
                        PsiClassUtil.logicalNameOfMember(classAndPropertyOrMethod.second!!),
                        defaultPropertyName
                    )
                } else {
                    property = classNameWithProperty
                        .substringAfter(first.name ?: "")
                        .filterNot { "[]()".contains(it) }
                        .takeIf { it.isNotBlank() }
                    resolveEnumOrStatic(
                        context,
                        first,
                        property,
                        defaultPropertyName
                    )
                }
            }
        }

        if (classNameWithProperty.contains("#")) {
            clsName = classNameWithProperty.substringBefore("#")
            property = classNameWithProperty
                .substringAfter("#")
                .trim()
        } else {
            clsName = classNameWithProperty.trim()
        }
        val cls = duckTypeHelper.resolveClass(clsName, context)?.let { getResourceClass(it) }
        if (cls == null) {
            devEnv?.dev {
                logger.debug("failed resolve $classNameWithProperty")
            }
        }
        return resolveEnumOrStatic(context, cls, property, defaultPropertyName)
    }

    override fun resolveEnumOrStatic(
        context: PsiElement,
        cls: PsiClass?,
        property: String?,
        defaultPropertyName: String,
        valueTypeHandle: ((DuckType) -> Unit)?
    ): ArrayList<HashMap<String, Any?>>? {
        if (cls == null) return null

        val options: ArrayList<HashMap<String, Any?>> = ArrayList()

        if (jvmClassHelper.isEnum(cls)) {
            resolveEnum(context, cls, property, defaultPropertyName, valueTypeHandle, options)
        } else {
            resolveConstant(cls, property, options)

        }
        return options
    }

    private fun resolveEnum(
        context: PsiElement,
        cls: PsiClass,
        property: String?,
        defaultPropertyName: String,
        valueTypeHandle: ((DuckType) -> Unit)?,
        options: ArrayList<HashMap<String, Any?>>
    ) {
        val enumConstants = parseEnumConstant(cls)

        var valProperty = property.trimToNull() ?: defaultPropertyName
        if (valProperty.maybeGetterMethodPropertyName()) {
            val candidateProperty = valProperty.getterPropertyName()
            if (valProperty != candidateProperty) {
                val allFields = jvmClassHelper.getAllFields(cls)
                if (!allFields.any { it.name == valProperty }
                    && allFields.any { it.name == candidateProperty }
                ) {
                    valProperty = candidateProperty
                }
            }
        }

        if (valProperty.isNotBlank()) {
            findConstantsByProperty(enumConstants, valProperty, options)
            if (options.isNotEmpty()) return
        }

        if (property.isNullOrBlank()) {
            val customFieldName = ruleComputer.computer(ClassRuleKeys.ENUM_USE_CUSTOM, cls)
            if (!customFieldName.isNullOrBlank()) {
                findConstantsByProperty(enumConstants, customFieldName, options)
                if (options.isNotEmpty()) {
                    jvmClassHelper.getField(cls, customFieldName)?.let { appropriateField ->
                        valueTypeHandle?.invoke(duckTypeHelper.resolve(appropriateField.type, appropriateField)!!)
                    }
                    return
                }
            }
        }

        if (ruleComputer.computer(ClassRuleKeys.ENUM_USE_BY_TYPE, cls) == true) {
            val appropriateProperty = findAppropriateProperty(context, cls)
            if (appropriateProperty.isNullOrBlank()) {
                logger.debug(
                    "can not resolve ${cls.qualifiedName} for ${
                        PsiClassUtil.qualifiedNameOfMember(
                            context
                        )
                    }"
                )
            } else {
                devEnv?.dev {
                    logger.debug(
                        "select $appropriateProperty in ${cls.qualifiedName} for ${
                            PsiClassUtil.qualifiedNameOfMember(
                                context
                            )
                        }"
                    )
                }
                findConstantsByProperty(enumConstants, appropriateProperty, options)
                if (options.isNotEmpty()) {
                    jvmClassHelper.getField(cls, appropriateProperty)?.let { appropriateField ->
                        valueTypeHandle?.invoke(duckTypeHelper.resolve(appropriateField.type, appropriateField)!!)
                    }
                    return
                }
            }
        }

        if (ruleComputer.computer(ClassRuleKeys.ENUM_USE_ORDINAL, cls) == true) {
            findConstantsByProperty(enumConstants, "ordinal()", options)
            valueTypeHandle?.let { it(duckTypeHelper.resolve("java.lang.Integer", context)!!) }
            return
        }

        if (ruleComputer.computer(ClassRuleKeys.ENUM_USE_NAME, cls) != false) {
            findConstantsByProperty(enumConstants, "name()", options)
            valueTypeHandle?.let { it(duckTypeHelper.resolve("java.lang.String", context)!!) }
        }
    }


    private fun resolveConstant(
        cls: PsiClass,
        property: String?,
        options: ArrayList<HashMap<String, Any?>>
    ) {
        var constants = parseStaticFields(cls)
        if (property.notNullOrBlank()) {
            constants = constants.filter { it["name"] == property }
        }
        for (constant in constants) {
            options.add(constantToOption(constant))
        }
    }

    private fun constantToOption(
        constant: Map<String, Any?>
    ): LinkedHashMap<String, Any?> {
        val mappedVal = constant["value"]
        val desc = constant["desc"] ?: constant["name"]

        return linkedMapOf(
            "value" to mappedVal,
            "desc" to desc
        )
    }

    private fun findAppropriateProperty(
        context: PsiElement,
        cls: PsiClass
    ): String? {

        val property: Property = (context as? PsiMember)?.let { Property.of(it) } ?: return null
        val targetType: PsiType = property.type()

        val candidates = LinkedHashSet<String>()
        jvmClassHelper.getFields(cls)
            .filter { jvmClassHelper.isAccepted(it.type, targetType) }
            .filter { !ignoreField(it) }
            .map { it.name }
            .forEach { candidates.add(it) }
        jvmClassHelper.getAllMethods(cls)
            .filter { it.returnType != null && jvmClassHelper.isAccepted(it.returnType!!, targetType) }
            .map { it.name }
            .filter { it.maybeGetterMethodPropertyName() }
            .map { it.propertyName() }
            .forEach { candidates.add(it) }
        if (candidates.isEmpty()) {
            return null
        }
        if (candidates.size == 1) {
            return candidates.first()
        }

        property.name().takeIf { it.isNotEmpty() }?.let {
            return AppropriateStringMatcher.findAppropriate(property.name(), candidates)
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun findConstantsByProperty(
        enumConstants: List<Map<String, Any?>>,
        valProperty: String,
        options: ArrayList<HashMap<String, Any?>>
    ) {
        for (enumConstant in enumConstants) {
            val mappedVal: Any = when (valProperty) {
                "name()" -> enumConstant["name"]!!
                "ordinal()" -> enumConstant["ordinal"]!!
                else -> (enumConstant["params"] as? HashMap<String, Any?>?)
                    ?.get(valProperty) ?: continue
            }

            var desc = enumConstant["desc"]
            if (desc == null) {
                val params = enumConstant["params"] as? Map<String, Any?>
                if (!params.isNullOrEmpty()) {
                    desc = params
                        .mutable(false)
                        .filterKeys { k -> k != valProperty }
                        .map { entry -> entry.value.toString() }
                        .joinToString(" ")
                        .trimToNull()
                }
            }
            if (desc.anyIsNullOrBlank()) {
                desc = enumConstant["name"]
            }
            options.add(
                hashMapOf(
                    "value" to mappedVal,
                    "desc" to desc
                )
            )
        }
    }

    protected open val staticResolvedInfo: HashMap<PsiClass, List<Map<String, Any?>>> = HashMap()

    override fun parseStaticFields(psiClass: PsiClass): List<Map<String, Any?>> {
        actionContext.checkStatus()
        val resourceClass = getResourceClass(psiClass)
        if (staticResolvedInfo.containsKey(resourceClass)) {
            return staticResolvedInfo[resourceClass]!!
        }
        val res = ArrayList<Map<String, Any?>>()
        val filter: (PsiField) -> Boolean = if (resourceClass.isInterface) {
            { field -> jvmClassHelper.isStaticFinal(field) }
        } else {
            { field -> jvmClassHelper.isPublicStaticFinal(field) }
        }
        for (field in jvmClassHelper.getAllFields(resourceClass)) {

            if (!filter(field)) {
                continue
            }

            if (ruleComputer.computer(ClassRuleKeys.CONSTANT_FIELD_IGNORE, field) == true) {
                continue
            }

            val value = field.computeConstantValue() ?: continue

            res.add(
                linkedMapOf(
                    "name" to field.name,
                    "value" to value.toString(),
                    "desc" to docHelper!!.getAttrOfField(field)?.trim()
                )
            )
        }
        staticResolvedInfo[resourceClass] = res
        return res
    }

    protected open fun ignoreField(psiField: PsiField) = jvmClassHelper.isStatic(psiField)

    override fun parseEnumConstant(psiClass: PsiClass): List<Map<String, Any?>> {
        actionContext.checkStatus()
        val sourcePsiClass = getResourceClass(psiClass)
        if (!jvmClassHelper.isEnum(sourcePsiClass)) return ArrayList()

        if (staticResolvedInfo.containsKey(sourcePsiClass)) {
            return staticResolvedInfo[sourcePsiClass]!!
        }

        val res = ArrayList<Map<String, Any?>>()
        for ((index, field) in jvmClassHelper.getAllFields(sourcePsiClass).withIndex()) {
            psiResolver!!.resolveEnumFields(index, field)?.let { res.add(it) }
        }

        staticResolvedInfo[sourcePsiClass] = res
        return res
    }

    override fun isNormalType(psiType: PsiType): Boolean {
        return jvmClassHelper.isNormalType(psiType.canonicalText)
    }

    override fun isNormalType(duckType: DuckType): Boolean {
        return (duckType.isSingle()) && jvmClassHelper.isNormalType(duckType.canonicalText())
    }

    override fun isNormalType(psiClass: PsiClass): Boolean {
        return jvmClassHelper.isNormalType(psiClass.qualifiedName ?: return false)
    }

    override fun getDefaultValue(psiType: PsiType): Any? {
        return jvmClassHelper.getDefaultValue(psiType.canonicalText)
    }

    override fun getDefaultValue(duckType: DuckType): Any? {
        return jvmClassHelper.getDefaultValue(duckType.canonicalText())
    }

    override fun getDefaultValue(psiClass: PsiClass): Any? {
        return jvmClassHelper.getDefaultValue(psiClass.qualifiedName ?: return null)
    }

    override fun unboxArrayOrList(psiType: PsiType): PsiType {
        when {
            psiType is PsiPrimitiveType -> return psiType
            isNormalType(psiType) -> return psiType
            psiType is PsiArrayType -> {   //array type
                return psiType.getDeepComponentType()
            }

            jvmClassHelper.isCollection(psiType) -> {   //list type
                val iterableType = PsiUtil.extractIterableTypeParameter(psiType, false)
                return when {
                    iterableType != null -> iterableType
                    else -> PsiType.NULL
                }
            }

            else -> {
                return psiType
            }
        }
    }

    override fun getJsonFieldName(psiField: PsiField): String = psiField.name

    override fun getJsonFieldName(psiMethod: PsiMethod): String = psiMethod.name.propertyName()

    override fun getJsonFieldName(accessibleField: AccessibleField): String = accessibleField.name

    open fun getJsonFieldType(accessibleField: AccessibleField): DuckType = accessibleField.type

    open fun getResourceClass(psiClass: PsiClass): PsiClass {
        return psiClass
    }

    @Suppress("UNCHECKED_CAST")
    open fun <T : DuckType> getResourceType(duckType: T): T {
        return when (duckType) {
            is SingleDuckType -> {
                val psiClass = duckType.psiClass()
                val resourceClass = getResourceClass(psiClass)
                if (resourceClass == psiClass) {
                    duckType
                } else {
                    SingleDuckType(resourceClass, duckType.genericInfo) as T
                }
            }

            is ArrayDuckType -> {
                val componentType = duckType.componentType()
                val resourceComponentType = getResourceType(componentType)
                if (resourceComponentType == componentType) {
                    duckType
                } else {
                    ArrayDuckType(componentType) as T
                }
            }

            else -> duckType
        }
    }

    open fun beforeParseClass(psiClass: PsiClass, resolveContext: ResolveContext, fields: MutableMap<String, Any?>) {

    }

    open fun afterParseClass(psiClass: PsiClass, resolveContext: ResolveContext, fields: MutableMap<String, Any?>) {

    }

    //return false to ignore current fieldOrMethod
    open fun beforeParseField(
        accessibleField: AccessibleField,
        resourcePsiClass: ExplicitClass,
        resolveContext: ResolveContext,
        fields: MutableMap<String, Any?>
    ): Boolean {
        return true
    }

    open fun parseField(
        accessibleField: AccessibleField,
        resourcePsiClass: ExplicitClass,
        resolveContext: ResolveContext,
        fields: MutableMap<String, Any?>
    ) {
        var typeObject = doGetTypeObject(accessibleField.jsonFieldType(), accessibleField.psi, resolveContext)
        if (ruleComputer.computer(ClassRuleKeys.JSON_UNWRAPPED, accessibleField) == true) {
            typeObject = typeObject?.upgrade()
        }
        fields[accessibleField.jsonFieldName()] = typeObject
    }

    open fun afterParseField(
        accessibleField: AccessibleField,
        resourcePsiClass: ExplicitClass,
        resolveContext: ResolveContext,
        fields: MutableMap<String, Any?>
    ) {

    }

    open fun onIgnoredParseField(
        accessibleField: AccessibleField,
        resourcePsiClass: ExplicitClass,
        resolveContext: ResolveContext,
        fields: MutableMap<String, Any?>
    ) {

    }

    open fun beforeParseType(
        psiClass: PsiClass,
        duckType: SingleDuckType,
        resolveContext: ResolveContext,
        fields: MutableMap<String, Any?>
    ) {

    }

    open fun afterParseType(
        psiClass: PsiClass,
        duckType: SingleDuckType,
        resolveContext: ResolveContext,
        fields: MutableMap<String, Any?>
    ) {

    }

    inner class SimpleResolveContext(
        override val basePackage: String?,
        override val deep: Int,
        override val option: Int,
        private val shareData: ShareData = ShareData()
    ) : ResolveContext {

        override fun blocked(): Boolean {
            return shareData.blocked()
        }

        override fun blockInfo(): String? {
            return shareData.blockInfo()
        }

        override fun blockResolve(className: String?): Boolean {
            var maxDeep = classRuleConfig?.maxDeep() ?: RuleConstant.DEFAULT_MAX_DEEP
            var maxElements = classRuleConfig?.maxElements() ?: RuleConstant.DEFAULT_MAX_ELEMENTS
            if (basePackage != null && className != null && className.startsWith(basePackage)) {
                maxDeep += 2
                maxElements += 256
            }

            if (deep > maxDeep) {
                shareData.setBlockInfo("deep reached $deep")
                return true
            } else if (elements() > maxElements) {
                shareData.setBlockInfo("elements reached ${elements()}")
                return true
            }
            return false
        }

        override fun next(): ResolveContext {
            return SimpleResolveContext(basePackage, deep + 1, option, shareData)
        }

        override fun copy(): ResolveContext {
            return SimpleResolveContext(basePackage, deep, option)
        }

        override fun incrementElement() {
            shareData.incrementElement()
        }

        override fun elements(): Int {
            return shareData.elements()
        }

        override fun withOption(option: Int): ResolveContext {
            return SimpleResolveContext(basePackage, deep, option)
        }
    }

    class ShareData {

        private var elements: Int = 0

        private var blockInfo: String? = null

        fun incrementElement() {
            ++elements
        }

        fun elements(): Int {
            return elements
        }

        fun blocked(): Boolean {
            return blockInfo != null
        }

        fun setBlockInfo(blockInfo: String) {
            this.blockInfo = blockInfo
        }

        fun blockInfo(): String? {
            return blockInfo
        }
    }

    fun AccessibleField.jsonFieldName(): String = cache("jsonFieldName") {
        getJsonFieldName(this)
    }!!

    fun AccessibleField.jsonFieldType(): DuckType = cache("jsonFieldType") {
        getJsonFieldType(this)
    }!!
}

interface ResolveContext {
    val basePackage: String?

    val deep: Int

    val option: Int

    fun blocked(): Boolean

    fun blockInfo(): String?

    fun blockResolve(className: String?): Boolean

    fun next(): ResolveContext

    fun copy(): ResolveContext

    fun incrementElement()

    fun elements(): Int

    fun withOption(option: Int): ResolveContext
}

/**
 * Default implementation of the [AccessibleField] interface.
 *
 * This class represents an accessible field in a class. It allows for
 * setting and getting the explicit field, getter, and setter representations.
 * It also provides default implementations for the [name], [type], and [psi]
 * properties, which are lazily evaluated based on the available explicit representations.
 *
 * @param field The explicit field representation, or `null` if not available.
 * @param getter The explicit getter method representation, or `null` if not available.
 * @param setter The explicit setter method representation, or `null` if not available.
 * @param extensible The [Extensible] implementation to delegate additional attributes.
 */
class DefaultAccessibleField(
    override val field: ExplicitField? = null,
    override var getter: ExplicitMethod? = null,
    override var setter: ExplicitMethod? = null,
    private val extensible: Extensible = SimpleExtensible()
) : AccessibleField, Extensible by extensible {

    /**
     * Lazily evaluates and returns the name of the field.
     *
     * The name is determined based on the available explicit representations.
     * If none of the explicit representations are available, an [IllegalArgumentException] is thrown.
     */
    override val name: String by lazy {
        field?.name()
            ?: getter?.name()?.getterPropertyName()
            ?: setter?.name()?.setterPropertyName()
            ?: throw IllegalArgumentException("invalid field")
    }

    /**
     * Lazily evaluates and returns the type of the field.
     *
     * The type is determined based on the available explicit representations.
     * If none of the explicit representations are available, an [IllegalArgumentException] is thrown.
     */
    override val type: DuckType by lazy {
        field?.getType()
            ?: getter?.getReturnType()
            ?: setter?.getParameters()?.firstOrNull()?.getType()
            ?: throw IllegalArgumentException("invalid field")
    }

    /**
     * Lazily evaluates and returns the PSI element representing the field.
     *
     * The PSI element is determined based on the available explicit representations.
     * If none of the explicit representations are available, an [IllegalArgumentException] is thrown.
     */
    override val psi: PsiElement by lazy {
        explicitElement.psi()
    }

    override val explicitElement: ExplicitElement<*> by lazy {
        field ?: getter ?: setter ?: throw IllegalArgumentException("invalid field")
    }
}

/**
 * Computes the value of a rule using the provided accessible field.
 *
 * @param ruleKey The rule key to compute the value for.
 * @param accessibleField The accessible field to compute the rule value from.
 * @param contextHandle The optional lambda function to handle the rule computation context.
 * @return The computed value of the rule, or `null` if it cannot be computed.
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any> RuleComputer.computer(
    ruleKey: RuleKey<T>,
    accessibleField: AccessibleField,
    contextHandle: (RuleContext) -> Unit = {}
): T? {
    val map: RuleChain<T> = sequenceOf(accessibleField.field, accessibleField.getter, accessibleField.setter)
        .filterNotNull().map {
            {
                this.computer(ruleKey, it, accessibleField.psi, contextHandle)
            }
        }
    return (ruleKey.mode() as RuleMode<T>).compute(map) as? T
}