package com.itangcent.intellij.io

import com.itangcent.common.concurrent.ValueHolder
import com.itangcent.common.logger.Log
import com.itangcent.common.utils.IOUtils
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class PipedProcess : Process() {


    private var outputStream: FastPipedOutputStream? = null
    private var inputStream: FastPipedInputStream? = null
    private var errorInputStream: FastPipedInputStream? = null


    private var inputForOutputStream: FastPipedInputStream? = null
    private var outForInputStream: FastPipedOutputStream? = null
    private var outForErrorInputStream: FastPipedOutputStream? = null

    private val exitValueHolder = ValueHolder<Int>()

    init {
        try {
            this.inputStream = FastPipedInputStream(2048)
            this.outForInputStream = FastPipedOutputStream(inputStream!!)

            this.errorInputStream = FastPipedInputStream(2048)
            this.outForErrorInputStream = FastPipedOutputStream(errorInputStream!!)

            this.inputForOutputStream = FastPipedInputStream(2048)
            this.outputStream = FastPipedOutputStream(inputForOutputStream!!)

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun getOutputStream(): OutputStream? {
        return outputStream
    }

    override fun getInputStream(): InputStream? {
        return inputStream
    }

    override fun getErrorStream(): InputStream? {
        return errorInputStream
    }

    fun getInputForOutputStream(): InputStream? {
        return inputForOutputStream
    }

    fun getOutForInputStream(): OutputStream? {
        return outForInputStream
    }

    fun getOutForErrorInputStream(): OutputStream? {
        return outForErrorInputStream
    }

    fun setExitValue(exitValue: Int) {
        this.exitValueHolder.success(exitValue)
        destroy()
    }

    @Throws(InterruptedException::class)
    override fun waitFor(): Int {
        val ret = exitValueHolder.value() ?: 0
        LOG.info("PipedProcess exit for $ret")
        return ret
    }

    override fun exitValue(): Int {
        return exitValueHolder.peek() ?: throw IllegalThreadStateException()
    }

    override fun destroy() {
        IOUtils.closeQuietly(
            this.errorInputStream,
            this.inputForOutputStream,
            this.inputStream,
            this.outForErrorInputStream,
            this.outForInputStream,
            this.outputStream
        )
    }

    companion object : Log()
}
