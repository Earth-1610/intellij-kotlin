package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationOwner
import com.intellij.psi.PsiMember
import com.intellij.util.containers.stream
import com.itangcent.common.utils.cast
import com.itangcent.intellij.jvm.AnnotationHelper
import com.itangcent.intellij.jvm.PsiResolver

@Singleton
class StandardAnnotationHelper : AnnotationHelper {

    @Inject
    val psiResolver: PsiResolver? = null

    override fun findAttr(psiMember: PsiMember?, annName: String): String? {
        return findAttr(psiMember, annName, "value")
    }

    override fun hasAnn(psiMember: PsiMember?, annName: String): Boolean {
        return findAnn(psiMember, annName) != null
    }

    override fun findAnnMap(psiMember: PsiMember?, annName: String): Map<String, String?>? {
        val psiAnn = findAnn(psiMember, annName) ?: return null
        val map: LinkedHashMap<String, String?> = LinkedHashMap()
        psiAnn.parameterList.attributes.stream()
            .forEach { attr ->
                attr.name?.let { map[it] = tinyAnnStr(psiResolver?.resolveRefText(attr.value)) }
            }

        return map
    }

    fun findAttr(psiAnnotation: PsiAnnotation?, vararg attrs: String): String? {
        if (psiAnnotation != null) {
            return attrs
                .mapNotNull { psiAnnotation.findAttributeValue(it) }
                .filter { it.textLength > 0 }
                .map { tinyAnnStr(psiResolver!!.resolveRefText(it)) }
                .firstOrNull { !it.isNullOrBlank() }
        }
        return null
    }

    override fun findAttr(psiMember: PsiMember?, annName: String, vararg attrs: String): String? {
        val ann = findAnn(psiMember, annName) ?: return null
        return attrs
            .mapNotNull { ann.findAttributeValue(it) }
            .map { psiResolver!!.resolveRefText(it) }
            .filter { !it.isNullOrBlank() }
            .map { tinyAnnStr(it) }
            .firstOrNull { !it.isNullOrBlank() }
    }

    private fun findAnn(psiMember: PsiMember?, annName: String): PsiAnnotation? {
        return psiMember.cast(PsiAnnotationOwner::class)?.annotations
            ?.let { annotations -> annotations.firstOrNull { it.qualifiedName == annName } }
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