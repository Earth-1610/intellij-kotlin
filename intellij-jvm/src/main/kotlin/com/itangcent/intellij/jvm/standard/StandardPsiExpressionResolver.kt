package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.intellij.psi.*
import com.intellij.psi.tree.IElementType
import com.itangcent.common.utils.*
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.JvmClassHelper
import com.itangcent.intellij.jvm.PsiExpressionResolver
import com.itangcent.intellij.jvm.PsiResolver
import org.apache.commons.lang.StringUtils
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass

class StandardPsiExpressionResolver : PsiExpressionResolver {

    @Inject
    private val psiResolver: PsiResolver? = null

    @Inject
    private val jvmClassHelper: JvmClassHelper? = null

    private val psiExpressionResolver: PsiExpressionResolver = ActionContext.local()

    private val registerTypedExpressionResolverHandler: HashMap<KClass<*>, (Any) -> Any?> = HashMap()

    private val registerGenericExpressionResolverHandler: LinkedList<Pair<(Any) -> Boolean, (Any) -> Any?>> =
        LinkedList()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> registerExpressionResolver(cls: KClass<T>, handle: (T) -> Any?) {
        registerTypedExpressionResolverHandler[cls] = handle as (Any) -> Any?
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> registerExpressionResolver(predicate: (Any) -> Boolean, handle: (T) -> Any?) {
        registerGenericExpressionResolverHandler.add(predicate to (handle as (Any) -> Any?))
    }

    open override fun process(psiExpression: PsiExpression): Any? {
        registerTypedExpressionResolverHandler[psiExpression::class]
            ?.let { it(psiExpression) }
            ?.let { return it }
        when (psiExpression) {
            is PsiReferenceExpression -> {
                psiExpression.qualifierExpression
                    ?.let { return psiExpressionResolver.process(it) }
                psiExpression.resolve()
                    ?.let { return psiExpressionResolver.process(it) }
            }
            is PsiLiteralExpression -> return psiExpression.value
            is PsiBinaryExpression -> return processBinaryExpression(psiExpression)
            is PsiPolyadicExpression -> return processPolyadicExpression(psiExpression)
            is PsiUnaryExpression -> return processUnaryExpression(psiExpression)
            is PsiLambdaExpression -> return psiExpression.body?.let { psiExpressionResolver.process(it) }
            is PsiClassObjectAccessExpression -> {
                val lastChild = psiExpression.lastChild
                if (lastChild is PsiKeyword && lastChild.text == PsiKeyword.CLASS) {
                    return psiExpressionResolver.process(psiExpression.firstChild)
                }
            }
            is PsiArrayAccessExpression -> {
                val array = psiExpressionResolver.process(psiExpression.arrayExpression) ?: return null
                if (array is Array<*> && array.size > 0) {
                    var index = psiExpression.indexExpression?.let { psiExpressionResolver.process(it) } ?: 0
                    if (index !is Int) {
                        index = 0
                    }
                    return array[index]
                }
            }
        }
        return psiExpression.text
    }

    open override fun process(psiElement: PsiElement): Any? {
        registerTypedExpressionResolverHandler[psiElement::class]
            ?.let { it(psiElement) }
            ?.let { return it }
        when (psiElement) {
            is PsiExpression -> return psiExpressionResolver.process(psiElement)
            is PsiLocalVariable -> {
                psiElement.initializer?.let {
                    return psiExpressionResolver.process(it)
                }
            }
            is PsiEnumConstant -> {
                return resolveEnumFields(psiElement)
            }
            is PsiField -> {
                if (jvmClassHelper!!.isStaticFinal(psiElement)) {
                    return psiExpressionResolver.processStaticField(psiElement)
                }
                psiElement.initializer?.let {
                    return psiExpressionResolver.process(it)
                }
            }
            is PsiKeyword -> {
                //todo:any keyword return null??
            }
            is PsiReferenceParameterList -> {
                //todo:what does PsiReferenceParameterList mean
            }
            is PsiWhiteSpace -> {
                //ignore white space
                return psiElement.text
            }
            is PsiJavaCodeReferenceElement -> {
                //todo:what does PsiJavaCodeReferenceElement mean
            }
            is PsiExpressionList -> {
                val list = ArrayList<Any?>()
                for (expression in psiElement.expressions) {
                    list.add(psiExpressionResolver.process(expression))
                }
                return list
            }
            is PsiArrayInitializerMemberValue -> {
                return psiElement.initializers.map { psiExpressionResolver.process(it) }.toTypedArray()
            }
            is PsiLambdaExpression -> {
                return psiElement.body?.let { psiExpressionResolver.process(it) }
            }
            is PsiTypeElement -> {
                return psiElement.type.canonicalText
            }
            else -> {
                //ignore
//                    logger!!.debug("no matched ele ${psiElement::class.qualifiedName}:${psiElement.text}")
            }
        }
        return psiElement.text
    }

    open override fun processStaticField(psiField: PsiField): Any? {
        registerTypedExpressionResolverHandler[psiField::class]
            ?.let { it(psiField) }
            ?.let { return it }
        val constantVal = psiField.computeConstantValue()
        if (constantVal != null) return constantVal
        val initializer = psiField.initializer
        if (initializer != null) {
            return psiExpressionResolver.process(initializer)
        }
        return psiField.text
    }

    @Suppress("UNCHECKED_CAST")
    fun resolveEnumFields(value: PsiEnumConstant): Map<String, Any?>? {

        val constantInfo = psiResolver!!.resolveEnumFields(0, value) ?: return null

        return constantInfo["params"] as? Map<String, Any?>
    }

    private fun processBinaryExpression(psiExpression: PsiBinaryExpression): Any? {
        val op = psiExpression.operationSign
        val operand = StandardOperand.findOperand(op.tokenType) ?: return null
        val lOperand = psiExpressionResolver.process(psiExpression.lOperand)
        val rOperand = psiExpression.rOperand?.let { psiExpressionResolver.process(it) }
        return operand.operate(lOperand, rOperand)
    }

    private fun processPolyadicExpression(psiExpression: PsiPolyadicExpression): Any? {
        val op = psiExpression.operationTokenType
        val operand = StandardOperand.findOperand(op) ?: return null
        return psiExpression.operands.map { psiExpressionResolver.process(it) }
            .reduceSafely { acc, any -> operand.operate(acc, any) }
    }

    private fun processUnaryExpression(psiExpression: PsiUnaryExpression): Any? {
        val op = psiExpression.operationSign
        val operand = psiExpression.operand?.let { psiExpressionResolver.process(it) } ?: return null
        when (op.tokenType) {
            JavaTokenType.MINUS -> {
                return when (operand) {
                    is Int -> -operand
                    is Long -> -operand
                    is Short -> -operand
                    is Byte -> -operand
                    is Float -> -operand
                    is Double -> -operand
                    else -> {
                        operand
                    }
                }
            }
            JavaTokenType.EQEQ, JavaTokenType.NE -> {
                return true
            }
        }
        return operand
    }

}

enum class StandardOperand : Operand {
    PLUS {
        override fun operateString(first: String?, second: String?): Any {
            return first + second
        }

        override fun operateBool(first: Boolean, second: Boolean): Any? {
            //should not appear
            return first
        }

        override fun operateInt(first: Int, second: Int): Any? {
            return first + second
        }

        override fun operateLong(first: Long, second: Long): Any? {
            return first + second
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            return first + second
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            return first + second
        }
    },
    MINUS {
        override fun operateString(first: String?, second: String?): Any {
            return StringUtils.remove(first, second!!)
        }

        override fun operateBool(first: Boolean, second: Boolean): Any? {
            //should not appear
            return first
        }

        override fun operateInt(first: Int, second: Int): Any? {
            return first - second
        }

        override fun operateLong(first: Long, second: Long): Any? {
            return first - second
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            return first - second
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            return first - second
        }
    },
    ASTERISK {
        override fun operateString(first: String?, second: String?): Any? {
            return first?.repeat(second?.toInt() ?: 0)
        }

        override fun operateBool(first: Boolean, second: Boolean): Any? {
            //should not appear
            return first
        }

        override fun operateInt(first: Int, second: Int): Any? {
            return first * second
        }

        override fun operateLong(first: Long, second: Long): Any? {
            return first * second
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            return first * second
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            return first * second
        }
    },
    DIV {
        override fun operateString(first: String?, second: String?): Any? {
            //should not appear
            return first
        }

        override fun operateBool(first: Boolean, second: Boolean): Any? {
            //should not appear
            return first
        }

        override fun operateInt(first: Int, second: Int): Any? {
            return first / second
        }

        override fun operateLong(first: Long, second: Long): Any? {
            return first / second
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            return first / second
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            return first / second
        }
    },
    PERC {
        override fun operateString(first: String?, second: String?): Any? {
            //should not appear
            return first
        }

        override fun operateBool(first: Boolean, second: Boolean): Any? {
            //should not appear
            return first
        }

        override fun operateInt(first: Int, second: Int): Any? {
            return first % second
        }

        override fun operateLong(first: Long, second: Long): Any? {
            return first % second
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            return first % second
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            return first % second
        }
    },
    LTLT {
        override fun operateString(first: String?, second: String?): Any? {
            //should not appear
            return first
        }

        override fun operateBool(first: Boolean, second: Boolean): Any? {
            //should not appear
            return first
        }

        override fun operateInt(first: Int, second: Int): Any? {
            return first.shl(second)
        }

        override fun operateLong(first: Long, second: Long): Any? {
            return first.shl(second.toInt())
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            //should not appear
            return first
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            //should not appear
            return first
        }
    },
    GTGT {
        override fun operateString(first: String?, second: String?): Any? {
            //should not appear
            return first
        }

        override fun operateBool(first: Boolean, second: Boolean): Any? {
            //should not appear
            return first
        }

        override fun operateInt(first: Int, second: Int): Any? {
            return first.shr(second)
        }

        override fun operateLong(first: Long, second: Long): Any? {
            return first.shr(second.toInt())
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            //should not appear
            return first
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            //should not appear
            return first
        }
    },
    AND {
        override fun operateString(first: String?, second: String?): Any? {
            //should not appear
            return first
        }

        override fun operateBool(first: Boolean, second: Boolean): Any? {
            //should not appear
            return first.and(second)
        }

        override fun operateInt(first: Int, second: Int): Any? {
            return first.and(second)
        }

        override fun operateLong(first: Long, second: Long): Any? {
            return first.and(second)
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            //should not appear
            return first
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            //should not appear
            return first
        }
    },
    EQ {
        override fun operateString(first: String?, second: String?): Any? {
            return second
        }

        override fun operateBool(first: Boolean, second: Boolean): Any? {
            return second
        }

        override fun operateInt(first: Int, second: Int): Any? {
            return second
        }

        override fun operateLong(first: Long, second: Long): Any? {
            return second
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            return second
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            return second
        }
    },
    EQEQ {
        override fun operateString(first: String?, second: String?): Any? {
            return first == second
        }

        override fun operateBool(first: Boolean, second: Boolean): Any? {
            return first == second
        }

        override fun operateInt(first: Int, second: Int): Any? {
            return first == second
        }

        override fun operateLong(first: Long, second: Long): Any? {
            return first == second
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            return first == second
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            return first == second
        }
    },
    NE {
        override fun operateString(first: String?, second: String?): Any? {
            return first != second
        }

        override fun operateBool(first: Boolean, second: Boolean): Any? {
            return first != second
        }

        override fun operateInt(first: Int, second: Int): Any? {
            return first != second
        }

        override fun operateLong(first: Long, second: Long): Any? {
            return first != second
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            return first != second
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            return first != second
        }
    },
    LT {
        override fun operateString(first: String?, second: String?): Any? {
            if (first == null || second == null) {
                return null
            }
            return first < second
        }

        override fun operateBool(first: Boolean, second: Boolean): Boolean? {
            return first < second
        }

        override fun operateInt(first: Int, second: Int): Boolean? {
            return first < second
        }

        override fun operateLong(first: Long, second: Long): Any? {
            return first < second
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            return first < second
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            return first < second
        }
    },
    LE {
        override fun operateString(first: String?, second: String?): Any? {
            if (first == null || second == null) {
                return null
            }
            return first <= second
        }

        override fun operateBool(first: Boolean, second: Boolean): Boolean? {
            return first <= second
        }

        override fun operateInt(first: Int, second: Int): Boolean? {
            return first <= second
        }

        override fun operateLong(first: Long, second: Long): Any? {
            return first <= second
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            return first <= second
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            return first <= second
        }
    },
    GT {
        override fun operateString(first: String?, second: String?): Any? {
            if (first == null || second == null) {
                return null
            }
            return first > second
        }

        override fun operateBool(first: Boolean, second: Boolean): Boolean? {
            return first > second
        }

        override fun operateInt(first: Int, second: Int): Boolean? {
            return first > second
        }

        override fun operateLong(first: Long, second: Long): Any? {
            return first > second
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            return first > second
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            return first > second
        }
    },
    GE {
        override fun operateString(first: String?, second: String?): Any? {
            if (first == null || second == null) {
                return null
            }
            return first >= second
        }

        override fun operateBool(first: Boolean, second: Boolean): Boolean? {
            return first >= second
        }

        override fun operateInt(first: Int, second: Int): Boolean? {
            return first >= second
        }

        override fun operateLong(first: Long, second: Long): Any? {
            return first >= second
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            return first >= second
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            return first >= second
        }
    },
    OR {
        override fun operateString(first: String?, second: String?): Any? {
            //should not appear
            return first
        }

        override fun operateBool(first: Boolean, second: Boolean): Any? {
            //should not appear
            return first.or(second)
        }

        override fun operateInt(first: Int, second: Int): Any? {
            return first.or(second)
        }

        override fun operateLong(first: Long, second: Long): Any? {
            return first.or(second)
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            //should not appear
            return first
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            //should not appear
            return first
        }
    },
    XOR {
        override fun operateString(first: String?, second: String?): Any? {
            //should not appear
            return first
        }

        override fun operateBool(first: Boolean, second: Boolean): Any? {
            //should not appear
            return first.xor(second)
        }

        override fun operateInt(first: Int, second: Int): Any? {
            return first.xor(second)
        }

        override fun operateLong(first: Long, second: Long): Any? {
            return first.xor(second)
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            //should not appear
            return first
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            //should not appear
            return first
        }
    },
    ANDAND {
        override fun operateString(first: String?, second: String?): Any? {
            //should not appear
            return first
        }

        override fun operateBool(first: Boolean, second: Boolean): Any? {
            return first && second
        }

        override fun operateInt(first: Int, second: Int): Any? {
            //should not appear
            return first
        }

        override fun operateLong(first: Long, second: Long): Any? {
            //should not appear
            return first
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            //should not appear
            return first
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            //should not appear
            return first
        }
    },
    OROR {
        override fun operateString(first: String?, second: String?): Any? {
            //should not appear
            return first
        }

        override fun operateBool(first: Boolean, second: Boolean): Any? {
            return first || second
        }

        override fun operateInt(first: Int, second: Int): Any? {
            //should not appear
            return first
        }

        override fun operateLong(first: Long, second: Long): Any? {
            //should not appear
            return first
        }

        override fun operateFloat(first: Float, second: Float): Any? {
            //should not appear
            return first
        }

        override fun operateDouble(first: Double, second: Double): Any? {
            //should not appear
            return first
        }
    }
    ;

    override fun operate(first: Any?, second: Any?): Any? {
        var cast = StandardCast.findCast(first)
        val cast2 = StandardCast.findCast(second)
        if (cast2 != cast && cast2.getPriority() > cast.getPriority()) {
            cast = cast2
        }
        val f = cast.cast(first)
        val s = cast.cast(second)
        if (f is String || f == null) {
            return operateString(f as String?, s as String?)
        }
        if (s == null) {
            return f
        }
        return when (f) {
            is Boolean -> {
                operateBool(f, s as Boolean)
            }
            is Int -> {
                operateInt(f, s as Int)
            }
            is Long -> {
                operateLong(f, s as Long)
            }
            is Float -> {
                operateFloat(f, s as Float)
            }
            is Double -> {
                operateDouble(f, s as Double)
            }
            else -> {
                null
            }
        }
    }

    abstract fun operateString(first: String?, second: String?): Any?
    abstract fun operateBool(first: Boolean, second: Boolean): Any?
    abstract fun operateInt(first: Int, second: Int): Any?
    abstract fun operateLong(first: Long, second: Long): Any?
    abstract fun operateFloat(first: Float, second: Float): Any?
    abstract fun operateDouble(first: Double, second: Double): Any?

    companion object {
        fun findOperand(javaTokenType: IElementType): Operand? {
            return when (javaTokenType) {
                JavaTokenType.PLUS -> {
                    PLUS
                }
                JavaTokenType.MINUS -> {
                    MINUS
                }
                JavaTokenType.ASTERISK -> {
                    ASTERISK
                }
                JavaTokenType.DIV -> {
                    DIV
                }
                JavaTokenType.PERC -> {
                    PERC
                }
                JavaTokenType.LTLT -> {
                    LTLT
                }
                JavaTokenType.GTGT -> {
                    GTGT
                }
                JavaTokenType.AND -> {
                    AND
                }
                JavaTokenType.EQ -> {
                    EQ
                }
                JavaTokenType.EQEQ -> {
                    EQEQ
                }
                JavaTokenType.NE -> {
                    NE
                }
                JavaTokenType.GT -> {
                    GT
                }
                JavaTokenType.GE -> {
                    GE
                }
                JavaTokenType.LT -> {
                    LT
                }
                JavaTokenType.LE -> {
                    LE
                }
                JavaTokenType.OR -> {
                    OR
                }
                JavaTokenType.XOR -> {
                    XOR
                }
                JavaTokenType.ANDAND -> {
                    ANDAND
                }
                JavaTokenType.OROR -> {
                    OROR
                }
                else -> null
            }
        }
    }
}

enum class StandardCast(private val priority: Int) : Cast {
    STRING(100) {
        override fun cast(any: Any?): Any? {
            return any.toString()
        }
    },
    BOOL(1) {
        override fun cast(any: Any?): Any? {
            return any.asBool()
        }
    },
    INT(2) {
        override fun cast(any: Any?): Any? {
            return any.asInt()
        }
    },
    LONG(3) {
        override fun cast(any: Any?): Any? {
            return any.asLong()
        }
    },
    FLOAT(4) {
        override fun cast(any: Any?): Any? {
            return any.asFloat()
        }
    },
    DOUBLE(5) {
        override fun cast(any: Any?): Any? {
            return any.asDouble()
        }
    };

    override fun getPriority(): Int {
        return this.priority
    }

    companion object {
        fun findCast(any: Any?): StandardCast {
            when (any) {
                null, is String -> {
                    return STRING
                }
                is Boolean -> {
                    return BOOL
                }
                is Int -> {
                    return INT
                }
                is Long -> {
                    return LONG
                }
                is Float -> {
                    return FLOAT
                }
                is Double -> {
                    return DOUBLE
                }
                else -> {
                    return STRING
                }
            }
        }
    }
}

interface Operand {
    fun operate(first: Any?, second: Any?): Any?
}

interface Cast {
    fun getPriority(): Int
    fun cast(any: Any?): Any?
}