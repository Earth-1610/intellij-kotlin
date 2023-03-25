package com.itangcent.intellij.jvm.kotlin

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.javadoc.PsiDocComment
import com.itangcent.common.utils.appendln
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.standard.BLOCK_COMMENT_REGEX
import com.itangcent.intellij.jvm.standard.COMMENT_PREFIX
import com.itangcent.intellij.jvm.standard.StandardDocHelper
import org.apache.commons.lang3.StringUtils
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import java.util.*

@Singleton
class KotlinDocHelper : StandardDocHelper() {

    @Inject
    private val actionContext: ActionContext? = null

    override fun getEolComment(psiElement: PsiElement): String? {
        if (!KtPsiUtils.isKtPsiInst(psiElement)) {
            throw NotImplementedError()
        }

        //text maybe null
        val text = psiElement.text?.trim() ?: return null

        if (text.startsWith(COMMENT_PREFIX)) {
            return text.lines()
                .map { it.trim() }
                .filter { it.startsWith(COMMENT_PREFIX) }
                .joinToString("\n") {
                    it.removePrefix(COMMENT_PREFIX)
                }
        }

        val ktProperty = psiElement.navigationElement
        if (ktProperty != null) {
            ktProperty.firstChild.nextSiblings()
                .findEolComment()
                ?.let { return it }

            ktProperty.prevSiblings()
                .findBlockComment()
                ?.let {
                    return it
                }
        }

        return null
    }

    override fun getDocCommentContent(docComment: PsiDocComment): String? {
        if (!KtPsiUtils.isKtPsiInst(docComment)) {
            throw NotImplementedError()
        }

        return super.getDocCommentContent(docComment)
    }

    override fun hasTag(psiElement: PsiElement?, tag: String?): Boolean {

        if (psiElement == null || tag == null) {
            return false
        }

        if (!KtPsiUtils.isKtPsiInst(psiElement)) {
            return false
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
                    .filterIsInstance<KDocSection>()
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
            return null
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
                    .filterIsInstance<KDocSection>()
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
            return null
        }

        val kDoc = findKDoc(psiElement)
        if (kDoc != null) {
            return actionContext!!.callInReadUI {
                kDoc.children
                    .filterIsInstance<KDocSection>()
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
            return null
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
                    .filterIsInstance<KDocSection>()
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
            return null
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
            return emptyMap()
        }

        val kDoc = findKDoc(psiElement)

        if (kDoc != null) {
            return actionContext!!.callInReadUI {
                val tagMap: HashMap<String, String?> = HashMap()
                kDoc.children
                    .filterIsInstance<KDocSection>()
                    .flatMap { it.findTagsByName(tag) }
                    .forEach { tagMap[it.getSubjectName() ?: ""] = it.contentOrLink() }
                return@callInReadUI tagMap
            } ?: Collections.emptyMap()
        }

        return emptyMap()

    }

    override fun getTagMapOfDocComment(psiElement: PsiElement?): Map<String, String?> {

        if (psiElement == null) {
            return Collections.emptyMap()
        }

        if (!KtPsiUtils.isKtPsiInst(psiElement)) {
            return emptyMap()
        }

        val kDoc = findKDoc(psiElement)
        if (kDoc != null) {
            return actionContext!!.callInReadUI {
                val tagMap: HashMap<String, String?> = HashMap()
                kDoc.children
                    .filterIsInstance<KDocSection>()
                    .flatMap { it.children.toList() }
                    .filterIsInstance<KDocTag>()
                    .forEach {
                        tagMap[it.name ?: ""] = it.contentOrLink()
                    }
                return@callInReadUI tagMap
            } ?: Collections.emptyMap()
        }

        return emptyMap()
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

        val link = this.getSubjectLink()?.text

        if (StringUtils.isNotBlank(link)) {
            return link
        }

        return null
    }

    override fun getAttrOfField(field: PsiField): String? {

        if (!KtPsiUtils.isKtPsiInst(field)) {
            throw NotImplementedError()
        }

        val containingClass = field.containingClass ?: return super.getAttrOfField(field)
        val attrFromPropertyTag = containingClass.constructors.firstNotNullOfOrNull {
            this.findDocsByTagAndName(it, "property", field.name)
        } ?: this.findDocsByTagAndName(containingClass, "property", field.name)

        return attrFromPropertyTag.appendln(super.getAttrOfField(field))
    }

    override fun PsiComment.eolComment(): String? {
        if (this.tokenType == KtTokens.EOL_COMMENT) {
            return text.trim().removePrefix(COMMENT_PREFIX).trimStart()
        }
        return null
    }

    override fun PsiComment.blockComment(): String? {
        if (this.tokenType == KtTokens.BLOCK_COMMENT) {
            return BLOCK_COMMENT_REGEX.find(text)?.let { it.groupValues[1].trim() }
        }
        return null
    }
}
