package com.itangcent.intellij.jvm.scala.adaptor

import com.itangcent.common.utils.invokeMethod
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeArgs
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement
import scala.collection.Seq


object ScTypeArgsAdaptor {

    @Suppress("UNCHECKED_CAST")
    fun ScTypeArgs.args(): Seq<ScTypeElement>? {
        return try {
            this.invokeMethod("typeArgs")
                    as? Seq<ScTypeElement>
        } catch (e: Exception) {
            null
        }
    }
}