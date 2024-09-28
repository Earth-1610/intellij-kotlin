package com.itangcent.intellij.extend.feature

import com.google.inject.matcher.Matchers
import com.itangcent.common.logger.Log
import com.itangcent.common.logger.traceError
import com.itangcent.common.spi.SetupAble
import com.itangcent.common.spi.SpiUtils
import com.itangcent.common.utils.ConversionService
import com.itangcent.intellij.context.ActionContext
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import java.lang.reflect.Method

/**
 * Annotation to mark methods with a feature toggle.
 *
 * @param key the feature key used to determine if the feature is enabled or disabled.
 * @param returnValue the value to return if the feature is disabled (default is an empty string).
 *
 * @author tangcent
 * @date 2024/09/22
 */
annotation class Feature(
    val key: String,
    val returnValue: String = "",
)

/**
 * FeatureToggleSupport is responsible for setting up method interceptors that toggle
 * features based on their configuration.
 * It intercepts methods annotated with [Feature] and disables or enables the feature based on runtime checks.
 */
class FeatureToggleSupport : SetupAble {
    override fun init() {
        ActionContext.addDefaultInject {
            it.bindInterceptor(
                // Match all methods except FeatureToggle and its subclasses
                // Due to the [FeatureInterceptor] depends on [FeatureToggle]
                Matchers.not(Matchers.subclassesOf(FeatureToggle::class.java)),
                Matchers.annotatedWith(Feature::class.java),
                FeatureInterceptor()
            )
        }
    }

    companion object : Log() {
        private val FEATURE_TOGGLE_SPI: FeatureToggle by lazy {
            return@lazy try {
                SpiUtils.loadService(FeatureToggle::class)!!
            } catch (e: Exception) {
                LOG.traceError(e, "FeatureToggle not available")
                NoFeatureToggle
            }
        }

        /**
         * Checks if a feature identified by the given key is disabled.
         *
         * @param key the feature key to check.
         * @return true if the feature is disabled, false otherwise.
         */
        fun isFeatureDisabled(key: String): Boolean {
            return try {
                val featureToggle = getFeatureToggleFromActionContext().takeIf { it != NoFeatureToggle }
                    ?: FEATURE_TOGGLE_SPI
                featureToggle.isFeatureDisabled(key)
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Retrieves the FeatureToggle instance from the current ActionContext, or returns NoFeatureToggle if not found.
         *
         * @return the FeatureToggle instance, or NoFeatureToggle.
         */
        private fun getFeatureToggleFromActionContext(): FeatureToggle {
            val actionContext = ActionContext.getContext() ?: return NoFeatureToggle
            return actionContext.cacheOrCompute("featureToggle") {
                try {
                    actionContext.instance(FeatureToggle::class)
                } catch (e: Exception) {
                    NoFeatureToggle
                }
            } ?: NoFeatureToggle
        }
    }
}

/**
 * FeatureInterceptor intercepts method invocations and checks if the feature related to the method is disabled.
 * If disabled, it prevents the method from being invoked and returns a predefined value or null.
 */
class FeatureInterceptor : MethodInterceptor {

    /**
     * Intercepts the method call and decides whether to proceed based on the feature toggle.
     *
     * @param invocation the method invocation context.
     * @return the method's return value, or a predefined value if the feature is disabled.
     */
    override fun invoke(invocation: MethodInvocation): Any? {
        val method: Method = invocation.method
        val feature = method.getAnnotation(Feature::class.java)  // Updated to @Feature

        // Check if the method is annotated with @Feature
        if (feature != null) {
            val key = feature.key

            // Check if the feature (key) is disabled
            if (FeatureToggleSupport.isFeatureDisabled(key)) {
                // If disabled, return the specific return value
                if (method.returnType == Void.TYPE) {
                    return null
                }

                // Return the returnValue convert to the appropriate type
                val returnValue = feature.returnValue
                return ConversionService.convert(returnValue, method.returnType)
            }
        }

        // If not disabled, proceed with the original method invocation
        return invocation.proceed()
    }
}

/**
 * Inline function that conditionally executes a block of code if the specified feature is not disabled.
 *
 * @param key the feature key to check.
 * @param block the block of code to execute if the feature is enabled.
 */
inline fun feature(key: String, block: () -> Unit) {
    if (FeatureToggleSupport.isFeatureDisabled(key)) {
        return
    }
    block()
}