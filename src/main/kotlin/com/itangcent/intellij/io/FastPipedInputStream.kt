package com.itangcent.intellij.io

import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.experimental.and

class FastPipedInputStream : InputStream {

    internal var closedByWriter = false
    @Volatile
    internal var closedByReader = false
    internal var connected = false

    /* REMIND: identification of the read and write sides needs to be
       more sophisticated.  Either using thread groups (but what about
       pipes within a thread?) or using finalization (but it may be a
       long time until the next GC). */
    internal var readSide: Thread? = null
    internal var writeSide: Thread? = null

    internal var lock: ReentrantLock = ReentrantLock()

    internal var condition: Condition = lock.newCondition()

    /**
     * The circular buffer into which incoming holdData is placed.
     *
     * @since JDK1.1
     */
    protected lateinit var buffer: ByteArray

    /**
     * The index of the position inIndex the circular buffer at which the
     * next byte of holdData will be stored when received from the connected
     * piped output stream. `inIndex<0` implies the buffer is empty,
     * `inIndex==outIndex` implies the buffer is full
     *
     * @since JDK1.1
     */
    internal var inIndex = -1

    /**
     * The index of the position inIndex the circular buffer at which the next
     * byte of holdData will be read by this piped input stream.
     *
     * @since JDK1.1
     */
    internal var outIndex = 0

    /**
     * Creates a `PipedInputStream` so that it is
     * connected to the piped output stream
     * `src` and uses the specified pipe size for
     * the pipe's buffer.
     * Data bytes written to `src` will then
     * be available as input from this stream.
     *
     * @param src      the stream to connect to.
     * @param pipeSize the size of the pipe's buffer.
     * @throws IOException              if an I/O error occurs.
     * @throws IllegalArgumentException if `pipeSize <= 0`.
     * @since 1.6
     */
    @Throws(IOException::class)
    @JvmOverloads
    constructor(src: FastPipedOutputStream, pipeSize: Int = DEFAULT_PIPE_SIZE) {
        initPipe(pipeSize)
        connect(src)
    }

    /**
     * Creates a `PipedInputStream` so
     * that it is not yet [ connected][.connect].
     * It must be [connected][com.itangcent.intellij.io.FastPipedOutputStream.connect] to a
     * `PipedOutputStream` before being used.
     */
    constructor() {
        initPipe(DEFAULT_PIPE_SIZE)
    }

    /**
     * Creates a `PipedInputStream` so that it is not yet
     * [connected][.connect] and
     * uses the specified pipe size for the pipe's buffer.
     * It must be [ connected][com.itangcent.intellij.io.FastPipedOutputStream.connect] to a `PipedOutputStream` before being used.
     *
     * @param pipeSize the size of the pipe's buffer.
     * @throws IllegalArgumentException if `pipeSize <= 0`.
     * @since 1.6
     */
    constructor(pipeSize: Int) {
        initPipe(pipeSize)
    }

    private fun initPipe(pipeSize: Int) {
        if (pipeSize <= 0) {
            throw IllegalArgumentException("Pipe Size <= 0")
        }
        buffer = ByteArray(pipeSize)
    }

    /**
     * Causes this piped input stream to be connected
     * to the piped  output stream `src`.
     * If this object is already connected to some
     * other piped output  stream, an `IOException`
     * is thrown.
     *
     *
     * If `src` is an
     * unconnected piped output stream and `snk`
     * is an unconnected piped input stream, they
     * may be connected by either the call:
     *
     *
     * <pre>`snk.connect(src)` </pre>
     *
     * or the call:
     *
     * <pre>`src.connect(snk)` </pre>
     *
     * The two calls have the same effect.
     *
     * @param src The piped output stream to connect to.
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    fun connect(src: FastPipedOutputStream) {
        src.connect(this)
    }

    /**
     * Receives a byte of holdData.  This method will block if no input is
     * available.
     *
     * @param b the byte being received
     * @throws IOException If the pipe is [ `broken`](#BROKEN),
     * [unconnected][.connect],
     * closed, or if an I/O error occurs.
     * @since JDK1.1
     */
    @Throws(IOException::class)
    internal fun receive(b: Int) {
        lock.withLock {
            checkStateForReceive()
            writeSide = Thread.currentThread()
            if (inIndex == outIndex)
                awaitSpace()
            if (inIndex < 0) {
                inIndex = 0
                outIndex = 0
            }
            buffer[inIndex++] = (b and 0xFF).toByte()
            if (inIndex >= buffer.size) {
                inIndex = 0
            }
        }
    }

    /**
     * Receives holdData into an array of bytes.  This method will
     * block until some input is available.
     *
     * @param b   the buffer into which the holdData is received
     * @param off the start offset of the holdData
     * @param len the maximum number of bytes received
     * @throws IOException If the pipe is [ broken](#BROKEN),
     * [unconnected][.connect],
     * closed,or if an I/O error occurs.
     */
    @Throws(IOException::class)
    internal fun receive(b: ByteArray, off: Int, len: Int) {
        lock.withLock {

            var _off = off
            checkStateForReceive()
            writeSide = Thread.currentThread()
            var bytesToTransfer = len
            while (bytesToTransfer > 0) {
                if (inIndex == outIndex)
                    awaitSpace()
                var nextTransferAmount = 0
                if (outIndex < inIndex) {
                    nextTransferAmount = buffer.size - inIndex
                } else if (inIndex < outIndex) {
                    if (inIndex == -1) {
                        outIndex = 0
                        inIndex = outIndex
                        nextTransferAmount = buffer.size - inIndex
                    } else {
                        nextTransferAmount = outIndex - inIndex
                    }
                }
                if (nextTransferAmount > bytesToTransfer)
                    nextTransferAmount = bytesToTransfer
                assert(nextTransferAmount > 0)
                System.arraycopy(b, _off, buffer, inIndex, nextTransferAmount)
                bytesToTransfer -= nextTransferAmount
                _off += nextTransferAmount
                inIndex += nextTransferAmount
                if (inIndex >= buffer.size) {
                    inIndex = 0
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun checkStateForReceive() {
        if (!connected) {
            throw IOException("Pipe not connected")
        } else if (closedByWriter || closedByReader) {
            throw IOException("Pipe closed")
        } else if (readSide != null && !readSide!!.isAlive) {
            throw IOException("Read end dead")
        }
    }

    @Throws(IOException::class)
    private fun awaitSpace() {
        while (inIndex == outIndex) {
            checkStateForReceive()

            /* full: kick any waiting readers */
            condition.signalAll()
            try {
                condition.await(100, TimeUnit.MILLISECONDS)
            } catch (ex: InterruptedException) {
                throw java.io.InterruptedIOException()
            }

        }
    }

    /**
     * Notifies all waiting threads that the last byte of holdData has been
     * received.
     */
    internal fun receivedLast() {
        lock.withLock {
            closedByWriter = true
            condition.signalAll()
        }
    }

    /**
     * Reads the next byte of holdData from this piped input stream. The
     * value byte is returned as an `int` inIndex the range
     * `0` to `255`.
     * This method blocks until input holdData is available, the end of the
     * stream is detected, or an exception is thrown.
     *
     * @return the next byte of holdData, or `-1` if the end of the
     * stream is reached.
     * @throws IOException if the pipe is
     * [unconnected][.connect],
     * [ `broken`](#BROKEN), closed,
     * or if an I/O error occurs.
     */
    @Throws(IOException::class)
    override fun read(): Int {
        lock.withLock {
            if (!connected) {
                throw IOException("Pipe not connected")
            } else if (closedByReader) {
                throw IOException("Pipe closed")
            } else if (writeSide != null && !writeSide!!.isAlive
                && !closedByWriter && inIndex < 0
            ) {
                throw IOException("Write end dead")
            }

            readSide = Thread.currentThread()
            var trials = 2
            while (inIndex < 0) {
                if (closedByWriter) {
                    /* closed by writer, return EOF */
                    return -1
                }
                if (writeSide != null && !writeSide!!.isAlive && --trials < 0) {
                    throw IOException("Pipe broken")
                }
                /* might be a writer waiting */
                condition.signalAll()
                try {
                    condition.await(100, TimeUnit.MILLISECONDS)
                } catch (ex: InterruptedException) {
                    throw java.io.InterruptedIOException()
                }

            }
            val ret = buffer[outIndex++] and 0xFF.toByte()
            if (outIndex >= buffer.size) {
                outIndex = 0
            }
            if (inIndex == outIndex) {
                /* now empty */
                inIndex = -1
            }

            return ret.toInt()
        }
    }

    /**
     * Reads up to `len` bytes of holdData from this piped input
     * stream into an array of bytes. Less than `len` bytes
     * will be read if the end of the holdData stream is reached or if
     * `len` exceeds the pipe's buffer size.
     * If `len ` is zero, then no bytes are read and 0 is returned;
     * otherwise, the method blocks until at least 1 byte of input is
     * available, end of the stream has been detected, or an exception is
     * thrown.
     *
     * @param b   the buffer into which the holdData is read.
     * @param off the start offset inIndex the destination array `b`
     * @param len the maximum number of bytes read.
     * @return the total number of bytes read into the buffer, or
     * `-1` if there is no more holdData because the end of
     * the stream has been reached.
     * @throws NullPointerException      If `b` is `null`.
     * @throws IndexOutOfBoundsException If `off` is negative,
     * `len` is negative, or `len` is greater than
     * `b.length - off`
     * @throws IOException               if the pipe is [ `broken`](#BROKEN),
     * [unconnected][.connect],
     * closed, or if an I/O error occurs.
     */
    @Throws(IOException::class)
    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        lock.withLock {
            var _len = len
            if (b == null) {
                throw NullPointerException()
            } else if (off < 0 || _len < 0 || _len > b.size - off) {
                throw IndexOutOfBoundsException()
            } else if (_len == 0) {
                return 0
            }

            /* possibly wait on the first character */
            val c = read()
            if (c < 0) {
                return -1
            }
            b[off] = c.toByte()
            var rlen = 1
            while (inIndex >= 0 && _len > 1) {

                var available: Int

                if (inIndex > outIndex) {
                    available = Math.min(buffer.size - outIndex, inIndex - outIndex)
                } else {
                    available = buffer.size - outIndex
                }

                // A byte is read beforehand outside the loop
                if (available > _len - 1) {
                    available = _len - 1
                }
                System.arraycopy(buffer, outIndex, b, off + rlen, available)
                outIndex += available
                rlen += available
                _len -= available

                if (outIndex >= buffer.size) {
                    outIndex = 0
                }
                if (inIndex == outIndex) {
                    /* now empty */
                    inIndex = -1
                }
            }
            return rlen
        }
    }

    /**
     * Returns the number of bytes that can be read from this input
     * stream without blocking.
     *
     * @return the number of bytes that can be read from this input stream
     * without blocking, or `0` if this input stream has been
     * closed by invoking its [.close] method, or if the pipe
     * is [unconnected][.connect], or
     * [ `broken`](#BROKEN).
     * @throws IOException if an I/O error occurs.
     * @since JDK1.0.2
     */
    @Throws(IOException::class)
    override fun available(): Int {
        lock.withLock {
            return when {
                inIndex < 0 -> 0
                inIndex == outIndex -> buffer.size
                inIndex > outIndex -> inIndex - outIndex
                else -> inIndex + buffer.size - outIndex
            }
        }
    }

    /**
     * Closes this piped input stream and releases any system resources
     * associated with the stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    override fun close() {
        closedByReader = true
        synchronized(this) {
            inIndex = -1
        }
    }

    companion object {

        private val DEFAULT_PIPE_SIZE = 1024

        /**
         * The default size of the pipe's circular input buffer.
         *
         * @since JDK1.1
         */
        // This used to be a constant before the pipe size was allowed
        // to change. This field will continue to be maintained
        // for backward compatibility.
        protected val PIPE_SIZE = DEFAULT_PIPE_SIZE
    }
}