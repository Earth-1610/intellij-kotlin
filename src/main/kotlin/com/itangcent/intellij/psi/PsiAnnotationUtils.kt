package com.itangcent.intellij.psi

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierListOwner
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.logger.Logger

object PsiAnnotationUtils {

    fun findAnn(ele: PsiModifierListOwner, annName: String): PsiAnnotation? {
        val annotations = ele.annotations
        val requestMappingAnn = annotations.firstOrNull { it.qualifiedName == annName }
        if (requestMappingAnn != null) return requestMappingAnn
        return null
    }

    fun findAttr(psiAnnotation: PsiAnnotation?, vararg attrs: String): String? {
        if (psiAnnotation != null) {
            return attrs
                .mapNotNull { psiAnnotation.findAttributeValue(it) }
                .filter { it.textLength > 0 }
                .map { tinyAnnStr(it.text) }
                .firstOrNull { !it.isNullOrBlank() }
        }
        return null
    }

    fun findAttr(ele: PsiModifierListOwner, annName: String, vararg attrs: String): String? {
        val ann = findAnn(ele, annName) ?: return null
        return attrs
            .mapNotNull { ann.findAttributeValue(it) }
            .filter { it.textLength > 0 }
            .map { tinyAnnStr(it.text) }
            .firstOrNull { !it.isNullOrBlank() }
    }

    private val logger: Logger = ActionContext.local()

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
