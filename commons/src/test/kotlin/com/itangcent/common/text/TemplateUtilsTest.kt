package com.itangcent.common.text

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Test case of [TemplateUtils]
 */
internal class TemplateUtilsTest {

    @Test
    fun testRender() {

        val context = mapOf<String?, Any?>("min" to 0, "max" to 99)
        //render with $ by default --------------------------------------------------------------------------------
        Assertions.assertEquals("min-max", TemplateUtils.render("min-max", emptyMap<String?, Any?>()))
        Assertions.assertEquals("-#{max}", TemplateUtils.render("\${min}-#{max}", emptyMap<String?, Any?>()))
        Assertions.assertEquals("0-#{max}", TemplateUtils.render("\${min}-#{max}", mapOf<String?, Any?>("min" to 0)))
        Assertions.assertEquals("-#{max}", TemplateUtils.render("\${min}-#{max}", mapOf<String?, Any?>("max" to 99)))
        Assertions.assertEquals(
            "0-#{max}",
            TemplateUtils.render("\${min}-#{max}", context)
        )
        Assertions.assertEquals(
            "min-#{\"max\"}",
            TemplateUtils.render("\${'min'}-#{\"max\"}", emptyMap<String?, Any?>())
        )
        Assertions.assertEquals(
            "min-#{\"max\"}",
            TemplateUtils.render("\${'min'}-#{\"max\"}", context)
        )
        Assertions.assertEquals(
            "0-99",
            TemplateUtils.render("\${min}-\${max}", context)
        )


        //render with $ and # --------------------------------------------------------------------------------

        Assertions.assertEquals(
            "min-max",
            TemplateUtils.render(
                "min-max",
                charArrayOf('$', '#'), emptyMap<String?, Any?>()
            )
        )
        Assertions.assertEquals(
            "-",
            TemplateUtils.render(
                "\${min}-#{max}",
                charArrayOf('$', '#'), emptyMap<String?, Any?>()
            )
        )
        Assertions.assertEquals(
            "0-",
            TemplateUtils.render(
                "\${min}-#{max}",
                charArrayOf('$', '#'), mapOf<String?, Any?>("min" to 0)
            )
        )
        Assertions.assertEquals(
            "-99",
            TemplateUtils.render(
                "\${min}-#{max}",
                charArrayOf('$', '#'), mapOf<String?, Any?>("max" to 99)
            )
        )
        Assertions.assertEquals(
            "0-99",
            TemplateUtils.render(
                "\${min}-#{max}",
                charArrayOf('$', '#'), context
            )
        )
        Assertions.assertEquals(
            "min-max",
            TemplateUtils.render(
                "\${'min'}-#{\"max\"}",
                charArrayOf('$', '#'), emptyMap<String?, Any?>()
            )
        )
        Assertions.assertEquals(
            "min-max",
            TemplateUtils.render(
                "\${'min'}-#{\"max\"}",
                charArrayOf('$', '#'), context
            )
        )

        //render with groovy --------------------------------------------------------------------------------

        Assertions.assertEquals(
            "There must be at least 1 character",
            TemplateUtils.render(
                "There must be at least \${value} character\${value > 1 ? 's' : ''}",
                mapOf<String?, Any?>("value" to 1)
            )
        )
        Assertions.assertEquals(
            "There must be at least 2 characters",
            TemplateUtils.render(
                "There must be at least \${value} character\${value > 1 ? 's' : ''}",
                mapOf<String?, Any?>("value" to 2)
            )
        )

        //render with TemplateRenderBuilder
        assertEquals("0-99", TemplateUtils.render("\${min}-#{max}")
            .placeholder(charArrayOf('$', '#'))
            .context(context)
            .onEval { property, resolved ->
                assertEquals(context[property], resolved)
            }
            .render())
        assertEquals(
            "min-#{max}", TemplateUtils.render("\${min}-#{max}")
                .templateEvaluator(TemplateEvaluator.nop())
                .render()
        )
    }
}