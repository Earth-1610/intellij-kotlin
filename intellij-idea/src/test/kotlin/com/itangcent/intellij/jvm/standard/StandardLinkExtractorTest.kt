package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.itangcent.testFramework.ContextLightCodeInsightFixtureTestCase

/**
 * Test case of [StandardLinkExtractor]
 */
internal class StandardLinkExtractorTest : ContextLightCodeInsightFixtureTestCase() {

    @Inject
    private lateinit var standardLinkExtractor: StandardLinkExtractor

    @Inject
    private lateinit var standardDocHelper: StandardDocHelper

    private lateinit var linkTestClass: PsiClass

    override fun beforeBind() {
        super.beforeBind()
        linkTestClass = loadClass("cases/LinkTest.java")!!
    }

    fun testFindLinkWithSimpleLinksJep467() {
        val method = linkTestClass.findMethodsByName("testSimpleLinksJep467", false)[0]
        val doc = method.getComments()
        val result = standardLinkExtractor.findLink(doc, method) { "[$it](https://itangcent.com/java/$it)" }
        assertEquals(
            """
            Test simple links with JEP 467 style
            See [UserInfo](https://itangcent.com/java/UserInfo) for user information [UserInfo](https://itangcent.com/java/UserInfo)
            See [Model](https://itangcent.com/java/Model) for model information [Model](https://itangcent.com/java/Model)
        """.trimIndent(), result
        )
    }

    fun testFindLinkWithMethodReferencesJep467() {
        val method = linkTestClass.findMethodsByName("testMethodReferencesJep467", false)[0]
        val doc = method.getComments()
        val result = standardLinkExtractor.findLink(doc, method) { "[$it](https://itangcent.com/java/$it)" }
        assertEquals(
            """
            Test method references with JEP 467 style
            See [#equals](https://itangcent.com/java/#equals) for equality comparison [#equals](https://itangcent.com/java/#equals)
            See [#hashCode](https://itangcent.com/java/#hashCode) for hash code generation [#hashCode](https://itangcent.com/java/#hashCode)
        """.trimIndent(), result
        )
    }

    fun testFindLinkWithMixedContentJep467() {
        val method = linkTestClass.findMethodsByName("testMixedContentJep467", false)[0]
        val doc = method.getComments()
        val result = standardLinkExtractor.findLink(doc, method) { "[$it](https://itangcent.com/java/$it)" }
        assertEquals(
            """
            Test mixed content with JEP 467 style
            The [UserInfo](https://itangcent.com/java/UserInfo) class provides user information [UserInfo](https://itangcent.com/java/UserInfo).
            See [#equals](https://itangcent.com/java/#equals) for equality comparison [#equals](https://itangcent.com/java/#equals).
            See [Model](https://itangcent.com/java/Model) for model information [Model](https://itangcent.com/java/Model).
        """.trimIndent(), result
        )
    }

    fun testFindLinkWithUnresolvedLinksJep467() {
        val method = linkTestClass.findMethodsByName("testUnresolvedLinksJep467", false)[0]
        val doc = method.getComments()
        val result = standardLinkExtractor.findLink(doc, method) { "[$it](https://itangcent.com/java/$it)" }
        assertEquals(
            """
            Test unresolved links with JEP 467 style
            See [UnknownClass](https://itangcent.com/java/UnknownClass) for unknown information [UnknownClass](https://itangcent.com/java/UnknownClass)
            See [#unknownMethod](https://itangcent.com/java/#unknownMethod) for unknown method [#unknownMethod](https://itangcent.com/java/#unknownMethod)
        """.trimIndent(), result
        )
    }

    fun testFindLinkWithSimpleLinksJavadoc() {
        val method = linkTestClass.findMethodsByName("testSimpleLinksJavadoc", false)[0]!!
        val doc = method.getComments()
        val result = standardLinkExtractor.findLink(doc, method) { "[$it](https://itangcent.com/java/$it)" }
        assertEquals(
            """
            Test simple links with Javadoc style
            See [UserInfo](https://itangcent.com/java/UserInfo) for user information[UserInfo](https://itangcent.com/java/UserInfo)
            See [Model](https://itangcent.com/java/Model) for model information[Model](https://itangcent.com/java/Model)
        """.trimIndent(), result
        )
    }

    fun testFindLinkWithMethodReferencesJavadoc() {
        val method = linkTestClass.findMethodsByName("testMethodReferencesJavadoc", false)[0]
        val doc = method.getComments()
        val result = standardLinkExtractor.findLink(doc, method) { "[$it](https://itangcent.com/java/$it)" }
        assertEquals(
            """
            Test method references with Javadoc style
            See [#equals](https://itangcent.com/java/#equals) for equality comparison[#equals](https://itangcent.com/java/#equals)
            See [#hashCode](https://itangcent.com/java/#hashCode) for hash code generation[#hashCode](https://itangcent.com/java/#hashCode)
        """.trimIndent(), result
        )
    }

    fun testFindLinkWithMixedContentJavadoc() {
        val method = linkTestClass.findMethodsByName("testMixedContentJavadoc", false)[0]
        val doc = method.getComments()
        val result = standardLinkExtractor.findLink(doc, method) { "[$it](https://itangcent.com/java/$it)" }
        assertEquals(
            """
            Test mixed content with Javadoc style
            The [UserInfo](https://itangcent.com/java/UserInfo) class provides user information[UserInfo](https://itangcent.com/java/UserInfo).
            See [#equals](https://itangcent.com/java/#equals) for equality comparison[#equals](https://itangcent.com/java/#equals).
            See [Model](https://itangcent.com/java/Model) for model information[Model](https://itangcent.com/java/Model).
        """.trimIndent(), result
        )
    }

    fun testFindLinkWithUnresolvedLinksJavadoc() {
        val method = linkTestClass.findMethodsByName("testUnresolvedLinksJavadoc", false)[0]
        val doc = method.getComments()
        val result = standardLinkExtractor.findLink(doc, method) { "[$it](https://itangcent.com/java/$it)" }
        assertEquals(
            """
            Test unresolved links with Javadoc style
            See [UnknownClass](https://itangcent.com/java/UnknownClass) for unknown information[UnknownClass](https://itangcent.com/java/UnknownClass)
            See [#unknownMethod](https://itangcent.com/java/#unknownMethod) for unknown method[#unknownMethod](https://itangcent.com/java/#unknownMethod)
        """.trimIndent(), result
        )
    }

    fun testFindLinkWithBacktickText() {
        val method = linkTestClass.findMethodsByName("testBacktickLinks", false)[0]
        val doc = method.getComments()
        val result = standardLinkExtractor.findLink(doc, method) { "[$it](https://itangcent.com/java/$it)" }
        assertEquals(
            """
            Test backtick-wrapped text links
            See [hashCode](https://itangcent.com/java/hashCode) for hash code generation[#hashCode](https://itangcent.com/java/#hashCode)
            See [toString](https://itangcent.com/java/toString) for string representation[#toString](https://itangcent.com/java/#toString)
            See [equals](https://itangcent.com/java/equals) for equality comparison[#equals](https://itangcent.com/java/#equals)
        """.trimIndent(), result
        )
    }

    fun testFindLinkWithExistingLinkTypes() {
        val method = linkTestClass.findMethodsByName("testExistingLinkTypes", false)[0]
        val doc = method.getComments()
        val result = standardLinkExtractor.findLink(doc, method) { "[$it](https://itangcent.com/java/$it)" }
        assertEquals(
            """
            Test existing link types
            See [test](https://itangcent.com/java/test) for test information[#test](https://itangcent.com/java/#test)
            See [url](https://itangcent.com/java/url) for more information
            See [url](https://itangcent.com/java/url) for reference information
            [url](https://itangcent.com/java/url): url
            See [text](https://itangcent.com/java/text)[] for collapsed reference information
        """.trimIndent(), result
        )
    }

    private fun PsiMethod.getComments(): String {
        val comment = this@StandardLinkExtractorTest.standardDocHelper.getAttrOfDocComment(this)
        if (comment != null) {
            return comment
        }
        return this.text.lines()
            .map { it.trim() }
            .filter { it.startsWith("///") }
            .joinToString("\n") { it.removePrefix("///").trim() }
    }
}