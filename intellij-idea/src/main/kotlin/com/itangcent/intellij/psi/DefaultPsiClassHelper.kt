package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.java.stubs.impl.PsiClassStubImpl
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.util.containers.isNullOrEmpty
import com.itangcent.intellij.util.KV
import org.apache.commons.lang3.StringUtils

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

        val options: ArrayList<HashMap<String, Any?>> = ArrayList()

        sees!!.forEach { see ->
            resolveEnumOrStatic(see, field, field.name)?.let { options.addAll(it) }
        }

        if (options.isNotEmpty()) {
            comment["${field.name}@options"] = options
        }
    }

    protected open fun getSees(field: PsiJavaDocumentedElement): List<String>? {
        val sees: List<String>? = docHelper!!.findDocsByTag(field, "see")
        if (sees.isNullOrEmpty()) {
            return null
        }
        return sees
    }

    protected val castCache: HashMap<String, PsiType?> = HashMap()

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
        return jvmClassHelper!!.isNormalType(classRuleConfig?.tryConvert(typeName) ?: typeName)
    }

    override fun getDefaultValue(typeName: String): Any? {
        return jvmClassHelper!!.getDefaultValue(classRuleConfig?.tryConvert(typeName) ?: typeName)
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
