package com.itangcent.intellij.config

import com.itangcent.intellij.config.resource.Resource
import org.junit.jupiter.api.Test
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Test case for [ConfigContent] implementations
 */
class ConfigContentTest {

    @Test
    fun testRawConfigContentData() {
        val content = "test content"
        val type = "txt"
        val configContent = RawConfigContentData(content, type)

        assertEquals("Data:" + content.hashCode().toString(), configContent.id)
        assertEquals(content, configContent.content)
        assertEquals(type, configContent.type)
        assertNull(configContent.url)
    }

    @Test
    fun testUrlConfigContent() {
        val content = "test content"
        val type = "txt"
        
        // Test with URL
        val url = "http://example.com/test.txt"
        val configContentWithUrl = UrlConfigContent(content, type, url)
        
        assertEquals("Url:$url", configContentWithUrl.id)
        assertEquals(content, configContentWithUrl.content)
        assertEquals(type, configContentWithUrl.type)
        assertEquals(url, configContentWithUrl.url)
        
        // Test without URL
        val configContentWithoutUrl = UrlConfigContent(content, type, null)
        
        assertEquals("Url:" + content.hashCode().toString(), configContentWithoutUrl.id)
        assertEquals(content, configContentWithoutUrl.content)
        assertEquals(type, configContentWithoutUrl.type)
        assertNull(configContentWithoutUrl.url)
    }

    @Test
    fun testConfigContentFactoryFunctions() {
        val content = "test content"
        val type = "txt"
        
        // Test without URL
        val configContent1 = ConfigContent(content, type)
        assertEquals("Data:" + content.hashCode().toString(), configContent1.id)
        assertEquals(content, configContent1.content)
        assertEquals(type, configContent1.type)
        assertNull(configContent1.url)
        
        // Test with URL
        val url = "http://example.com/test.txt"
        val configContent2 = ConfigContent(content, type, url)
        assertEquals("Url:$url", configContent2.id)
        assertEquals(content, configContent2.content)
        assertEquals(type, configContent2.type)
        assertEquals(url, configContent2.url)
    }

    @Test
    fun testResourceConfigContent() {
        val mockResource = MockResource()
        val configContent = ConfigContent(mockResource)
        
        assertEquals("Resource:http://example.com/test.json", configContent.id)
        assertEquals("test content", configContent.content)
        assertEquals("json", configContent.type)
        assertEquals("http://example.com/test.json", configContent.url)
    }
    
    private class MockResource : Resource() {
        override val url: URL = URL("http://example.com/test.json")
        
        override val content: String?
            get() = "test content"
    }
} 