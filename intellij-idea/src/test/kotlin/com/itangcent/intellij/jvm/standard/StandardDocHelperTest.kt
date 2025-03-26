package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.intellij.psi.PsiClass
import com.itangcent.testFramework.ContextLightCodeInsightFixtureTestCase

/**
 * Test case of [StandardDocHelper]
 */
internal class StandardDocHelperTest : ContextLightCodeInsightFixtureTestCase() {

    private lateinit var commentDemoPsiClass: PsiClass

    @Inject
    private lateinit var standardDocHelper: StandardDocHelper

    override fun beforeBind() {
        super.beforeBind()
        commentDemoPsiClass = loadClass("model/CommentDemo.java")!!
    }

    fun testHasTag() {
        assertTrue(standardDocHelper.hasTag(commentDemoPsiClass.fields[0], "single"))
        assertFalse(standardDocHelper.hasTag(commentDemoPsiClass.fields[1], "single"))
        assertFalse(standardDocHelper.hasTag(commentDemoPsiClass.fields[0], "multi"))
        assertTrue(standardDocHelper.hasTag(commentDemoPsiClass.fields[1], "multi"))
    }

    fun testFindDocByTag() {
        assertEquals("low case of A", standardDocHelper.findDocByTag(commentDemoPsiClass.fields[0], "desc"))
        assertNull(standardDocHelper.findDocByTag(commentDemoPsiClass.fields[1], "desc"))
    }

    fun testFindDocsByTag() {
        assertEquals(listOf("low case of A"), standardDocHelper.findDocsByTag(commentDemoPsiClass.fields[0], "desc"))
        assertNull(standardDocHelper.findDocByTag(commentDemoPsiClass.fields[1], "desc"))
        assertEquals(
            listOf("x\nx1 x2 x3", "y"),
            standardDocHelper.findDocsByTag(commentDemoPsiClass.fields[1], "module")
        )
        assertNull(standardDocHelper.findDocByTag(commentDemoPsiClass.fields[0], "module"))
    }

    fun testFindDocsByTagAndName() {
        assertEquals(
            "A",
            standardDocHelper.findDocsByTagAndName(commentDemoPsiClass.methods[0], "param", "a")
        )
    }

    fun testGetAttrOfDocComment() {
        assertEquals(
            "single line",
            standardDocHelper.getAttrOfDocComment(commentDemoPsiClass.fields[0])
        )
        assertEquals(
            "multi-line\n" +
                    "second line",
            standardDocHelper.getAttrOfDocComment(commentDemoPsiClass.fields[1])
        )
        assertEquals(
            "head line\n" +
                    "second line\n" +
                    "<pre>\n" +
                    "    {\n" +
                    "        \"a\":\"b\",\n" +
                    "        \"c\":{\n" +
                    "             \"x\":[\"y\"]\n" +
                    "        }\n" +
                    "    }\n" +
                    "</pre>\n" +
                    "see @{link somelink}\n" +
                    "tail line",
            standardDocHelper.getAttrOfDocComment(commentDemoPsiClass.fields[2])
        )
        assertEquals(
            "head line\n" +
                    "second line\n" +
                    "<pre>\n" +
                    "\n" +
                    "    {\n" +
                    "        \"a\":\"b\",\n" +
                    "        \"c\":{\n" +
                    "             \"x\":[\"y\"]\n" +
                    "        }\n" +
                    "    }\n" +
                    "\n" +
                    "</pre>\n" +
                    "<p>\n" +
                    "see @{link somelink}\n" +
                    "tail line",
            standardDocHelper.getAttrOfDocComment(commentDemoPsiClass.fields[3])
        )
    }

    fun testGetDocCommentContent() {
        assertEquals(
            "single line",
            standardDocHelper.getDocCommentContent(commentDemoPsiClass.fields[0].docComment!!)
        )
        assertEquals(
            "multi-line\n" +
                    "second line",
            standardDocHelper.getDocCommentContent(commentDemoPsiClass.fields[1].docComment!!)
        )
        assertEquals(
            "head line\n" +
                    "second line\n" +
                    "<pre>\n" +
                    "    {\n" +
                    "        \"a\":\"b\",\n" +
                    "        \"c\":{\n" +
                    "             \"x\":[\"y\"]\n" +
                    "        }\n" +
                    "    }\n" +
                    "</pre>\n" +
                    "see @{link somelink}\n" +
                    "tail line",
            standardDocHelper.getDocCommentContent(commentDemoPsiClass.fields[2].docComment!!)
        )
        assertEquals(
            "head line\n" +
                    "second line\n" +
                    "<pre>\n" +
                    "\n" +
                    "    {\n" +
                    "        \"a\":\"b\",\n" +
                    "        \"c\":{\n" +
                    "             \"x\":[\"y\"]\n" +
                    "        }\n" +
                    "    }\n" +
                    "\n" +
                    "</pre>\n" +
                    "<p>\n" +
                    "see @{link somelink}\n" +
                    "tail line",
            standardDocHelper.getDocCommentContent(commentDemoPsiClass.fields[3].docComment!!)
        )
    }

    fun testGetSubTagMapOfDocComment() {
        assertEquals(
            emptyMap<Any?, Any?>(),
            standardDocHelper.getSubTagMapOfDocComment(
                commentDemoPsiClass.fields[0], "param"
            )
        )
        assertEquals(
            mapOf("a" to "A", "b" to "B"),
            standardDocHelper.getSubTagMapOfDocComment(
                commentDemoPsiClass.methods[0], "param"
            )
        )
    }

    fun testGetTagMapOfDocComment() {
        assertEquals(
            mapOf("single" to "", "tag" to "public", "desc" to "low case of A"),
            standardDocHelper.getTagMapOfDocComment(commentDemoPsiClass.fields[0])
        )
        assertEquals(
            mapOf("module" to "y", "multi" to ""),
            standardDocHelper.getTagMapOfDocComment(commentDemoPsiClass.fields[1])
        )
    }

    fun testGetSuffixComment() {
        assertNull(standardDocHelper.getEolComment(commentDemoPsiClass.fields[3]))
        assertEquals(
            "E is a mathematical constant approximately equal to 2.71828",
            standardDocHelper.getEolComment(commentDemoPsiClass.fields[4])
        )
        assertNull(
            standardDocHelper.getEolComment(commentDemoPsiClass.fields[5])
        )
    }

    fun testGetAttrOfField() {
        assertEquals(
            "head line\n" +
                    "second line\n" +
                    "<pre>\n" +
                    "\n" +
                    "    {\n" +
                    "        \"a\":\"b\",\n" +
                    "        \"c\":{\n" +
                    "             \"x\":[\"y\"]\n" +
                    "        }\n" +
                    "    }\n" +
                    "\n" +
                    "</pre>\n" +
                    "<p>\n" +
                    "see @{link somelink}\n" +
                    "tail line", standardDocHelper.getAttrOfField(commentDemoPsiClass.fields[3])
        )
        assertEquals(
            "E is a mathematical constant approximately equal to 2.71828",
            standardDocHelper.getAttrOfField(commentDemoPsiClass.fields[4])
        )
        assertEquals(
            "R, or r, is the eighteenth letter of the modern English alphabet and the ISO basic Latin alphabet.",
            standardDocHelper.getAttrOfField(commentDemoPsiClass.fields[5])
        )
    }
}