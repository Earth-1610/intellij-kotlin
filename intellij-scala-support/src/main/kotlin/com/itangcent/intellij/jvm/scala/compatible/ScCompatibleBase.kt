package com.itangcent.intellij.jvm.scala.compatible

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.itangcent.common.logger.ILogger
import com.itangcent.common.spi.SpiUtils
import com.itangcent.common.utils.getPropertyValue
import com.itangcent.common.utils.invokeMethod
import com.itangcent.intellij.jvm.scala.castToList
import com.itangcent.intellij.jvm.scala.castToTypedList
import com.itangcent.intellij.jvm.scala.getOrNull
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import kotlin.reflect.KClass

abstract class ScCompatibleBase {

    protected abstract val classNames: Array<String>

    private var cls: KClass<*>? = null
    private var init: Int = 0

    private fun checkInit() {
        if (init == 0) {
            synchronized(this) {
                if (init == 0) {
                    init = 1
                    val logger: ILogger? = SpiUtils.loadService(ILogger::class)
                    for (className in classNames) {
                        try {
                            logger?.log("try load class $className")
                            val loadClass = this::class.java.classLoader.loadClass(className)
                            cls = loadClass.kotlin
                            onLoadClass(className)
                            return
                        } catch (e: Throwable) {
                            logger?.log("load class $className failed")
                        }
                    }
                }
            }
        }
    }

    open fun onLoadClass(className: String) {

    }

    fun isInstance(target: Any): Boolean {
        checkInit()
        return cls?.isInstance(target) ?: false
    }

    fun call(target: Any, methodName: String, vararg args: Any?): Any? {
        checkInit()
        return target.invokeMethod(methodName, *args)
    }

    fun readField(target: Any, fieldName: String) {
        checkInit()
        target.getPropertyValue(fieldName)
    }

    companion object {
        const val scala_api_base = "org.jetbrains.plugins.scala.lang.psi.api.base"
        const val scala_api_expr = "org.jetbrains.plugins.scala.lang.psi.api.expr"
    }
}

object ScCompatibleAnnotation : ScCompatibleBase() {
    override val classNames: Array<String>
        get() = arrayOf("$scala_api_base.ScAnnotation", "$scala_api_expr.ScAnnotation")

    fun annotationExpr(target: Any): Any? {
        return call(target, "annotationExpr")
    }
}

object ScCompatibleAnnotationExpr : ScCompatibleBase() {
    override val classNames: Array<String>
        get() = arrayOf("$scala_api_base.ScAnnotationExpr", "$scala_api_expr.ScAnnotationExpr")

    fun getAnnotationParameters(target: Any): List<Any> {
        val ret = call(target, "getAnnotationParameters") ?: return emptyList()
        return ret.castToList()
    }
}

object ScCompatibleAssignment : ScCompatibleBase() {
    override val classNames: Array<String>
        get() = arrayOf("$scala_api_expr.ScAssignStmt", "$scala_api_expr.ScAssignment")

    private var leftExpressionMethodName: String? = null
    private var rightExpressionMethodName: String? = null
    private var referenceNameMethodName: String? = null

    override fun onLoadClass(className: String) {
        if (className == "$scala_api_expr.ScAssignStmt") {
            leftExpressionMethodName = "getLExpression"
            rightExpressionMethodName = "getRExpression"
            referenceNameMethodName = "assignName"
        } else if (className == "$scala_api_expr.ScAssignment") {
            leftExpressionMethodName = "leftExpression"
            rightExpressionMethodName = "rightExpression"
            referenceNameMethodName = "referenceName"
        }
    }

    fun leftExpression(target: Any): ScExpression? {
        return leftExpressionMethodName?.let { call(target, it) } as? ScExpression? ?: return null
    }

    fun rightExpression(target: Any): ScExpression? {
        return rightExpressionMethodName?.let { call(target, it) }?.getOrNull() ?: return null
    }

    fun referenceName(target: Any): String? {
        val ret = referenceNameMethodName?.let { call(target, it) } ?: return null
        return ret.getOrNull<Any>()?.toString()
    }
}

object ScCompatibleAnnotationsHolder : ScCompatibleBase() {
    override val classNames: Array<String>
        get() = arrayOf("$scala_api_base.ScAnnotationsHolder", "$scala_api_expr.ScAnnotationsHolder")

    fun findAnnotation(target: Any, qualifiedName: String): PsiAnnotation? {
        val ret = call(target, "findAnnotation", qualifiedName) ?: return null
        return ret as? PsiAnnotation?
    }

    fun annotations(target: Any): List<PsiAnnotation> {
        val ret = call(target, "annotations") ?: return emptyList()
        return ret.castToTypedList()
    }
}

object ScCompatibleLiteral : ScCompatibleBase() {
    override val classNames: Array<String>
        get() = arrayOf("$scala_api_base.ScLiteral", "$scala_api_expr.ScLiteral")

    fun getValue(target: Any): Any? {
        return call(target, "getValue") ?: return null
    }

    fun getText(target: Any): String? {
        return (target as? PsiElement)?.text ?: call(target, "getText")?.toString() ?: return null
    }
}

object ScCompatiblePhysicalMethodSignature : ScCompatibleBase() {
    override val classNames: Array<String>
        get() = arrayOf(
            "org.jetbrains.plugins.scala.lang.psi.types.PhysicalMethodSignature",
            "org.jetbrains.plugins.scala.lang.psi.types.PhysicalSignature"
        )

    fun method(target: Any): PsiMethod? {
        return call(target, "method") as? PsiMethod? ?: return null
    }

}