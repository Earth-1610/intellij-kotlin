package com.itangcent.common.shell

abstract class AbstractShellUtils : ShellUtils {
    /**
     * check whether has root permission
     *
     * @return
     */
    override fun checkRootPermission(): Boolean {
        return execCommand("echo root", true, false).result == 0
    }

    /**
     * execute shell command, default return result msg
     *
     * @param command command
     * @param isRoot  whether need to run with root
     * @return
     * @see DefaultShellUtils.execCommand
     */
    override fun execCommand(command: String?, isRoot: Boolean): CommandResult {
        return execCommand(
            when (command) {
                null -> arrayOf()
                else -> arrayOf(command)
            },
            isRoot,
            true
        )
    }

    /**
     * execute shell commands, default return result msg
     *
     * @param commands command list
     * @param isRoot   whether need to run with root
     * @return
     * @see DefaultShellUtils.execCommand
     */
    override fun execCommand(commands: List<String>?, isRoot: Boolean): CommandResult {
        return execCommand(commands?.toTypedArray(), isRoot, true)
    }

    /**
     * execute shell commands, default return result msg
     *
     * @param commands command array
     * @param isRoot   whether need to run with root
     * @return
     * @see DefaultShellUtils.execCommand
     */
    override fun execCommand(commands: Array<String>?, isRoot: Boolean): CommandResult {
        return execCommand(commands, isRoot, true)
    }

    /**
     * execute shell command
     *
     * @param command         command
     * @param isRoot          whether need to run with root
     * @param isNeedResultMsg whether need result msg
     * @return
     * @see DefaultShellUtils.execCommand
     */
    override fun execCommand(command: String?, isRoot: Boolean, isNeedResultMsg: Boolean): CommandResult {
        return execCommand(
            when (command) {
                null -> arrayOf()
                else -> arrayOf(command)
            }, isRoot, isNeedResultMsg
        )
    }

    /**
     * execute shell commands
     *
     * @param commands        command list
     * @param isRoot          whether need to run with root
     * @param isNeedResultMsg whether need result msg
     * @return
     * @see DefaultShellUtils.execCommand
     */
    override fun execCommand(commands: List<String>?, isRoot: Boolean, isNeedResultMsg: Boolean): CommandResult {
        return execCommand(commands?.toTypedArray(), isRoot, isNeedResultMsg)
    }
}
