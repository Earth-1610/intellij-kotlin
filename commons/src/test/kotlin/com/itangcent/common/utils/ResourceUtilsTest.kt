package com.itangcent.common.utils

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.net.URL

class ResourceUtilsTest {

    @Test
    fun testReadResource() {
        // Test reading a valid resource
        val contents = ResourceUtils.readResource("demo.properties")
        assertEquals("token=111111", contents)

        // Test reading a non-existent resource
        val nonExistentContents = ResourceUtils.readResource("does-not-exist.properties")
        assertEquals("", nonExistentContents)

        // Test reading the same resource twice to ensure caching
        val cachedContents = ResourceUtils.readResource("demo.properties")
        assertEquals("token=111111", cachedContents)
    }

    @Test
    fun testFindResource() {
        // Test finding a valid resource
        val resourceURL: URL? = ResourceUtils.findResource("demo.properties")
        assertNotNull(resourceURL)

        // Test finding a non-existent resource
        val nonExistentResourceURL: URL? = ResourceUtils.findResource("does-not-exist.properties")
        kotlin.test.assertNull(nonExistentResourceURL)

        // Test finding the same resource URL twice to ensure caching
        val cachedResourceURL: URL? = ResourceUtils.findResource("demo.properties")
        assertNotNull(cachedResourceURL)
        assertEquals(resourceURL, cachedResourceURL)
    }
}