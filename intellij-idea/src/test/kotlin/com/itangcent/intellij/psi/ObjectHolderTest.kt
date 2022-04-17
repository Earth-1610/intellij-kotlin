package com.itangcent.intellij.psi

import com.itangcent.common.utils.GsonUtils
import org.junit.Assert
import org.junit.jupiter.api.Test

/**
 * Test case of [PsiClassUtils]
 */
internal class ObjectHolderTest {

    @Test
    fun asObjectHolder() {
        Assert.assertSame(NULL_OBJECT_HOLDER, null.asObjectHolder())
        Assert.assertSame(NULL_OBJECT_HOLDER, NULL_OBJECT_HOLDER.asObjectHolder())
        Assert.assertTrue(emptyArray<Any?>().asObjectHolder() is ArrayObjectHolder)
        Assert.assertTrue(emptyList<Any?>().asObjectHolder() is CollectionObjectHolder)
        Assert.assertTrue(emptyMap<Any?, Any?>().asObjectHolder() is MapObjectHolder)
        Assert.assertTrue(1.asObjectHolder() is ResolvedObjectHolder)
    }

    @Test
    fun notResolved() {
        Assert.assertFalse(NULL_OBJECT_HOLDER.notResolved())
        Assert.assertTrue(emptyArray<Any?>().asObjectHolder().notResolved())
        Assert.assertTrue(emptyList<Any?>().asObjectHolder().notResolved())
        Assert.assertTrue(emptyMap<Any?, Any?>().asObjectHolder().notResolved())
        Assert.assertFalse(1.asObjectHolder().notResolved())
    }

    @Test
    fun getOrResolve() {
        Assert.assertNull((null as ObjectHolder?).getOrResolve())
        Assert.assertEquals("str", "str".asObjectHolder().getOrResolve())
        Assert.assertEquals(1, 1.asObjectHolder().getOrResolve())
        val objectHolder =
            arrayListOf(hashMapOf("key".asObjectHolder() to null.asObjectHolder()).asObjectHolder()).asObjectHolder()
        Assert.assertEquals("[{\"key\":null}]", GsonUtils.toJsonWithNulls(objectHolder.getOrResolve()))
        Assert.assertNotSame(objectHolder.getOrResolve(), objectHolder.getOrResolve())
    }

    @Test
    fun upgrade() {
        val objectHolder = NULL_OBJECT_HOLDER.upgrade()
        Assert.assertSame(objectHolder, objectHolder.upgrade())
    }

    @Test
    fun extend() {
        val objectHolder = NULL_OBJECT_HOLDER.extend()
        Assert.assertSame(objectHolder, objectHolder.extend())
    }

    @Test
    fun parent() {
        val context = SimpleContext()
        Assert.assertNull(context.parent())

        context.pushHolder(NULL_OBJECT_HOLDER, null)
        Assert.assertSame(NULL_OBJECT_HOLDER, context.parent())

        val objectHolder = "str".asObjectHolder()
        context.pushHolder(objectHolder, null)
        Assert.assertSame(objectHolder, context.parent())
    }

    @Test
    fun nearestMap() {
        val context = SimpleContext()
        Assert.assertNull(context.nearestMap())

        context.pushHolder(NULL_OBJECT_HOLDER, null)
        Assert.assertNull(context.nearestMap())

        val objectHolder = hashMapOf("key" to 1).asObjectHolder()
        context.pushHolder(objectHolder, null)
        Assert.assertSame(objectHolder, context.nearestMap())

        val strObjectHolder = "str".asObjectHolder()
        context.pushHolder(strObjectHolder, null)
        Assert.assertSame(objectHolder, context.nearestMap())
    }

    @Test
    fun nearestProperty() {
        val context = SimpleContext()
        Assert.assertNull(context.nearestProperty())

        context.pushHolder(NULL_OBJECT_HOLDER, null)
        Assert.assertNull(context.nearestProperty())

        context.pushHolder(NULL_OBJECT_HOLDER, "key")
        Assert.assertSame("key", context.nearestProperty())

        context.pushHolder(NULL_OBJECT_HOLDER, null)
        Assert.assertSame("key", context.nearestProperty())

        context.pushHolder(NULL_OBJECT_HOLDER, "beta")
        Assert.assertSame("beta", context.nearestProperty())
    }

    @Test
    fun with() {
        val context = SimpleContext()
        context.pushHolder(NULL_OBJECT_HOLDER, null)

        Assert.assertEquals(1, context.holders().size)
        context.with(NULL_OBJECT_HOLDER) {
            Assert.assertEquals(2, it.holders().size)
            Assert.assertNull(context.nearestProperty())
        }
        Assert.assertEquals(1, context.holders().size)

        context.with(NULL_OBJECT_HOLDER, "key") {
            Assert.assertEquals(2, it.holders().size)
            Assert.assertEquals("key", context.nearestProperty())
        }
        Assert.assertEquals(1, context.holders().size)
    }

    @Test
    fun testResolvedObjectHolder() {
        val objectHolder = 1.asObjectHolder() as ResolvedObjectHolder
        Assert.assertTrue(objectHolder.resolved())
        Assert.assertEquals(1, objectHolder.circularEliminate())
        Assert.assertEquals(1, objectHolder.getObject())

        objectHolder.resolve(null)
        objectHolder.onResolve(SimpleContext())

        Assert.assertEquals(1, objectHolder.circularEliminate())
        Assert.assertEquals(1, objectHolder.getObject())
        objectHolder.collectUnResolvedObjectHolders {
            Assert.fail()
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testArrayObjectHolder() {
        run {
            val array: Array<Any?> = arrayOf("a", "b")
            val objectHolder = array.asObjectHolder() as ArrayObjectHolder

            Assert.assertFalse(objectHolder.resolved())
            Assert.assertSame(array, objectHolder.getObject())
            Assert.assertEquals(
                "[\"a\",\"b\"]",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )
            objectHolder.collectUnResolvedObjectHolders {
                Assert.fail()
            }

            objectHolder.resolve(null)
            Assert.assertTrue(objectHolder.resolved())
            Assert.assertArrayEquals(array, objectHolder.getObject() as Array<out Any>)
            Assert.assertArrayEquals(array, objectHolder.circularEliminate() as Array<out Any>)
        }
        run {
            val map: HashMap<String, Any?> = linkedMapOf("key" to 1)
            val mapObjectHolder = map.asObjectHolder()
            val array: Array<Any?> = arrayOf("a", "b", "x", mapObjectHolder)
            val objectHolder = array.asObjectHolder() as ArrayObjectHolder
            array[2] = objectHolder
            map["array"] = objectHolder

            Assert.assertFalse(objectHolder.resolved())
            Assert.assertSame(array, objectHolder.getObject())
            Assert.assertEquals(
                "[\"a\",\"b\",[],{\"key\":1,\"array\":[]}]",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )
            val holders = ArrayList<ObjectHolder>()
            objectHolder.collectUnResolvedObjectHolders {
                holders.add(it)
            }
            Assert.assertEquals(2, holders.size)
            Assert.assertEquals(objectHolder, holders[0])
            Assert.assertEquals(mapObjectHolder, holders[1])

            objectHolder.resolve(null)
            Assert.assertFalse(objectHolder.resolved())
            Assert.assertEquals(
                "[\"a\",\"b\",[],{\"key\":1,\"array\":[]}]",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )

            SimpleContext().with(objectHolder, null) {
                objectHolder.resolve(it)
            }
            Assert.assertTrue(objectHolder.resolved())
            Assert.assertEquals(
                "[\"a\",\"b\",[\"a\",\"b\",[\"a\",\"b\",[],{\"key\":1,\"array\":[]}],{\"key\":1,\"array\":[\"a\",\"b\",[],{\"key\":1,\"array\":[]}]}],{\"key\":1,\"array\":[\"a\",\"b\",[],{\"key\":1,\"array\":[]}]}]",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )
            Assert.assertEquals(
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

            Assert.assertFalse(objectHolder.resolved())
            Assert.assertSame(list, objectHolder.getObject())
            Assert.assertEquals(
                "[\"a\",\"b\"]",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )
            objectHolder.collectUnResolvedObjectHolders {
                Assert.fail()
            }

            objectHolder.resolve(null)
            Assert.assertTrue(objectHolder.resolved())
            Assert.assertEquals(list, objectHolder.getObject())
            Assert.assertEquals(list, objectHolder.circularEliminate())
        }
        run {
            val map: HashMap<String, Any?> = linkedMapOf("key" to 1)
            val mapObjectHolder = map.asObjectHolder()
            val list: ArrayList<Any?> = arrayListOf("a", "b", "x", mapObjectHolder)
            val objectHolder = list.asObjectHolder() as CollectionObjectHolder
            list[2] = objectHolder
            map["array"] = objectHolder

            Assert.assertFalse(objectHolder.resolved())
            Assert.assertSame(list, objectHolder.getObject())
            Assert.assertEquals(
                "[\"a\",\"b\",[],{\"key\":1,\"array\":[]}]",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )
            val holders = ArrayList<ObjectHolder>()
            objectHolder.collectUnResolvedObjectHolders {
                holders.add(it)
            }
            Assert.assertEquals(2, holders.size)
            Assert.assertEquals(objectHolder, holders[0])
            Assert.assertEquals(mapObjectHolder, holders[1])

            objectHolder.resolve(null)
            Assert.assertFalse(objectHolder.resolved())
            Assert.assertEquals(
                "[\"a\",\"b\",[],{\"key\":1,\"array\":[]}]",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )

            SimpleContext().with(objectHolder, null) {
                objectHolder.resolve(it)
            }
            Assert.assertTrue(objectHolder.resolved())
            Assert.assertEquals(
                "[\"a\",\"b\",[\"a\",\"b\",[\"a\",\"b\",[],{\"key\":1,\"array\":[]}],{\"key\":1,\"array\":[\"a\",\"b\",[],{\"key\":1,\"array\":[]}]}],{\"key\":1,\"array\":[\"a\",\"b\",[],{\"key\":1,\"array\":[]}]}]",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )
            Assert.assertEquals(
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

            Assert.assertFalse(objectHolder.resolved())
            Assert.assertSame(map, objectHolder.getObject())
            Assert.assertEquals(
                "{\"a\":\"b\"}",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )
            objectHolder.collectUnResolvedObjectHolders {
                Assert.fail()
            }

            objectHolder.resolve(null)
            Assert.assertTrue(objectHolder.resolved())
            Assert.assertEquals(map, objectHolder.getObject())
            Assert.assertEquals(map, objectHolder.circularEliminate())
        }
        run {
            val map: HashMap<String, Any?> = linkedMapOf("key" to 1)
            val objectHolder = map.asObjectHolder() as MapObjectHolder
            val list: ArrayList<Any?> = arrayListOf("a", "b", objectHolder)
            val listObjectHolder = list.asObjectHolder()
            map["array"] = listObjectHolder
            map["map"] = objectHolder

            Assert.assertFalse(objectHolder.resolved())
            Assert.assertSame(map, objectHolder.getObject())
            Assert.assertEquals(
                "{\"key\":1,\"array\":[\"a\",\"b\",{}],\"map\":{}}",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )
            val holders = ArrayList<ObjectHolder>()
            objectHolder.collectUnResolvedObjectHolders {
                holders.add(it)
            }
            Assert.assertEquals(2, holders.size)
            Assert.assertEquals(listObjectHolder, holders[0])
            Assert.assertEquals(objectHolder, holders[1])

            objectHolder.resolve(null)
            Assert.assertFalse(objectHolder.resolved())
            Assert.assertEquals(
                "{\"key\":1,\"array\":[\"a\",\"b\",{}],\"map\":{}}",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )

            SimpleContext().with(objectHolder, null) {
                objectHolder.resolve(it)
            }
            Assert.assertTrue(objectHolder.resolved())
            Assert.assertEquals(
                "{\"key\":1,\"array\":[\"a\",\"b\",{\"key\":1,\"array\":[\"a\",\"b\",{}],\"map\":{}}],\"map\":{\"key\":1,\"array\":[\"a\",\"b\",{\"key\":1,\"array\":[\"a\",\"b\",{}],\"map\":{}}],\"map\":{\"key\":1,\"array\":[\"a\",\"b\",{\"key\":1,\"array\":[\"a\",\"b\",{}],\"map\":{}}],\"map\":{}}}}",
                GsonUtils.toJson(objectHolder.circularEliminate())
            )
            Assert.assertEquals(
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
            Assert.assertEquals("{\"key\":\"value\"}", GsonUtils.toJson(target))
        }
        run {
            val objectHolder = UpgradeObjectHolder(hashMapOf("a" to "b").asObjectHolder())
            val target = arrayListOf(1, 2)
            SimpleContext().with(target.asObjectHolder()) {
                objectHolder.onResolve(it)
            }
            Assert.assertEquals("[1,2]", GsonUtils.toJson(target))
        }
        run {
            val objectHolder = UpgradeObjectHolder(hashMapOf("a" to "b").asObjectHolder())
            val target = hashMapOf("key" to "value")
            SimpleContext().with(target.asObjectHolder()) {
                objectHolder.onResolve(it)
            }
            Assert.assertEquals("{\"a\":\"b\",\"key\":\"value\"}", GsonUtils.toJson(target))
        }
    }

    @Test
    fun testExtendObjectHolder() {
        run {
            val objectHolder = ExtendObjectHolder("str".asObjectHolder())
                .set("@comment", "a string")
                .set("@comment@options", mapOf("name" to "X", "value" to 10))
                .set("a", 1)
            val target = arrayListOf(1, 2)
            SimpleContext().with(target.asObjectHolder()) {
                objectHolder.onResolve(it)
            }
            Assert.assertEquals("[1,2]", GsonUtils.toJson(target))
        }
        run {
            val objectHolder = ExtendObjectHolder("str".asObjectHolder())
                .set("@comment", "a string")
                .set("@comment@options", mapOf("name" to "X", "value" to 10))
                .set("a", 1)
            val target = linkedMapOf("key" to "value")
            SimpleContext().with(target.asObjectHolder()) {
                objectHolder.onResolve(it)
            }
            Assert.assertEquals("{\"key\":\"value\"}", GsonUtils.toJson(target))
            SimpleContext().with(target.asObjectHolder(), "key") {
                objectHolder.onResolve(it)
            }
            Assert.assertEquals(
                "{\"key\":\"value\",\"@comment\":{\"key@options\":{\"name\":\"X\",\"value\":10},\"key\":\"a string\"},\"a\":1}",
                GsonUtils.toJson(target)
            )
        }
    }
}