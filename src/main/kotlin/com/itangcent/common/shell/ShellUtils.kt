package com.itangcent.common.shell

interface ShellUtils {
    fun checkRootPermission(): Boolean


    /**
     * output real time
     */
    fun liveReport(): Boolean {
        return false
    }

    fun closeLiveReport() {}

    fun execCommand(command: String?, isRoot: Boolean): CommandResult

    fun execCommand(commands: List<String>?, isRoot: Boolean): CommandResult

    fun execCommand(commands: Array<String>?, isRoot: Boolean): CommandResult

    fun execCommand(command: String?, isRoot: Boolean, isNeedResultMsg: Boolean): CommandResult

    fun execCommand(commands: List<String>?, isRoot: Boolean, isNeedResultMsg: Boolean): CommandResult

    fun execCommand(commands: Array<String>?, isRoot: Boolean, isNeedResultMsg: Boolean): CommandResult
}
