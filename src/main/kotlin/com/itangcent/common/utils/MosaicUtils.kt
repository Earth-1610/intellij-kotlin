package com.itangcent.common.utils

import com.google.common.cache.CacheBuilder
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Function


object MosaicUtils {

    private const val mask = 0x00ff

    private val asteriskCache = CacheBuilder.newBuilder()
        .maximumSize(10)
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .build<Int, CharArray>()

    //endregion builds-------------------------------------------------------------

    private val idCardMosaic =
        mosaicBuild().keep(5).mosaic(4, '*').skipTo(-5).appendTail().returnDefectOnError().build()

    private val phoneMosaic = mosaicBuild().keep(3).mosaicAndSkip(4, '*').appendTail().returnDefectOnError().build()

    private val nameMosaic = mosaicBuild().keep(1).mosaic(2, '*').returnDefectOnError().build()

    private fun getAsterisks(len: Int, asterisk: Char): CharArray {
        val key = len shl 16 or asterisk.toInt()
        var asterisks = asteriskCache.getIfPresent(key)
        if (asterisks == null) {
            asterisks = buildAsterisks(key)
            asteriskCache.put(key, asterisks)
        }
        return asterisks
    }

    private fun buildAsterisks(lenAndAsterisk: Int): CharArray {
        val asterisk = (lenAndAsterisk and mask).toChar()
        val len = lenAndAsterisk shr 16
        val asterisks = CharArray(len)
        Arrays.fill(asterisks, asterisk)
        return asterisks
    }

    fun mosaic(str: String, start: Int, len: Int, asterisk: Char): String {
        return mosaic(str, start, getAsterisks(len, asterisk))
    }

    fun mosaic(str: String, start: Int, asterisks: CharArray): String {
        val chars = str.toCharArray()
        System.arraycopy(asterisks, 0, chars, start, asterisks.size)
        return String(chars)
    }

    fun mosaic(str: String, head: Int, tail: Int, asterisks: CharArray): String {
        val src = str.toCharArray()
        val length = head + tail + asterisks.size
        val chars = CharArray(length)
        System.arraycopy(src, 0, chars, 0, head)
        System.arraycopy(asterisks, 0, chars, head, asterisks.size)
        System.arraycopy(src, src.size - tail, chars, head + asterisks.size, tail)
        return String(chars)
    }

    //region builds-------------------------------------------------------------
    fun mosaicBuild(): DefaultMosaicBuild {
        return DefaultMosaicBuild()
    }

    interface Mosaic : Function<String?, String?> {
        fun mosaic(str: String?): String?

        override fun apply(str: String?): String? {
            return mosaic(str)
        }
    }

    interface MosaicBuild {
        fun keep(len: Int): MosaicBuild

        fun keepTo(index: Int): MosaicBuild

        fun skip(len: Int): MosaicBuild

        fun skipTo(index: Int): MosaicBuild

        fun appendTail(): MosaicBuild

        fun mosaic(asterisks: String): MosaicBuild

        fun mosaic(asterisks: CharArray): MosaicBuild

        fun mosaic(len: Int, asterisk: Char): MosaicBuild

        fun mosaicAndSkip(asterisks: String): MosaicBuild

        fun mosaicAndSkip(asterisks: CharArray): MosaicBuild

        fun mosaicAndSkip(len: Int, asterisk: Char): MosaicBuild

        fun onException(exceptionHandle: MosaicExceptionHandle): MosaicBuild

        fun returnSrcOnError(): MosaicBuild

        fun returnDefectOnError(): MosaicBuild

        fun returnOnError(defaultTxt: String): MosaicBuild

        fun build(): Mosaic
    }

    /**
     * In general, the length of the code will not exceed the length of the original character,
     * so it is not supported to exceed the length of the original character
     */
    class DefaultMosaicBuild : MosaicBuild {

        private val actions = ArrayList<MosaicAction>(4)

        private var tail = -1

        private var minLen = 0

        private var exceptionHandle: MosaicExceptionHandle? = null

        fun addAction(simpleMosaicAction: SimpleMosaicAction) {
            actions.add(simpleMosaicAction)
        }

        fun safety(): SafetyMosaicBuild {
            return SafetyMosaicBuild(actions, tail, minLen, exceptionHandle)
        }

        /**
         * 保留位数
         */
        override fun keep(len: Int): DefaultMosaicBuild {
            addAction(object : SimpleMosaicAction {
                override fun next(src: CharArray, result: CharArray, indexes: IntArray) {
                    System.arraycopy(src, indexes[0], result, indexes[1], len)
                    indexes[0] = indexes[0] + len
                    indexes[1] = indexes[1] + len
                }
            })
            tail = -1
            minLen += len
            return this
        }

        /**
         * 保留位数至
         * 负数从后往前算
         */
        override fun keepTo(index: Int): DefaultMosaicBuild {
            if (index < 0) {
                addAction(object : SimpleMosaicAction {
                    override fun next(src: CharArray, result: CharArray, indexes: IntArray) {
                        val nextIndex = src.size + index
                        val len = nextIndex - indexes[0]
                        System.arraycopy(src, indexes[0], result, indexes[1], len)
                        indexes[0] = nextIndex
                        indexes[1] = indexes[1] + len
                    }
                })
                tail = 0 - index
            } else {
                addAction(object : SimpleMosaicAction {
                    override fun next(src: CharArray, result: CharArray, indexes: IntArray) {
                        val len = index - indexes[0]
                        System.arraycopy(src, indexes[0], result, indexes[1], len)
                        indexes[0] = index
                        indexes[1] = indexes[1] + len
                    }
                })
                tail = -1
            }
            return this
        }

        override fun skip(len: Int): DefaultMosaicBuild {
            addAction(object : SimpleMosaicAction {
                override fun next(src: CharArray, result: CharArray, indexes: IntArray) {
                    indexes[0] = indexes[0] + len
                }
            })
            tail = -1
            return this
        }

        override fun skipTo(index: Int): DefaultMosaicBuild {

            if (index < 0) {
                addAction(object : SimpleMosaicAction {
                    override fun next(src: CharArray, result: CharArray, indexes: IntArray) {
                        val nextIndex = src.size + index
                        indexes[0] = nextIndex
                    }
                })
                tail = 0 - index
            } else {
                addAction(object : SimpleMosaicAction {
                    override fun next(src: CharArray, result: CharArray, indexes: IntArray) {
                        indexes[0] = index
                    }
                })
                tail = -1
            }
            return this
        }

        override fun appendTail(): DefaultMosaicBuild {
            if (tail == -1) {
                addAction(object : SimpleMosaicAction {
                    override fun next(src: CharArray, result: CharArray, indexes: IntArray) {
                        val tailLen = src.size - indexes[0]
                        System.arraycopy(src, indexes[0], result, indexes[1], tailLen)
                        indexes[0] += tailLen
                        indexes[1] += tailLen
                    }
                })
            } else {
                val tailLen = tail
                addAction(object : SimpleMosaicAction {
                    override fun next(src: CharArray, result: CharArray, indexes: IntArray) {
                        System.arraycopy(src, indexes[0], result, indexes[1], tailLen)
                        indexes[0] += tailLen
                        indexes[1] += tailLen
                    }
                })
            }
            return this
        }

        override fun mosaic(asterisks: String): DefaultMosaicBuild {
            return mosaic(asterisks.toCharArray())
        }

        override fun mosaic(asterisks: CharArray): DefaultMosaicBuild {
            addAction(object : SimpleMosaicAction {
                override fun next(src: CharArray, result: CharArray, indexes: IntArray) {
                    System.arraycopy(asterisks, 0, result, indexes[1], asterisks.size)
                    indexes[1] += asterisks.size
                }
            })
            minLen += asterisks.size
            return this
        }

        override fun mosaic(len: Int, asterisk: Char): DefaultMosaicBuild {
            return mosaic(getAsterisks(len, asterisk))
        }

        override fun mosaicAndSkip(asterisks: String): DefaultMosaicBuild {
            return mosaic(asterisks.toCharArray())
        }

        override fun mosaicAndSkip(asterisks: CharArray): DefaultMosaicBuild {
            addAction(object : SimpleMosaicAction {
                override fun next(src: CharArray, result: CharArray, indexes: IntArray) {
                    System.arraycopy(asterisks, 0, result, indexes[1], asterisks.size)
                    indexes[0] += asterisks.size
                    indexes[1] += asterisks.size
                }
            })
            tail = -1
            minLen += asterisks.size
            return this
        }

        override fun mosaicAndSkip(len: Int, asterisk: Char): DefaultMosaicBuild {
            return mosaicAndSkip(getAsterisks(len, asterisk))
        }

        override fun onException(exceptionHandle: MosaicExceptionHandle): DefaultMosaicBuild {
            this.exceptionHandle = exceptionHandle
            return this
        }

        override fun returnSrcOnError(): DefaultMosaicBuild {
            this.exceptionHandle = { _, src, _ -> src }
            return this
        }

        override fun returnDefectOnError(): DefaultMosaicBuild {
            this.exceptionHandle = { _, _, defect -> defect }
            return this
        }

        override fun returnOnError(defaultTxt: String): DefaultMosaicBuild {
            this.exceptionHandle = { _, _, _ -> defaultTxt }
            return this
        }

        override fun build(): Mosaic {
            return ActionsMosaic(actions.toTypedArray(), exceptionHandle, minLen)
        }
    }

    /**
     * The length is checked before each operation
     */
    class SafetyMosaicBuild : MosaicBuild {

        private var actions: MutableList<MosaicAction>? = null

        private var tail: Int = 0

        private var minLen: Int = 0

        private var exceptionHandle: MosaicExceptionHandle? = null


        fun addAction(safetyMosaicAction: SafetyMosaicAction) {
            actions!!.add(safetyMosaicAction)
        }

        constructor() {
            this.actions = ArrayList(4)
            this.tail = -1
            this.minLen = 0
        }

        constructor(
            actions: MutableList<MosaicAction>,
            tail: Int,
            minLen: Int,
            exceptionHandle: MosaicExceptionHandle?
        ) {
            this.actions = actions
            this.tail = tail
            this.minLen = minLen
            this.exceptionHandle = exceptionHandle
        }

        internal fun safetyCopy(
            srcAndResult: Array<CharArray>, src: CharArray, srcPos: Int,
            result: CharArray, resultPos: Int,
            len: Int
        ): Int {
            var result = result
            var fixLen = len
            if (src.size - srcPos - fixLen < 0) {
                fixLen = src.size - srcPos
                if (fixLen < 0) {
                    return 0
                }
            }
            if (needGrowCapacity(srcAndResult, resultPos + fixLen)) {
                result = srcAndResult[1]
            }
            System.arraycopy(src, srcPos, result, resultPos, fixLen)
            return fixLen
        }

        override fun keep(len: Int): SafetyMosaicBuild {
            addAction(object : SafetyMosaicAction {
                override fun next(
                    srcAndResult: Array<CharArray>,
                    src: CharArray,
                    result: CharArray,
                    indexes: IntArray
                ) {
                    val iLen = safetyCopy(srcAndResult, src, indexes[0], result, indexes[1], len)
                    indexes[0] = indexes[0] + iLen
                    indexes[1] = indexes[1] + iLen
                }
            })
            tail = -1
            minLen += len
            return this
        }

        override fun keepTo(index: Int): SafetyMosaicBuild {
            if (index < 0) {
                addAction(object : SafetyMosaicAction {
                    override fun next(
                        srcAndResult: Array<CharArray>,
                        src: CharArray,
                        result: CharArray,
                        indexes: IntArray
                    ) {
                        val nextIndex = src.size + index
                        var len = nextIndex - indexes[0]
                        len = safetyCopy(srcAndResult, src, indexes[0], result, indexes[1], len)
                        indexes[0] = nextIndex
                        indexes[1] = indexes[1] + len
                    }
                })
                tail = 0 - index
            } else {
                addAction(object : SafetyMosaicAction {
                    override fun next(
                        srcAndResult: Array<CharArray>,
                        src: CharArray,
                        result: CharArray,
                        indexes: IntArray
                    ) {
                        var len = index - indexes[0]
                        len = safetyCopy(srcAndResult, src, indexes[0], result, indexes[1], len)
                        indexes[0] = index
                        indexes[1] = indexes[1] + len
                    }
                })
                tail = -1
            }
            return this
        }

        override fun skip(len: Int): SafetyMosaicBuild {

            addAction(object : SafetyMosaicAction {
                override fun next(
                    srcAndResult: Array<CharArray>,
                    src: CharArray,
                    result: CharArray,
                    indexes: IntArray
                ) {
                    indexes[0] = indexes[0] + len
                }
            })
            tail = -1
            return this
        }


        override fun skipTo(index: Int): SafetyMosaicBuild {

            if (index < 0) {
                addAction(object : SafetyMosaicAction {
                    override fun next(
                        srcAndResult: Array<CharArray>,
                        src: CharArray,
                        result: CharArray,
                        indexes: IntArray
                    ) {
                        val nextIndex = src.size + index
                        indexes[0] = nextIndex
                    }
                })
                tail = 0 - index
            } else {
                addAction(object : SafetyMosaicAction {
                    override fun next(
                        srcAndResult: Array<CharArray>,
                        src: CharArray,
                        result: CharArray,
                        indexes: IntArray
                    ) {
                        indexes[0] = index
                    }
                })
                tail = -1
            }
            return this
        }

        override fun appendTail(): SafetyMosaicBuild {
            if (tail == -1) {
                addAction(object : SafetyMosaicAction {
                    override fun next(
                        srcAndResult: Array<CharArray>,
                        src: CharArray,
                        result: CharArray,
                        indexes: IntArray
                    ) {
                        var len = src.size - indexes[0]
                        len = safetyCopy(srcAndResult, src, indexes[0], result, indexes[1], len)
                        indexes[0] += len
                        indexes[1] += len
                    }
                })
            } else {
                val tailLen = tail
                addAction(object : SafetyMosaicAction {
                    override fun next(
                        srcAndResult: Array<CharArray>,
                        src: CharArray,
                        result: CharArray,
                        indexes: IntArray
                    ) {
                        val len = safetyCopy(srcAndResult, src, indexes[0], result, indexes[1], tailLen)
                        indexes[0] += len
                        indexes[1] += len
                    }
                })
            }
            return this
        }

        override fun mosaic(asterisks: String): SafetyMosaicBuild {
            return mosaic(asterisks.toCharArray())
        }

        override fun mosaic(asterisks: CharArray): SafetyMosaicBuild {
            addAction(object : SafetyMosaicAction {
                override fun next(
                    srcAndResult: Array<CharArray>,
                    src: CharArray,
                    result: CharArray,
                    indexes: IntArray
                ) {
                    val len = safetyCopy(srcAndResult, asterisks, 0, result, indexes[1], asterisks.size)
                    indexes[1] += len
                }
            })
            minLen += asterisks.size
            return this
        }


        override fun mosaic(len: Int, asterisk: Char): SafetyMosaicBuild {
            return mosaic(getAsterisks(len, asterisk))
        }

        override fun mosaicAndSkip(asterisks: String): SafetyMosaicBuild {
            return mosaic(asterisks.toCharArray())
        }

        override fun mosaicAndSkip(asterisks: CharArray): SafetyMosaicBuild {
            addAction(object : SafetyMosaicAction {
                override fun next(
                    srcAndResult: Array<CharArray>,
                    src: CharArray,
                    result: CharArray,
                    indexes: IntArray
                ) {
                    val len = safetyCopy(srcAndResult, asterisks, 0, result, indexes[1], asterisks.size)
                    indexes[0] += len
                    indexes[1] += len
                }
            })
            tail = -1
            minLen += asterisks.size
            return this
        }

        override fun mosaicAndSkip(len: Int, asterisk: Char): SafetyMosaicBuild {
            return mosaicAndSkip(getAsterisks(len, asterisk))
        }

        override fun onException(exceptionHandle: MosaicExceptionHandle): SafetyMosaicBuild {
            this.exceptionHandle = exceptionHandle
            return this
        }

        override fun returnSrcOnError(): SafetyMosaicBuild {
            this.exceptionHandle = { _, src, _ -> src }
            return this
        }

        override fun returnDefectOnError(): SafetyMosaicBuild {
            this.exceptionHandle = { _, _, defect -> defect }
            return this
        }

        override fun returnOnError(defaultTxt: String): SafetyMosaicBuild {
            this.exceptionHandle = { _, _, _ -> defaultTxt }
            return this
        }

        override fun build(): Mosaic {
            return ActionsMosaic(actions!!.toTypedArray(), exceptionHandle, minLen)
        }

        private fun needGrowCapacity(srcAndResult: Array<CharArray>, minimumCapacity: Int): Boolean {
            if (minimumCapacity > srcAndResult[1].size) {
                srcAndResult[1] = Arrays.copyOf(
                    srcAndResult[1],
                    newCapacity(srcAndResult[1], minimumCapacity)
                )
                return true
            } else {
                return false
            }
        }

        private fun hugeCapacity(minCapacity: Int): Int {
            if (Integer.MAX_VALUE - minCapacity < 0) { // overflow
                throw OutOfMemoryError()
            }
            return if (minCapacity > MAX_ARRAY_SIZE)
                minCapacity
            else
                MAX_ARRAY_SIZE
        }

        private fun newCapacity(value: CharArray, minCapacity: Int): Int {
            // overflow-conscious code
            var newCapacity = (value.size shl 1) + 2
            if (newCapacity - minCapacity < 0) {
                newCapacity = minCapacity
            }
            return if (newCapacity <= 0 || MAX_ARRAY_SIZE - newCapacity < 0)
                hugeCapacity(minCapacity)
            else
                newCapacity
        }

        companion object {

            private val MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8
        }

    }

    interface MosaicAction {
        fun next(srcAndResult: Array<CharArray>, indexes: IntArray)
    }

    interface SimpleMosaicAction : MosaicAction {
        override fun next(srcAndResult: Array<CharArray>, indexes: IntArray) {
            next(srcAndResult[0], srcAndResult[1], indexes)
        }

        fun next(src: CharArray, result: CharArray, indexes: IntArray)
    }

    interface SafetyMosaicAction : MosaicAction {
        override fun next(srcAndResult: Array<CharArray>, indexes: IntArray) {
            next(srcAndResult, srcAndResult[0], srcAndResult[1], indexes)
        }

        fun next(srcAndResult: Array<CharArray>, src: CharArray, result: CharArray, indexes: IntArray)
    }


    private class ActionsMosaic : Mosaic {
        private var actions: Array<MosaicAction>? = null

        private var exceptionHandle: MosaicExceptionHandle? = null

        private var minLen: Int = 0

        constructor(
            actions: kotlin.Array<MosaicUtils.MosaicAction>,
            minLen: kotlin.Int,
            exceptionHandle: MosaicExceptionHandle?
        ) {
            this.actions = actions
            this.minLen = minLen
            this.exceptionHandle = exceptionHandle
        }

        constructor(actions: Array<MosaicAction>, exceptionHandle: MosaicExceptionHandle?, minLen: Int) {
            this.actions = actions
            this.exceptionHandle = exceptionHandle
            this.minLen = minLen
        }

        override fun mosaic(str: String?): String? {
            try {
                val src = str!!.toCharArray()
                val srcAndResult = arrayOf(src, CharArray(Integer.max(src.size, minLen)))
                val indexes = intArrayOf(0, 0)
                try {
                    for (action in actions!!) {
                        action.next(srcAndResult, indexes)
                    }
                } catch (throwable: Throwable) {
                    when {
                        exceptionHandle == null -> throw throwable
                        else -> return exceptionHandle?.invoke(throwable, str, result(srcAndResult[1], indexes[1]))!!
                    }
                }

                return result(srcAndResult[1], indexes[1])
            } catch (throwable: Throwable) {
                return exceptionHandle?.invoke(throwable, str, null) ?: throw throwable
            }

        }

        private fun result(result: CharArray, len: Int): String {
            return if (len == result.size) {
                String(result)
            } else {
                String(result, 0, len)
            }

        }
    }

    fun idCardMosaic(idCard: String?): String? {
        return idCardMosaic.mosaic(idCard)
    }

    fun phoneMosaic(phone: String?): String? {
        return phoneMosaic.mosaic(phone)
    }

    fun nameMosaic(phone: String?): String? {
        return nameMosaic.mosaic(phone)
    }
}

typealias MosaicExceptionHandle = (Throwable, String?, String?) -> String?