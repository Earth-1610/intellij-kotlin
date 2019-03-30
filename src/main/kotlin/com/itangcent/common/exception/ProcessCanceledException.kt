package com.itangcent.common.exception

class ProcessCanceledException : RuntimeException {
    val stopMsg: String?

    constructor(stopMsg: String) : super(stopMsg) {
        this.stopMsg = stopMsg
    }

    constructor(cause: Throwable?) : super(cause) {
        this.stopMsg = cause?.message
    }
}
