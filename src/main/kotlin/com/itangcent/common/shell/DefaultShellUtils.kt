package com.itangcent.common.shell


import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * copied from https://github.com/Trinea/android-common/edit/master/src/cn/trinea/android/common/util/ShellUtils.java
 * ShellUtils
 * **Check root**
 *
 *  * [DefaultShellUtils.checkRootPermission]
 *
 * **Execte command**
 *
 *  * [DefaultShellUtils.execCommand]
 *  * [DefaultShellUtils.execCommand]
 *  * [DefaultShellUtils.execCommand]
 *  * [DefaultShellUtils.execCommand]
 *  * [DefaultShellUtils.execCommand]
 *  * [DefaultShellUtils.execCommand]
 *
 */
class DefaultShellUtils : AbstractShellUtils() {

    /**
     * execute shell commands
     *
     * @param commands        command array
     * @param isRoot          whether need to run with root
     * @param isNeedResultMsg whether need result msg
     * @return
     *  * if isNeedResultMsg is false, [CommandResult.successMsg] is null and
     * [CommandResult.errorMsg] is null.
     *  * if [CommandResult.result] is -1, there maybe some excepiton.
     *
     */
    override fun execCommand(commands: Array<String>?, isRoot: Boolean, isNeedResultMsg: Boolean): CommandResult {
        var result = -1
        if (commands == null || commands.size == 0) {
            return CommandResult(result, null, null)
        }

        var process: Process? = null
        var successResult: BufferedReader? = null
        var errorResult: BufferedReader? = null
        var successMsg: StringBuilder? = null
        var errorMsg: StringBuilder? = null

        var os: DataOutputStream? = null
        try {
            process = Runtime.getRuntime().exec(if (isRoot) COMMAND_SU else COMMAND_SH)
            os = DataOutputStream(process!!.outputStream)
            for (command in commands) {
                if (command == null) {
                    continue
                }

                // donnot use os.writeBytes(commmand), avoid chinese charset error
                os.write(command.toByteArray())
                os.writeBytes(COMMAND_LINE_END)
                os.flush()
            }
            os.writeBytes(COMMAND_EXIT)
            os.flush()

            result = process.waitFor()
            // get command result
            if (isNeedResultMsg) {
                successMsg = StringBuilder()
                errorMsg = StringBuilder()
                successResult = BufferedReader(InputStreamReader(process.inputStream))
                errorResult = BufferedReader(InputStreamReader(process.errorStream))
                var s: String? = successResult.readLine()
                while (s != null) {
                    successMsg.append(s)
                    successMsg.append(NEW_LINE)
                    s = successResult.readLine()
                }
                s = errorResult.readLine()
                while (s != null) {
                    errorMsg.append(s)
                    errorMsg.append(NEW_LINE)
                    s = errorResult.readLine()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                os?.close()
                successResult?.close()
                errorResult?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            process?.destroy()
        }
        return CommandResult(result, successMsg?.toString(), errorMsg?.toString())
    }

    companion object {
        /**
         * Gets the line separator for the current system.
         */
        val NEW_LINE = System.getProperty("line.separator")

        val COMMAND_SU = "su"
        val COMMAND_SH = "sh"
        val COMMAND_EXIT = "exit\n"
        val COMMAND_LINE_END = "\n"
    }

}