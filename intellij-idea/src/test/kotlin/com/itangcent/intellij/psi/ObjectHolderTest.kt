package com.itangcent.intellij.psi

import com.itangcent.common.utils.GsonUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import kotlin.test.*

/**
 * Test case of [PsiClassUtils]
 */
internal class ObjectHolderTest {

    @Test
    fun asObjectHolder() {
        assertSame(NULL_OBJECT_HOLDER, null.asObjectHolder())
        assertSame(NULL_OBJECT_HOLDER, NULL_OBJECT_HOLDER.asObjectHolder())
        assertTrue(emptyArray<Any?>().asObjectHolder() is ArrayObjectHolder)
        assertTrue(emptyList<Any?>().asObjectHolder() is CollectionObjectHolder)
        assertTrue(emptyMap<Any?, Any?>().asObjectHolder() is MapObjectHolder)
        assertTrue(1.asObjectHolder() is ResolvedObjectHolder)
    }

    @Test
    fun notResolved() {
        assertFalse(NULL_OBJECT_HOLDER.notResolved())
        assertTrue(emptyArray<Any?>().asObjectHolder().notResolved())
        assertTrue(emptyList<Any?>().asObjectHolder().notResolved())
        assertTrue(emptyMap<Any?, Any?>().asObjectHolder().notResolved())
        assertFalse(1.asObjectHolder().notResolved())
    }

    @Test
    fun getOrResolve() {
        assertNull((null as ObjectHolder?).getOrResolve())
        assertEquals("str", "str".asObjectHolder().getOrResolve())
        assertEquals(1, 1.asObjectHolder().getOrResolve())
        val objectHolder =
            arrayListOf(hashMapOf("key".asObjectHolder() to null.asObjectHolder()).asObjectHolder()).asObjectHolder()
        assertEquals("[{\"key\":null}]", GsonUtils.toJsonWithNulls(objectHolder.getOrResolve()))
        assertNotSame(objectHolder.getOrResolve(), objectHolder.getOrResolve())
    }

    @Test
    fun upgrade() {
        val objectHolder = NULL_OBJECT_HOLDER.upgrade()
        assertSame(objectHolder, objectHolder.upgrade())
    }

    @Test
    fun extend() {
        val objectHolder = NULL_OBJECT_HOLDER.extend()
        assertSame(objectHolder, objectHolder.extend())
    }

    @Test
    fun parent() {
        val context = SimpleContext()
        assertNull(context.parent())

        context.pushHolder(NULL_OBJECT_HOLDER, null)
        assertSame(NULL_OBJECT_HOLDER, context.parent())

        val objectHolder = "str".asObjectHolder()
        context.pushHolder(objectHolder, null)
        assertSame(objectHolder, context.parent())
    }

    @Test
    fun nearestMap() {
        val context = SimpleContext()
        assertNull(context.nearestMap())

        context.pushHolder(NULL_OBJECT_HOLDER, null)
        assertNull(context.nearestMap())

        val objectHolder = hashMapOf("key" to 1).asObjectHolder()
        context.pushHolder(objectHolder, null)
        assertSame(objectHolder, context.nearestMap())

        val strObjectHolder = "str".asObjectHolder()
        context.pushHolder(strObjectHolder, null)
        assertSame(objectHolder, context.nearestMap())
    }

    @Test
    fun nearestProperty() {
        val context = SimpleContext()
        assertNull(context.nearestProperty())

        context.pushHolder(NULL_OBJECT_HOLDER, null)
        assertNull(context.nearestProperty())

        context.pushHolder(NULL_OBJECT_HOLDER, "key")
        assertSame("key", context.nearestProperty())

        context.pushHolder(NULL_OBJECT_HOLDER, null)
        assertSame("key", context.nearestProperty())

        context.pushHolder(NULL_OBJECT_HOLDER, "beta")
        assertSame("beta", context.nearestProperty())
    }

    @Test
    fun with() {
        val context = SimpleContext()
        context.pushHolder(NULL_OBJECT_HOLDER, null)

        assertEquals(1, context.holders().size)
        context.with(NULL_OBJECT_HOLDER) {
            assertEquals(2, it.holders().size)
            assertNull(context.nearestProperty())
        }
        assertEquals(1, context.holders().size)

        context.with(NULL_OBJECT_HOLDER, "key") {
            assertEquals(2, it.holders().size)
            assertEquals("key", context.nearestProperty())
        }
        assertEquals(1, context.holders().size)
    }

    @Test
    fun testResolvedObjectHolder() {
        val objectHolder = 1.asObjectHolder() as ResolvedObjectHolder
        assertTrue(objectHolder.resolved())
        assertEquals(1, objectHolder.circularEliminate())
        assertEquals(1, objectHolder.getObject())

        objectHolder.resolve(null)
        objectHolder.onResolve(SimpleContext())

        assertEquals(1, objectHolder.circularEliminate())
        assertEquals(1, objectHolder.getObject())
        objectHolder.collectUnResolvedObjectHolders {
            fail()
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testArrayObjectHolder() {
        run {
            val array: Array<Any?> = arrayOf("a", "b")
            val objectHolder = array.asObjectHolder() as ArrayObjectHolder

            assertFalse(objectHolder.resolved())
            assertSame(array, objectHolder.getObject())
            assertEquals(
                "[\"a\",\"b\"]",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )
            objectHolder.collectUnResolvedObjectHolders {
                fail()
            }

            objectHolder.resolve(null)
            assertTrue(objectHolder.resolved())
            assertArrayEquals(array, objectHolder.getObject() as Array<out Any>)
            assertArrayEquals(array, objectHolder.circularEliminate() as Array<out Any>)
        }
        run {
            val map: HashMap<String, Any?> = linkedMapOf("key" to 1)
            val mapObjectHolder = map.asObjectHolder()
            val array: Array<Any?> = arrayOf("a", "b", "x", mapObjectHolder)
            val objectHolder = array.asObjectHolder() as ArrayObjectHolder
            array[2] = objectHolder
            map["array"] = objectHolder

            assertFalse(objectHolder.resolved())
            assertSame(array, objectHolder.getObject())
            assertEquals(
                "[\"a\",\"b\",[],{\"key\":1,\"array\":[]}]",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )
            val holders = ArrayList<ObjectHolder>()
            objectHolder.collectUnResolvedObjectHolders {
                holders.add(it)
            }
            assertEquals(2, holders.size)
            assertEquals(objectHolder, holders[0])
            assertEquals(mapObjectHolder, holders[1])

            objectHolder.resolve(null)
            assertFalse(objectHolder.resolved())
            assertEquals(
                "[\"a\",\"b\",[],{\"key\":1,\"array\":[]}]",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )

            SimpleContext().with(objectHolder, null) {
                objectHolder.resolve(it)
            }
            assertTrue(objectHolder.resolved())
            assertEquals(
                "[\"a\",\"b\",[\"a\",\"b\",[\"a\",\"b\",[],{\"key\":1,\"array\":[]}],{\"key\":1,\"array\":[\"a\",\"b\",[],{\"key\":1,\"array\":[]}]}],{\"key\":1,\"array\":[\"a\",\"b\",[],{\"key\":1,\"array\":[]}]}]",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )
            assertEquals(
                "[\"a\",\"b\",[\"a\",\"b\",[],{\"key\":1,\"array\":[]}],{\"key\":1,\"array\":[\"a\",\"b\",[],{\"key\":1,\"array\":[]}]}]",
                GsonUtils.toJson(objectHolder.getObject())
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testCollectionObjectHolder() {
        run {
            val list: ArrayList<Any?> = arrayListOf("a", "b")
            val objectHolder = list.asObjectHolder() as CollectionObjectHolder

            assertFalse(objectHolder.resolved())
            assertSame(list, objectHolder.getObject())
            assertEquals(
                "[\"a\",\"b\"]",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )
            objectHolder.collectUnResolvedObjectHolders {
                fail()
            }

            objectHolder.resolve(null)
            assertTrue(objectHolder.resolved())
            assertEquals(list, objectHolder.getObject())
            assertEquals(list, objectHolder.circularEliminate())
        }
        run {
            val map: HashMap<String, Any?> = linkedMapOf("key" to 1)
            val mapObjectHolder = map.asObjectHolder()
            val list: ArrayList<Any?> = arrayListOf("a", "b", "x", mapObjectHolder)
            val objectHolder = list.asObjectHolder() as CollectionObjectHolder
            list[2] = objectHolder
            map["array"] = objectHolder

            assertFalse(objectHolder.resolved())
            assertSame(list, objectHolder.getObject())
            assertEquals(
                "[\"a\",\"b\",[],{\"key\":1,\"array\":[]}]",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )
            val holders = ArrayList<ObjectHolder>()
            objectHolder.collectUnResolvedObjectHolders {
                holders.add(it)
            }
            assertEquals(2, holders.size)
            assertEquals(objectHolder, holders[0])
            assertEquals(mapObjectHolder, holders[1])

            objectHolder.resolve(null)
            assertFalse(objectHolder.resolved())
            assertEquals(
                "[\"a\",\"b\",[],{\"key\":1,\"array\":[]}]",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )

            SimpleContext().with(objectHolder, null) {
                objectHolder.resolve(it)
            }
            assertTrue(objectHolder.resolved())
            assertEquals(
                "[\"a\",\"b\",[\"a\",\"b\",[\"a\",\"b\",[],{\"key\":1,\"array\":[]}],{\"key\":1,\"array\":[\"a\",\"b\",[],{\"key\":1,\"array\":[]}]}],{\"key\":1,\"array\":[\"a\",\"b\",[],{\"key\":1,\"array\":[]}]}]",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )
            assertEquals(
                "[\"a\",\"b\",[\"a\",\"b\",[],{\"key\":1,\"array\":[]}],{\"key\":1,\"array\":[\"a\",\"b\",[],{\"key\":1,\"array\":[]}]}]",
                GsonUtils.toJson(objectHolder.getObject())
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testMapObjectHolder() {
        run {
            val map: HashMap<Any?, Any?> = hashMapOf("a" to "b")
            val objectHolder = map.asObjectHolder() as MapObjectHolder

            assertFalse(objectHolder.resolved())
            assertSame(map, objectHolder.getObject())
            assertEquals(
                "{\"a\":\"b\"}",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )
            objectHolder.collectUnResolvedObjectHolders {
                fail()
            }

            objectHolder.resolve(null)
            assertTrue(objectHolder.resolved())
            assertEquals(map, objectHolder.getObject())
            assertEquals(map, objectHolder.circularEliminate())
        }
        run {
            val map: HashMap<String, Any?> = linkedMapOf("key" to 1)
            val objectHolder = map.asObjectHolder() as MapObjectHolder
            val list: ArrayList<Any?> = arrayListOf("a", "b", objectHolder)
            val listObjectHolder = list.asObjectHolder()
            map["array"] = listObjectHolder
            map["map"] = objectHolder

            assertFalse(objectHolder.resolved())
            assertSame(map, objectHolder.getObject())
            assertEquals(
                "{\"key\":1,\"array\":[\"a\",\"b\",{}],\"map\":{}}",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )
            val holders = ArrayList<ObjectHolder>()
            objectHolder.collectUnResolvedObjectHolders {
                holders.add(it)
            }
            assertEquals(2, holders.size)
            assertEquals(listObjectHolder, holders[0])
            assertEquals(objectHolder, holders[1])

            objectHolder.resolve(null)
            assertFalse(objectHolder.resolved())
            assertEquals(
                "{\"key\":1,\"array\":[\"a\",\"b\",{}],\"map\":{}}",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )

            SimpleContext().with(objectHolder, null) {
                objectHolder.resolve(it)
            }
            assertTrue(objectHolder.resolved())
            assertEquals(
                "{\"key\":1,\"array\":[\"a\",\"b\",{\"key\":1,\"array\":[\"a\",\"b\",{}],\"map\":{}}],\"map\":{\"key\":1,\"array\":[\"a\",\"b\",{\"key\":1,\"array\":[\"a\",\"b\",{}],\"map\":{}}],\"map\":{\"key\":1,\"array\":[\"a\",\"b\",{\"key\":1,\"array\":[\"a\",\"b\",{}],\"map\":{}}],\"map\":{}}}}",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )
            assertEquals(
                "{\"key\":1,\"array\":[\"a\",\"b\",{\"key\":1,\"array\":[\"a\",\"b\",{}],\"map\":{}}],\"map\":{\"key\":1,\"array\":[\"a\",\"b\",{\"key\":1,\"array\":[\"a\",\"b\",{}],\"map\":{}}],\"map\":{}}}",
                GsonUtils.toJson(objectHolder.getObject())
            )
        }
    }

    @Test
    fun testUpgradeObjectHolder() {
        run {
            val objectHolder = UpgradeObjectHolder("str".asObjectHolder())
            val target = hashMapOf("key" to "value")
            SimpleContext().with(target.asObjectHolder()) {
                objectHolder.onResolve(it)
            }
            assertEquals("{\"key\":\"value\"}", GsonUtils.toJson(target))
        }
        run {
            val objectHolder = UpgradeObjectHolder(hashMapOf("a" to "b").asObjectHolder())
            val target = arrayListOf(1, 2)
            SimpleContext().with(target.asObjectHolder()) {
                objectHolder.onResolve(it)
            }
            assertEquals("[1,2]", GsonUtils.toJson(target))
        }
        run {
            val objectHolder = UpgradeObjectHolder(hashMapOf("a" to "b").asObjectHolder())
            val target = linkedMapOf("key" to "value")
            SimpleContext().with(target.asObjectHolder()) {
                objectHolder.onResolve(it)
            }
            assertEquals("{\"key\":\"value\",\"a\":\"b\"}", GsonUtils.toJson(target))
        }
    }

    @Test
    fun testExtendObjectHolder() {
        run {
            val objectHolder = ExtendObjectHolder("str".asObjectHolder())
                .set("@comment", "a string")
                .set("@comment@options", linkedMapOf("name" to "X", "value" to 10))
                .set("a", 1)
            val target = arrayListOf(1, 2)
            SimpleContext().with(target.asObjectHolder()) {
                objectHolder.onResolve(it)
            }
            Assertions.assertEquals("[1,2]", GsonUtils.toJson(target))
        }
        run {
            val objectHolder = ExtendObjectHolder("str".asObjectHolder())
                .set("@comment", "a string")
                .set("@comment@options", linkedMapOf("name" to "X", "value" to 10))
                .set("a", 1)
            val target = linkedMapOf("key" to "value")
            SimpleContext().with(target.asObjectHolder()) {
                objectHolder.onResolve(it)
            }
            Assertions.assertEquals("{\"key\":\"value\"}", GsonUtils.toJson(target))
            SimpleContext().with(target.asObjectHolder(), "key") {
                objectHolder.onResolve(it)
            }
            Assertions.assertEquals(
                "{\"key\":\"value\",\"@comment\":{\"key\":\"a string\",\"key@options\":{\"name\":\"X\",\"value\":10}},\"a\":1}",
                GsonUtils.toJson(target)
            )
        }
    }
}