package com.itangcent.intellij.util

import com.intellij.uiDesigner.core.GridConstraints
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.logger.Logger
import org.apache.commons.lang3.exception.ExceptionUtils
import java.awt.Dimension
import java.awt.EventQueue
import javax.swing.JScrollPane
import javax.swing.JTextArea


object SwingUtils {

    fun scroll(
        jTextArea: JTextArea,
        row: Int = 0,
        column: Int = 0,
        rowSpan: Int = 1,
        colSpan: Int = 1,
        anchor: Int = GridConstraints.ANCHOR_CENTER,
        fill: Int = GridConstraints.FILL_BOTH,
        hSizePolicy: Int = GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW,
        vSizePolicy: Int = GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW
    ) {
        val actionContext = ActionContext.getContext()
        val logger = actionContext?.instance(Logger::class)
        EventQueue.invokeLater {
            val parent = jTextArea.parent
            jTextArea.autoscrolls = true

//        EventQueue.invokeLater {
//            dialog.pack()
//            dialog.isVisible = true
//        }

            logger?.info("maximumSize:${jTextArea.maximumSize}")
            logger?.info("minimumSize:${jTextArea.minimumSize}")
            logger?.info("preferredSize:${jTextArea.preferredSize}")
            val pane = JScrollPane(jTextArea)
//            pane.layout = com.intellij.uiDesigner.core.GridLayoutManager(1, 1, Insets(0, 0, 0, 0), -1, -1);

//            jTextArea.maximumSize = fixScrollDimension(jTextArea.maximumSize)
//            jTextArea.minimumSize = fixScrollDimension(jTextArea.minimumSize)
//            jTextArea.preferredSize = fixScrollDimension(jTextArea.preferredSize)

            pane.maximumSize = jTextArea.maximumSize
            pane.minimumSize = jTextArea.minimumSize
            pane.preferredSize = jTextArea.preferredSize
            logger?.info("maximumSize:${jTextArea.maximumSize}")
            logger?.info("minimumSize:${jTextArea.minimumSize}")
            logger?.info("preferredSize:${jTextArea.preferredSize}")
            pane.location = jTextArea.location

//            parent.remove(jTextArea)

//        val layoutManager = parent.layout
//        if (layoutManager != null) {
//            parent.add(pane, BorderLayout.CENTER)
//        }

            try {
                parent.add(
                    pane, GridConstraints(
                        row, column, rowSpan, colSpan, anchor,
                        fill, hSizePolicy,
                        vSizePolicy,
                        pane.minimumSize, pane.preferredSize, pane.maximumSize, 0, false
                    )
                )
//                parent.add(pane, GridConstraints(row, column, rowSpan, colSpan, anchor,
//                        fill, hSizePolicy,
//                        vSizePolicy,
//                        null, null, null, 0, false))
//                parent.add(pane, GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER,
//                        GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW,
//                        GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW,
//                        pane.minimumSize, pane.preferredSize, pane.maximumSize, 0, false))
            } catch (e: Throwable) {
                actionContext?.instance(Logger::class)?.error("error to add:" + ExceptionUtils.getStackTrace(e))
                try {
                    parent.add(pane)
                } catch (e: Throwable) {
                    actionContext?.instance(Logger::class)?.error("error to add:" + ExceptionUtils.getStackTrace(e))
                }
            }
        }
    }

    fun fixScrollWidth(dimension: Dimension): Dimension {
        return Dimension(dimension.width - 10, dimension.height)
    }

    fun fixScrollDimension(dimension: Dimension): Dimension {
        return Dimension(dimension.width - 30, dimension.height - 30)
    }
}