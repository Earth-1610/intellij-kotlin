package com.itangcent.common.shell

import org.apache.commons.lang3.StringUtils

/**
 * result of command
 *
 *  * [CommandResult.result] means result of command, 0 means normal, else means error, same to excute in
 * linux shell
 *  * [CommandResult.successMsg] means success message of command result
 *  * [CommandResult.errorMsg] means error message of command result
 *
 *
 * @author [Trinea](http://www.trinea.cn) 2013-5-16
 */
class CommandResult {
    constructor()
    constructor(result: Int, successMsg: String?, errorMsg: String?) {
        this.result = result
        this.successMsg = successMsg
        this.errorMsg = errorMsg
    }

    /**
     * result of command
     */
    var result: Int = 0
    /**
     * success message of command result
     */
    var successMsg: String? = null
    /**
     * error message of command result
     */
    var errorMsg: String? = null

    val msg: String
        get() = if (StringUtils.isEmpty(this.successMsg)) {
            if (StringUtils.isEmpty(this.errorMsg)) "" else this.errorMsg!!
        } else if (StringUtils.isEmpty(this.errorMsg)) {
            this.successMsg!!
        } else {
            this.successMsg + "\n" + this.errorMsg
        }

}
