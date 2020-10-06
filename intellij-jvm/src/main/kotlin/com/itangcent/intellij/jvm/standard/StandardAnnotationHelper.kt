package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.itangcent.common.utils.*
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.PostConstruct
import com.itangcent.intellij.jvm.AnnotationHelper
import com.itangcent.intellij.jvm.PsiExpressionResolver

@Singleton
open class StandardAnnotationHelper : AnnotationHelper {

    @Inject
    private val actionContext: ActionContext? = null

    @Inject
    private val psiExpressionResolver: PsiExpressionResolver? = null

    @PostConstruct
    private fun init() {
        psiExpressionResolver!!
            .registerExpressionResolver(PsiAnnotation::class) {
                return@registerExpressionResolver annToMap(it)
            }
    }

    override fun hasAnn(psiElement: PsiElement?, annName: String): Boolean {
        return findAnn(psiElement, annName) != null
    }

    override fun findAnnMap(psiElement: PsiElement?, annName: String): Map<String, Any?>? {
        val psiAnn = findAnn(psiElement, annName) ?: return null
        return actionContext!!.callInReadUI { annToMap(psiAnn) }
    }

    override fun findAnnMaps(psiElement: PsiElement?, annName: String): List<Map<String, Any?>>? {
        val psiAnn = findAnns(psiElement, annName) ?: return null
        return actionContext!!.callInReadUI { psiAnn.map { annToMap(it) } }
    }

    protected fun annToMap(psiAnn: PsiAnnotation): Map<String, Any?> {
        val map: LinkedHashMap<String, Any?> = LinkedHashMap()
        psiAnn.parameterList.attributes.stream()
            .forEach { attr ->
                map[attr.name ?: "value"] = attr.value?.let { psiExpressionResolver!!.process(it) }
            }

        return map
    }

    override fun findAttr(psiElement: PsiElement?, annName: String): Any? {
        return findAttr(psiElement, annName, "value")
    }

    override fun findAttr(psiElement: PsiElement?, annName: String, vararg attrs: String): Any? {
        val ann = findAnn(psiElement, annName) ?: return null
        return attrs
            .mapNotNull { ann.findAttributeValue(it) }
            .mapNotNull { psiExpressionResolver!!.process(it) }
            .firstOrNull()
    }

    override fun findAttrAsString(psiElement: PsiElement?, annName: String): String? {
        return findAttrAsString(psiElement, annName, "value")
    }

    override fun findAttrAsString(psiElement: PsiElement?, annName: String, vararg attrs: String): String? {
        val ann = findAnn(psiElement, annName) ?: return null
        return actionContext!!.callInReadUI {
            return@callInReadUI attrs
                .stream()
                .mapNotNull { ann.findAttributeValue(it) }
                .mapNotNull { psiExpressionResolver!!.process(it) }
                .mapNotNull { tinyAnnStr(it) }
                .longest()
        }
    }

    private fun findAnn(psiElement: PsiElement?, annName: String): PsiAnnotation? {
        return findAnnotations(psiElement)
            ?.let { annotations -> annotations.firstOrNull { it.qualifiedName == annName } }
    }

    private fun findAnns(psiElement: PsiElement?, annName: String): List<PsiAnnotation>? {
        return findAnnotations(psiElement)
            ?.let { annotations ->
                annotations.filter {
                    it.qualifiedName == annName
                }
            }
    }

    private fun findAnnotations(psiElement: PsiElement?): Array<out PsiAnnotation>? {
        return psiElement.cast(PsiAnnotationOwner::class)?.annotations
            ?: psiElement.cast(PsiModifierListOwner::class)?.annotations
    }

    fun tinyAnnStr(annStr: Any?): String? {
        return when (annStr) {
            null -> null
            is Array<*> -> annStr.joinToString(separator = "\n")
            is Collection<*> -> annStr.joinToString(separator = "\n")
            is String -> annStr
            else -> GsonUtils.toJson(annStr)
        }
    }

    /**
     * clean the str of param in annotaion
     * 1.remove {}  e.g.{*}->*
     * 2.remove ""  e.g."*"->*
     */
    fun tinyAnnStr(annStr: String?): String? {
        if (annStr == null) return null
        var str = annStr.trim()
        while (true) {
            if (str.startsWith("{")) {
                str = str.trim('{', '}')
                continue
            }
            if (str.startsWith("\"") || str.endsWith("\"")) {
                str = str.trim('\"')
                continue
            }
            break
        }
        return str
    }
}