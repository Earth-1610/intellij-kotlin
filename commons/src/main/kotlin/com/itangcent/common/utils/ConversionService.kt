package com.itangcent.common.utils

import com.itangcent.common.spi.SpiUtils
import java.lang.reflect.Type
import java.util.*

/**
 * Singleton service that handles type conversions.
 * It maintains a list of converters to transform string values into different target types.
 *
 * @author tangcent
 * @date 2024/09/28
 */
object ConversionService {
    private val converters: List<Converter<*>> by lazy {
        (SpiUtils.loadServices(Converter::class) ?: emptyList()) + listOf(
            BooleanConverter,
            ShortConverter,
            IntegerConverter,
            LongConverter,
            FloatConverter,
            DoubleConverter,
            StringConverter,
            DateConverter,
            GsonConverter,
            EnumConverter
        )
    }

    /**
     * Converts a string value to a specified target type.
     * Searches through registered converters and applies the first one that supports the target type.
     * Throws an IllegalArgumentException if no converter is found for the type.
     *
     * @param value the string to convert.
     * @param targetType the target type to convert the value to.
     * @return the converted value as an instance of the target type.
     */
    fun convert(value: String, targetType: Type): Any {
        val converter = converters.find { it.supports(targetType) }
        if (converter == null) {
            if (targetType is Class<*> && targetType.isInstance(value)) {
                return value
            } else {
                throw IllegalArgumentException("No converter found for type $targetType")
            }
        }
        return converter.convert(value, targetType) as Any
    }

    /**
     * Tries to convert a string value to a specified target type.
     * If conversion fails, it catches the exception and returns null instead of throwing.
     *
     * @param value the string to convert.
     * @param targetType the target type to convert the value to.
     * @return the converted value or null if conversion fails.
     */
    fun tryConvert(value: String, targetType: Type): Any? {
        return try {
            convert(value, targetType)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Interface for converters. Each converter handles converting string values to a specific type.
 *
 * @param <T> the target type the converter supports.
 */
interface Converter<T> {
    fun supports(type: Type): Boolean
    fun convert(value: String, targetType: Type): T
}

/**
 * Converts strings to boolean values.
 */
object BooleanConverter : Converter<Boolean> {
    override fun supports(type: Type): Boolean {
        return type == Boolean::class.java || type == java.lang.Boolean::class.java
    }

    override fun convert(value: String, targetType: Type): Boolean {
        return value.toBoolean()
    }
}

/**
 * Converts strings to short values.
 */
object ShortConverter : Converter<Short> {
    override fun supports(type: Type): Boolean {
        return type == Short::class.java || type == java.lang.Short::class.java
    }

    override fun convert(value: String, targetType: Type): Short {
        return value.toShort()
    }
}

/**
 * Converts strings to integer values.
 */
object IntegerConverter : Converter<Int> {
    override fun supports(type: Type): Boolean {
        return type == Int::class.java || type == Integer::class.java
    }

    override fun convert(value: String, targetType: Type): Int {
        return value.toInt()
    }
}

/**
 * Converts strings to long values.
 */
object LongConverter : Converter<Long> {
    override fun supports(type: Type): Boolean {
        return type == Long::class.java || type == java.lang.Long::class.java
    }

    override fun convert(value: String, targetType: Type): Long {
        return value.toLong()
    }
}

/**
 * Converts strings to float values.
 */
object FloatConverter : Converter<Float> {
    override fun supports(type: Type): Boolean {
        return type == Float::class.java || type == java.lang.Float::class.java
    }

    override fun convert(value: String, targetType: Type): Float {
        return value.toFloat()
    }
}

/**
 * Converts strings to double values.
 */
object DoubleConverter : Converter<Double> {
    override fun supports(type: Type): Boolean {
        return type == Double::class.java || type == java.lang.Double::class.java
    }

    override fun convert(value: String, targetType: Type): Double {
        return value.toDouble()
    }
}

/**
 * Converts strings to other strings (essentially no conversion).
 */
object StringConverter : Converter<String> {
    override fun supports(type: Type): Boolean {
        return type == String::class.java
    }

    override fun convert(value: String, targetType: Type): String {
        return value
    }
}

/**
 * Converts strings to date values.
 */
object DateConverter : Converter<Date> {
    override fun supports(type: Type): Boolean {
        return type == Date::class.java
    }

    override fun convert(value: String, targetType: Type): Date {
        val timeStamp = value.toLongOrNull()
        if (timeStamp != null) {
            return Date(timeStamp)
        }
        return DateUtils.parse(value)
    }
}

/**
 * Converts strings to enum values.
 */
object EnumConverter : Converter<Enum<*>> {
    override fun supports(type: Type): Boolean {
        return type is Class<*> && type.isEnum
    }

    @Suppress("UNCHECKED_CAST")
    override fun convert(value: String, targetType: Type): Enum<*> {
        val enumClass = targetType as Class<Enum<*>>
        return enumClass.enumConstants.first { it.name == value }
    }
}

/**
 * Converts strings to any type using Gson for JSON deserialization.
 */
object GsonConverter : Converter<Any> {
    override fun supports(type: Type): Boolean {
        return true
    }

    override fun convert(value: String, targetType: Type): Any {
        return GsonUtils.fromJson(value, targetType)
    }
}