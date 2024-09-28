package com.itangcent.intellij.extend.feature

import com.itangcent.common.spi.Setup
import com.itangcent.common.spi.SpiUtils
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.singleton
import com.itangcent.intellij.extend.guice.with
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

/**
 * Test class for [Feature]
 * @author tangcent
 * @date 2024/09/27
 */
open class BaseFeatureTest {

    @BeforeEach
    fun setUp() {
        FeatureToggleSpi.updateFeatures(
            disabledFeatures = setOf("test_disable"),
            unknownFeatures = setOf("test_error")
        )
    }
}

class FeatureTest : BaseFeatureTest() {

    @Test
    fun testFeature() {
        var called = false
        feature("test_enable") {
            called = true
        }
        assertTrue(called)

        called = false
        feature("test_disable") {
            called = true
        }
        assertFalse(called)

        called = false
        feature("test_error") {
            called = true
        }
        assertTrue(called)
    }
}

class FeatureToggleSupportTest : BaseFeatureTest() {

    @Test
    fun `test isFeatureDisabled returns false when feature is enable`() {
        // When
        val result = FeatureToggleSupport.isFeatureDisabled("test_enable")

        // Then
        assertFalse(result)
    }

    @Test
    fun `test isFeatureDisabled returns true when feature is disable`() {
        // When
        val result = FeatureToggleSupport.isFeatureDisabled("test_disable")

        // Then
        assertTrue(result)
    }

    @Test
    fun `test isFeatureDisabled returns false when feature toggle throws exception`() {
        // When
        val result = FeatureToggleSupport.isFeatureDisabled("test_error")

        // Then
        assertFalse(result)
    }

    @Test
    fun `test default feature toggle is used when none in context`() {
        // This test will not mock anything, assuming the use of the NoFeatureToggle object
        SpiUtils.clearCache()

        // Given
        val result = FeatureToggleSupport.isFeatureDisabled("some_key")

        // Then
        assertFalse(result) // NoFeatureToggle always returns false
    }
}

interface FeatureTestBean {
    fun testEnable(): String
    fun testDisable(): String
    fun testError(): String
}

open class FeatureTestBeanImpl : FeatureTestBean {

    @Feature("test_enable", returnValue = "disable")
    override fun testEnable(): String {
        return "enable"
    }

    @Feature("test_disable", returnValue = "disable")
    override fun testDisable(): String {
        return "fake"
    }

    @Feature("test_error")
    override fun testError(): String {
        return "fake"
    }
}

class FeatureInterceptorTest : BaseFeatureTest() {

    @Test
    fun testBindFeatureToggleInActionContext() {
        val mockFeatureToggle: FeatureToggle = mock {
            on { isFeatureDisabled("test_enable") }.thenReturn(false)
            on { isFeatureDisabled("test_disable") }.thenReturn(true)
            on { isFeatureDisabled("test_error") }.thenThrow(RuntimeException("FeatureToggle error"))
        }
        val builder = ActionContext.builder()
        builder.bindInstance(FeatureToggle::class, mockFeatureToggle)
        builder.bind(FeatureTestBean::class) { it.with(FeatureTestBeanImpl::class).singleton() }
        val actionContext = builder.build()

        val featureTestBean = actionContext.instance(FeatureTestBean::class)
        assertEquals("enable", featureTestBean.testEnable())
        assertEquals("disable", featureTestBean.testDisable())
        assertEquals("fake", featureTestBean.testError())
    }

    @Test
    fun `test not bind FeatureToggle in ActionContext`() {
        val builder = ActionContext.builder()
        builder.bind(FeatureTestBean::class) { it.with(FeatureTestBeanImpl::class).singleton() }
        val actionContext = builder.build()

        val featureTestBean = actionContext.instance(FeatureTestBean::class)
        assertEquals("enable", featureTestBean.testEnable())
        assertEquals("disable", featureTestBean.testDisable())
        assertEquals("fake", featureTestBean.testError())
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun load() {
            Setup.load()
        }
    }
}

class FeatureToggleSpi : FeatureToggle {

    companion object {
        private var disabledFeatures = mutableSetOf<String>()
        private var unknownFeatures = mutableSetOf<String>()

        fun updateFeatures(disabledFeatures: Set<String>, unknownFeatures: Set<String>) {
            this.disabledFeatures = disabledFeatures.toMutableSet()
            this.unknownFeatures = unknownFeatures.toMutableSet()
        }
    }

    override fun isFeatureDisabled(key: String): Boolean {
        if (key in unknownFeatures) {
            throw RuntimeException("FeatureToggle error")

        }
        return disabledFeatures.contains(key)
    }
}