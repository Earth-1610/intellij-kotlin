package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.itangcent.common.logger.Log
import com.itangcent.common.logger.traceError
import com.itangcent.intellij.context.ActionContext

/**
 * @author tangcent
 * @date 2024/06/30
 */
@Singleton
class DataContextProvider {
    companion object : Log() {
        private val NULL = Any()
    }

    @Inject
    private lateinit var actionContext: ActionContext

    @Inject
    private lateinit var dataContext: DataContext

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getData(key: DataKey<T>): T? {
        return try {
            actionContext.cacheOrCompute("dataContext:${key.name}") {
                actionContext.callInReadUI {
                    dataContext.getData(key)
                } ?: NULL
            }?.takeIf { it != NULL } as T?
        } catch (e: Exception) {
            LOG.traceError("error when get data '${key.name}' from dataContext", e)
            null
        }
    }
}