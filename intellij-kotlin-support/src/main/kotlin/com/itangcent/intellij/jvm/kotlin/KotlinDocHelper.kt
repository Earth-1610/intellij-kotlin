package com.itangcent.intellij.jvm.kotlin

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.PsiElement
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.standard.StandardDocHelper
import org.apache.commons.lang3.StringUtils
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtDeclaration
import java.util.*

@Singleton
class KotlinDocHelper : StandardDocHelper() {

    @Inject
    private val actionContext: ActionContext? = null

    override fun hasTag(psiElement: PsiElement?, tag: String?): Boolean {

        if (psiElement == null || tag == null) {
            return false
        }

        if (!KtPsiUtils.isKtPsiInst(psiElement)) {
            return super.hasTag(psiElement, tag)
        }

        val kDoc = findKDoc(psiElement)
        if (kDoc != null) {

            val kTag = KDocKnownTag.findByTagName(tag)
            if (kTag != null) {
                if (actionContext!!.callInReadUI {
                        kDoc.findSectionByTag(kTag) != null
                    } == true) {
                    return true
                }
            }

            return actionContext!!.callInReadUI {
                kDoc.children
                    .filter { it is KDocSection }
                    .map { it as KDocSection }
                    .flatMap { it.findTagsByName(tag) }
                    .any()
            } ?: false
        }

        return false
    }

    override fun findDocByTag(psiElement: PsiElement?, tag: String?): String? {

        if (psiElement == null || tag == null) {
            return null
        }

        if (!KtPsiUtils.isKtPsiInst(psiElement)) {
            return super.findDocByTag(psiElement, tag)
        }

        val kDoc = findKDoc(psiElement)
        if (kDoc != null) {
            val kTag = KDocKnownTag.findByTagName(tag)
            if (kTag != null) {
                val content = actionContext!!.callInReadUI {
                    kDoc.findSectionByTag(kTag)?.getContent()
                }
                if (content != null) {
                    return content
                }
            }

            return actionContext!!.callInReadUI {
                kDoc.children
                    .filter { it is KDocSection }
                    .map { it as KDocSection }
                    .flatMap { it.findTagsByName(tag) }
                    .map { it.contentOrLink() }
                    .firstOrNull()
            }
        }

        return null
    }

    override fun findDocsByTag(psiElement: PsiElement?, tag: String?): List<String>? {

        if (psiElement == null || tag == null) {
            return null
        }

        if (!KtPsiUtils.isKtPsiInst(psiElement)) {
            return super.findDocsByTag(psiElement, tag)
        }

        val kDoc = findKDoc(psiElement)
        if (kDoc != null) {
            return actionContext!!.callInReadUI {
                kDoc.children
                    .filter { it is KDocSection }
                    .map { it as KDocSection }
                    .flatMap { it.findTagsByName(tag) }
                    .mapNotNull { it.contentOrLink() }
                    .toList()
            }
        }

        return null
    }

    override fun findDocsByTagAndName(psiElement: PsiElement?, tag: String, name: String): String? {


        if (psiElement == null) {
            return null
        }

        if (!KtPsiUtils.isKtPsiInst(psiElement)) {
            return super.findDocsByTagAndName(psiElement, tag, name)
        }

        val kDoc = findKDoc(psiElement)
        if (kDoc != null) {
            val kTag = KDocKnownTag.findByTagName(tag)
            if (kTag != null) {
                val content = actionContext!!.callInReadUI {
                    kDoc.findSectionByTag(kTag, name)?.getContent()
                }
                if (content != null) {
                    return content
                }
            }

            return actionContext!!.callInReadUI {
                kDoc.children
                    .filter { it is KDocSection }
                    .map { it as KDocSection }
                    .flatMap { it.findTagsByName(tag) }
                    .filter { it.getSubjectName() == name }
                    .map { it.contentOrLink() }
                    .firstOrNull()
            }
        }

        return null
    }

    override fun getAttrOfDocComment(psiElement: PsiElement?): String? {

        if (psiElement == null) {
            return null
        }

        if (!KtPsiUtils.isKtPsiInst(psiElement)) {
            return super.getAttrOfDocComment(psiElement)
        }

        val kDoc = findKDoc(psiElement)
        if (kDoc != null) {
            return actionContext!!.callInReadUI {
                kDoc.getDefaultSection().getContent()
            }
        }

        return null
    }

    override fun getSubTagMapOfDocComment(psiElement: PsiElement?, tag: String): Map<String, String?> {

        if (psiElement == null) {
            return Collections.emptyMap()
        }

        if (!KtPsiUtils.isKtPsiInst(psiElement)) {
            return super.getSubTagMapOfDocComment(psiElement, tag)
        }

        val kDoc = findKDoc(psiElement)
        if (kDoc != null) {
            return actionContext!!.callInReadUI {
                val tagMap: HashMap<String, String?> = HashMap()
                kDoc.children
                    .filter { it is KDocSection }
                    .map { it as KDocSection }
                    .flatMap { it.findTagsByName(tag) }
                    .forEach { tagMap[it.getSubjectName() ?: ""] = it.contentOrLink() }
                return@callInReadUI tagMap
            } ?: Collections.emptyMap()
        }

        return Collections.emptyMap()

    }

    override fun getTagMapOfDocComment(psiElement: PsiElement?): Map<String, String?> {

        if (psiElement == null) {
            return Collections.emptyMap()
        }

        if (!KtPsiUtils.isKtPsiInst(psiElement)) {
            return super.getTagMapOfDocComment(psiElement)
        }

        val kDoc = findKDoc(psiElement)
        if (kDoc != null) {
            return actionContext!!.callInReadUI {
                val tagMap: HashMap<String, String?> = HashMap()
                kDoc.children
                    .filter { it is KDocSection }
                    .map { it as KDocSection }
                    .flatMap { it.children.toList() }
                    .filter { it is KDocTag }
                    .map { it as KDocTag }
                    .forEach {
                        tagMap[it.name ?: ""] = it.contentOrLink()
                    }
                return@callInReadUI tagMap
            } ?: Collections.emptyMap()
        }

        return Collections.emptyMap()
    }

    private fun findKDoc(psiElement: PsiElement): KDoc? {

        //kotlin doc
        if (psiElement is KtLightElement<*, *>) {
            return actionContext!!.callInReadUI {
                val kotlinOrigin = (psiElement).kotlinOrigin ?: return@callInReadUI null
                if (kotlinOrigin is KtDeclaration) {
                    return@callInReadUI kotlinOrigin.docComment
                }
                return@callInReadUI null
            }
        }

        if (psiElement is KtDeclaration) {
            return actionContext!!.callInReadUI { psiElement.docComment }
        }

        return null
    }

    private fun KDocTag?.contentOrLink(): String? {
        if (this == null) {
            return null
        }
        val content = this.getContent()
        if (StringUtils.isNotBlank(content)) {
            return content
        }

        var link = this.getSubjectLink()?.text

        if (StringUtils.isNotBlank(link)) {
            return link
        }

        return null
    }
}