package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.java.stubs.impl.PsiClassStubImpl
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.util.containers.isNullOrEmpty
import com.intellij.util.containers.stream
import com.itangcent.intellij.util.DocCommentUtils
import com.itangcent.intellij.util.KV
import com.itangcent.intellij.util.reduceSafely
import org.apache.commons.lang3.StringUtils

open class DefaultPsiClassHelper : AbstractPsiClassHelper() {

    @Inject(optional = true)
    private val classRuleConfig: ClassRuleConfig? = null

    @Inject
    private val project: Project? = null

    open fun getAttrOfField(field: PsiField): String? {

        var result: String? = null

        val attrInDoc = DocCommentUtils.getAttrOfDocComment(field.docComment)
        if (StringUtils.isNotBlank(attrInDoc)) {
            result = (result ?: "") + attrInDoc
        }

        if (classRuleConfig != null) {
            val docByRule = classRuleConfig.findDoc(field)

            if (StringUtils.isNotBlank(docByRule)) {
                result = (result ?: "") + docByRule
            }
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
    fun resolveDeprecatedDoc(field: PsiField, comment: HashMap<String, Any?>) {
        val deprecatedInfo = DocCommentUtils.findDocsByTag(field.docComment, "deprecated")
        if (deprecatedInfo != null) {
            val fieldName = field.name
            val oldComment = comment[fieldName]
            if (oldComment == null) {
                comment[fieldName] = "[字段已废弃]$deprecatedInfo"
            } else {
                comment[fieldName] = "$oldComment\n[字段已废弃]$deprecatedInfo"
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun resolveSeeDoc(field: PsiField, comment: HashMap<String, Any?>) {
        val sees = getSees(field)
        if (sees.isNullOrEmpty()) return

        val options: ArrayList<HashMap<String, Any?>> = ArrayList()

        sees!!.forEach { see ->
            resolveEnumOrStatic(see, field, field.name)?.let { options.addAll(it) }
        }

        if (options.isNotEmpty()) {
            comment["${field.name}@options"] = options
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun resolveEnumOrStatic(
        classNameWithProperty: String,
        psiMember: PsiMember,
        defaultPropertyName: String
    ): ArrayList<HashMap<String, Any?>>? {
        val options: ArrayList<HashMap<String, Any?>> = ArrayList()
        var clsName: String? = null
        var property: String? = null
        var cls: PsiClass? = null

        if (classNameWithProperty.contains("#")) {
            clsName = classNameWithProperty.substringBefore("#")
            property = classNameWithProperty.substringAfter("#").trim()
        } else {
            clsName = classNameWithProperty
        }
        cls = resolveClass(clsName, psiMember)
        if (cls == null) return null

        if (cls.isEnum) {
            val enumConstants = parseEnumConstant(cls)

            if (property == null) {
                property = defaultPropertyName
            }

            for (enumConstant in enumConstants) {
                val mappedVal = (enumConstant["params"] as HashMap<String, Any?>?)
                    ?.get(property) ?: continue

                var desc = enumConstant["desc"]
                if (desc == null) {
                    desc = (enumConstant["params"] as HashMap<String, Any?>?)!!
                        .filterKeys { k -> k != property }
                        .map { entry -> entry.value.toString() }
                        .reduceSafely { s1, s2 -> s1 + " " + s2 }
                        ?.trim()
                }
                if (desc == null || (desc is String && desc.isBlank())) {
                    desc = enumConstant["name"]
                }
                options.add(
                    KV.create<String, Any?>()
                        .set("value", mappedVal)
                        .set("desc", desc)
                )

            }

        } else {
            val constants = parseStaticFields(cls)

            if (property.isNullOrBlank()) {
                for (constant in constants) {
                    val mappedVal = constant["value"]
                    val desc = constant["desc"] ?: constant["name"]
                    options.add(
                        KV.create<String, Any?>()
                            .set("value", mappedVal)
                            .set("desc", desc)
                    )
                }
            } else {
                for (constant in constants) {
                    val name = constant["name"] as String

                    if (name != property) continue
                    val mappedVal = constant["value"]
                    val desc = constant["desc"] ?: constant["name"]

                    options.add(
                        KV.create<String, Any?>()
                            .set("value", mappedVal)
                            .set("desc", desc)
                    )
                    break
                }
            }
        }
        return options
    }

    fun resolveClass(className: String, psiMember: PsiMember): PsiClass? {
        return when {
            className.contains(".") -> tmTypeHelper!!.findClass(className, psiMember)
            else -> getContainingClass(psiMember)?.let { resolveClassFromImport(it, className) }
                ?: tmTypeHelper!!.findClass(className, psiMember)
        }
    }

    fun getContainingClass(psiMember: PsiMember): PsiClass? {
        psiMember.containingClass?.let { return it }
        if (psiMember is PsiClass) return psiMember
        return null
    }

    private fun resolveClassFromImport(psiClass: PsiClass, clsName: String): PsiClass? {

        val imports = PsiTreeUtil.findChildrenOfType(psiClass.context, PsiImportStatement::class.java)

        var cls = imports
            .mapNotNull { it.qualifiedName }
            .firstOrNull { it.endsWith(".$clsName") }
            ?.let { tmTypeHelper!!.findClass(it, psiClass) }
        if (cls != null) {
            return cls
        }

        val defaultPackage = psiClass.qualifiedName!!.substringBeforeLast(".")
        cls = tmTypeHelper!!.findClass("$defaultPackage.$clsName", psiClass)
        if (cls != null) {
            return cls
        }
        cls = imports
            .mapNotNull { it.qualifiedName }
            .filter { it.endsWith(".*") }
            .map { it -> it.removeSuffix("*") + clsName }
            .map { tmTypeHelper.findClass(it, psiClass) }
            .firstOrNull()

        return cls
    }

    private fun getSees(field: PsiJavaDocumentedElement): List<String>? {
        val docComment = field.docComment ?: return null

        val sees: ArrayList<String> = ArrayList()
        docComment.findTagsByName("see")
            .map { psiDocTag ->
                psiDocTag.dataElements.stream()
                    .map { it.text }
                    .filter { it != null }
                    .reduce { s1, s2 -> s1 + s2 }
                    .orElse(null)
            }
            .forEach { it?.let { sees.add(it) } }
        if (sees.isEmpty()) return null
        return sees
    }

    private val staticResolvedInfo: HashMap<PsiClass, List<Map<String, Any?>>> = HashMap()

    fun parseStaticFields(psiClass: PsiClass): List<Map<String, Any?>> {
        val psiClass = getResourceClass(psiClass)
        if (staticResolvedInfo.containsKey(psiClass)) {
            return staticResolvedInfo[psiClass]!!
        }
        val res = ArrayList<Map<String, Any?>>()
        val checkModifier: Set<String> =
            if (psiClass.isInterface) {
                staticFinalFieldModifiers
            } else {
                publicStaticFinalFieldModifiers
            }
        for (field in psiClass.allFields) {

            if (!hasAnyModify(field, checkModifier)) {
                continue
            }

            val value = field.computeConstantValue() ?: continue

            val constant = HashMap<String, Any?>()
            constant.put("name", field.name)
            constant.put("value", value.toString())
            constant.put("desc", getAttrOfField(field))
            res.add(constant)
        }
        staticResolvedInfo[psiClass] = res
        return res
    }

    fun parseEnumConstant(psiClass: PsiClass): List<Map<String, Any?>> {
        val psiClass = getResourceClass(psiClass)
        if (!psiClass.isEnum) return ArrayList()

        if (staticResolvedInfo.containsKey(psiClass)) {
            return staticResolvedInfo[psiClass]!!
        }

        val res = ArrayList<Map<String, Any?>>()
        for (field in psiClass.allFields) {

            val value = field.computeConstantValue() ?: continue

            if (value !is PsiEnumConstant) continue

            val constant = HashMap<String, Any?>()
            val params = HashMap<String, Any?>()
            val construct = value.resolveConstructor()
            val expressions = value.argumentList?.expressions
            val parameters = construct?.parameterList?.parameters
            if (expressions != null && parameters != null && parameters.isNotEmpty()) {
                if (parameters.last().isVarArgs) {

                    for (index in 0 until parameters.size - 1) {
                        params[parameters[index].name!!] = resolveExpr(expressions[index])
                    }
                    try {
                        //resolve varArgs
                        val lastVarArgParam: ArrayList<Any?> = ArrayList(1)
                        params[parameters[parameters.size - 1].name!!] = lastVarArgParam
                        for (index in parameters.size - 1..expressions.size) {
                            lastVarArgParam.add(resolveExpr(expressions[index]))
                        }
                    } catch (e: Throwable) {
                    }
                }

                for ((index, parameter) in parameters.withIndex()) {
                    try {
                        params[parameter.name!!] = resolveExpr(expressions[index])
                    } catch (e: Throwable) {
                    }
                }
            }
            constant["params"] = params
            constant["name"] = field.name
            constant["desc"] = getAttrOfField(field)
            res.add(constant)
        }

        staticResolvedInfo[psiClass] = res
        return res
    }

    private fun resolveExpr(psiExpression: PsiExpression): Any? {
        if (psiExpression is PsiLiteralExpression) {
            return psiExpression.value
        } else if (psiExpression is PsiReferenceExpression) {
            val value = psiExpression.resolve()
            if (value is PsiField) {
                val constantValue = value.computeConstantValue()
                if (constantValue != null) return constantValue
            }
        } else if (psiExpression is PsiField) {
            return psiExpression.name
        }
        return psiExpression.text
    }

    private val castCache: HashMap<String, PsiType?> = HashMap()

    override fun tryCastTo(psiType: PsiType, context: PsiElement): PsiType {
        if (classRuleConfig == null) return psiType

        val canonicalText = psiType.canonicalText

        if (castCache.contains(canonicalText)) {
            return castCache[canonicalText] ?: psiType
        } else {
            val convertTo = classRuleConfig.tryConvert(canonicalText)

            val convertToType: PsiType
            convertToType = when (convertTo) {
                null -> psiType
                "null" -> PsiType.NULL
                else -> {
                    val cls = tmTypeHelper!!.findClass(convertTo, context)
                    if (cls != null) {
                        PsiTypesUtil.getClassType(cls)
                    } else {
                        psiType
                    }
                }
            }
            castCache[canonicalText] = convertToType
            return convertToType
        }
    }

    override fun isNormalType(typeName: String): Boolean {
        return normalTypes.containsKey(classRuleConfig?.tryConvert(typeName) ?: typeName)
    }

    override fun getDefaultValue(typeName: String): Any? {
        return normalTypes[classRuleConfig?.tryConvert(typeName) ?: typeName]
    }

    override fun getJsonFieldName(psiField: PsiField): String {
        if (classRuleConfig != null) {
            val nameByRule = classRuleConfig.getFieldName(psiField)
            if (!nameByRule.isNullOrBlank()) {
                return nameByRule
            }
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

    private fun isLocalClass(psiClass: PsiClass): Boolean {
        if (psiClass is StubBasedPsiElement<*>) {
            val stub = psiClass.stub
            return stub is PsiClassStubImpl<*> && stub.isLocalClassInner
        }
        return false
    }

    override fun beforeParseFieldOrMethod(
        fieldName: String,
        fieldType: PsiType,
        fieldOrMethod: PsiElement,
        resourcePsiClass: PsiClass,
        duckType: SingleDuckType,
        option: Int,
        kv: KV<String, Any?>
    ): Boolean {

        //doc comment
        if (JsonOption.needComment(option)) {
            var commentKV: KV<String, Any?>? = kv["@comment"] as KV<String, Any?>?
            if (commentKV == null) {
                commentKV = KV.create()
            }
            if (fieldOrMethod is PsiField) {
                val field: PsiField = fieldOrMethod
                commentKV[fieldName] = getAttrOfField(field)
                resolveDeprecatedDoc(field, commentKV)
                resolveSeeDoc(field, commentKV)
            } else if (fieldOrMethod is PsiMethod) {
                val attrInDoc = DocCommentUtils.getAttrOfDocComment(fieldOrMethod.docComment)
                if (StringUtils.isNotBlank(attrInDoc)) {
                    commentKV[fieldName] = attrInDoc
                }
            }
        }
        return true
    }

    override fun beforeParseFieldOrMethod(
        fieldName: String,
        fieldType: PsiType,
        fieldOrMethod: PsiElement,
        resourcePsiClass: PsiClass,
        option: Int,
        kv: KV<String, Any?>
    ): Boolean {

        //doc comment
        if (JsonOption.needComment(option)) {
            var commentKV: KV<String, Any?>? = kv["@comment"] as KV<String, Any?>?
            if (commentKV == null) {
                commentKV = KV.create()
            }
            if (fieldOrMethod is PsiField) {
                val field: PsiField = fieldOrMethod
                commentKV[fieldName] = getAttrOfField(field)
                resolveDeprecatedDoc(field, commentKV)
                resolveSeeDoc(field, commentKV)
            } else if (fieldOrMethod is PsiMethod) {
                val attrInDoc = DocCommentUtils.getAttrOfDocComment(fieldOrMethod.docComment)
                if (StringUtils.isNotBlank(attrInDoc)) {
                    commentKV[fieldName] = attrInDoc
                }
            }
        }
        return true
    }
}
