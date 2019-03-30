package com.itangcent.intellij.io

import java.io.IOException
import java.io.OutputStream
import kotlin.concurrent.withLock

class FastPipedOutputStream : OutputStream {

    /* REMIND: identification of the read and write sides needs to be
       more sophisticated.  Either using thread groups (but what about
       pipes within a thread?) or using finalization (but it may be a
       long time until the next GC). */
    private var sink: FastPipedInputStream? = null

    /**
     * Creates a piped output stream connected to the specified piped
     * input stream. Data bytes written to this stream will then be
     * available as input from `snk`.
     *
     * @param snk The piped input stream to connect to.
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    constructor(snk: FastPipedInputStream) {
        connect(snk)
    }

    /**
     * Creates a piped output stream that is not yet connected to a
     * piped input stream. It must be connected to a piped input stream,
     * either by the receiver or the sender, before being used.
     *
     * @see com.itangcent.intellij.io.FastPipedInputStream.connect
     * @see com.itangcent.intellij.io.FastPipedOutputStream.connect
     */
    constructor() {}

    /**
     * Connects this piped output stream to a receiver. If this object
     * is already connected to some other piped input stream, an
     * `IOException` is thrown.
     *
     *
     * If `snk` is an unconnected piped input stream and
     * `src` is an unconnected piped output stream, they may
     * be connected by either the call:
     * <blockquote><pre>
     * src.connect(snk)</pre></blockquote>
     * or the call:
     * <blockquote><pre>
     * snk.connect(src)</pre></blockquote>
     * The two calls have the same effect.
     *
     * @param snk the piped input stream to connect to.
     * @throws IOException if an I/O error occurs.
     */
    @Synchronized
    @Throws(IOException::class)
    fun connect(snk: FastPipedInputStream?) {
        if (snk == null) {
            throw NullPointerException()
        } else if (sink != null || snk.connected) {
            throw IOException("Already connected")
        }
        sink = snk
        snk.inIndex = -1
        snk.outIndex = 0
        snk.connected = true
    }

    /**
     * Writes the specified `byte` to the piped output stream.
     *
     *
     * Implements the `write` method of `OutputStream`.
     *
     * @param b the `byte` to be written.
     * @throws IOException if the pipe is [ broken](#BROKEN),
     * [unconnected][.connect],
     * closed, or if an I/O error occurs.
     */
    @Throws(IOException::class)
    override fun write(b: Int) {
        if (sink == null) {
            throw IOException("Pipe not connected")
        }
        sink!!.receive(b)
    }

    /**
     * Writes `len` bytes from the specified byte array
     * starting at offset `off` to this piped output stream.
     * This method blocks until all the bytes are written to the output
     * stream.
     *
     * @param b   the holdData.
     * @param off the start offset inIndex the holdData.
     * @param len the number of bytes to write.
     * @throws IOException if the pipe is [ broken](#BROKEN),
     * [unconnected][.connect],
     * closed, or if an I/O error occurs.
     */
    @Throws(IOException::class)
    override fun write(b: ByteArray?, off: Int, len: Int) {
        if (sink == null) {
            throw IOException("Pipe not connected")
        } else if (b == null) {
            throw NullPointerException()
        } else if (off < 0 || off > b.size || len < 0 ||
            off + len > b.size || off + len < 0
        ) {
            throw IndexOutOfBoundsException()
        } else if (len == 0) {
            return
        }
        sink!!.receive(b, off, len)
    }

    /**
     * Flushes this output stream and forces any buffered output bytes
     * to be written outIndex.
     * This will notify any readers that bytes are waiting inIndex the pipe.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Synchronized
    @Throws(IOException::class)
    override fun flush() {
        if (sink != null) {
            sink!!.lock.withLock {
                sink!!.condition.signalAll()
            }
        }
    }

    /**
     * Closes this piped output stream and releases any system resources
     * associated with this stream. This stream may no longer be used for
     * writing bytes.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    override fun close() {
        if (sink != null) {
            sink!!.receivedLast()
        }
    }
}
