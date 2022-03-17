package com.itangcent.common.utils


/**
 * A collection that maps keys to values, similar to [Map], but in which each key may be
 * associated with <i>multiple</i> values. You can visualize the contents of a multimap either as a
 * map from keys to <i>nonempty</i> collections of values:
 *
 * <ul>
 *   <li>a → 1, 2
 *   <li>b → 3
 * </ul>
 *
 * ... or as a single "flattened" collection of key-value pairs:
 *
 * <ul>
 *   <li>a → 1
 *   <li>a → 2
 *   <li>b → 3
 * </ul>
 * @author tangcent
 */
class MultiValuesMap<K, V> : Map<K, Collection<V>?> {

    /**
     * The head (eldest) of the doubly linked list.
     */
    private var head: Node<K, V>? = null

    /**
     * The tail (youngest) of the doubly linked list.
     */
    private var tail: Node<K, V>? = null

    @Transient
    private val keyToNode: HashMap<K, Node<K, V>> = LinkedHashMap()

    @Transient
    private var count = 0

    internal open class Node<K, V>(override val key: K, override val value: V) : Map.Entry<K, V> {
        var previous: Node<K, V>? = null// the previous node (with any key)

        var next: Node<K, V>? = null// the next node (with any key)

        var nextSibling: Node<K, V>? = null // the next node with the same key

        override fun toString(): String {
            return "Node(key=$key, value=$value)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Node<*, *>

            if (key != other.key) return false
            if (value != other.value) return false

            return true
        }

        override fun hashCode(): Int {
            var result = key?.hashCode() ?: 0
            result = 31 * result + (value?.hashCode() ?: 0)
            return result
        }
    }

    override val entries: Set<Map.Entry<K, Collection<V>?>>
        get() = EntryCollectionImpl(keyToNode.entries, keyToNode.size)

    override val keys: Set<K>
        get() = keyToNode.keys

    override val size: Int
        get() = keyToNode.size

    val valueSize: Int
        get() = count

    override val values: Collection<Collection<V>?>
        get() {
            val node: MutableCollection<Node<K, V>> = keyToNode.values
            return ValueCollectionImpl<V>(node as MutableCollection<Node<*, V>>, count)
        }

    override fun containsValue(value: Collection<V>?): Boolean {
        return values.contains(value)
    }

    override fun isEmpty(): Boolean {
        return count == 0
    }

    fun putAll(key: K, values: Collection<V>) {
        for (value in values) {
            put(key, value)
        }
    }

    fun putAll(key: K, vararg values: V) {
        for (value in values) {
            put(key, value)
        }
    }

    fun put(key: K, value: V) {
        var node = keyToNode[key]
        if (node == null) {
            node = Node(key, value)
            keyToNode[key] = node
            addNode(node)
        } else {
            do {
                if (node!!.value == value) {
                    return
                }
                if (node.nextSibling == null) {
                    break
                }
                node = node.nextSibling
            } while (true)
            val newNode = Node(key, value)
            addNode(newNode)
            node!!.nextSibling = newNode
        }
    }

    private fun addNode(node: Node<K, V>) {
        if (tail == null) {
            head = node
            tail = node
        } else {
            tail!!.next = node
            node.previous = tail
            tail = node
        }
        ++count
    }

    fun replace(key: K, value: V) {
        var node = keyToNode[key]
        if (node != null) {
            removeAll(node)
        }
        node = Node(key, value)
        keyToNode[key] = node
        addNode(node)
        return

    }

    override operator fun get(key: K): Collection<V>? {
        return keyToNode[key]?.let { ValueSiblingCollectionImpl(it) }
    }

    fun flattenValues(): Collection<V> {
        return ValueFlattenCollectionImpl(head, count)
    }

    fun flattenForEach(action: (K, V) -> Unit) {
        var h: Node<K, V>? = head ?: return
        while (h != null) {
            action(h.key, h.value)
            h = h.next
        }
    }

    fun flattenForEach(keyFilter: (K) -> Boolean, action: (K, V) -> Unit) {
        flattenForEach { key, value ->
            if (keyFilter(key)) {
                action(key, value)
            }
        }
    }

    fun remove(key: K, value: V) {
        var node = keyToNode[key] ?: return
        if (node.value == value) {
            val next = node.nextSibling
            if (next == null) {
                keyToNode.remove(key)
            } else {
                keyToNode[key] = next
            }
            removeNode(node)
            return
        }
        do {
            val next = node.next ?: return
            if (next.value == value) {
                node.nextSibling = next.nextSibling
                removeNode(next)
                return
            }
            node = next
        } while (true)
    }

    fun clear() {
        keyToNode.clear()
        head = null
        tail = null
        count = 0
    }

    fun removeAll(key: K) {
        val node = keyToNode[key]
        if (node != null) {
            removeAll(node)
            keyToNode.remove(key)
        }
    }

    private fun removeAll(node: Node<K, V>) {
        var n: Node<K, V>? = node
        while (n != null) {
            removeNode(n)
            n = n.nextSibling
        }
    }

    private fun removeNode(node: Node<K, V>) {
        --count
        val previous = node.previous
        val next = node.next
        if (node === head) {
            head = next
        }
        if (node === tail) {
            tail = previous
        }
        previous?.next = next
        next?.previous = previous
    }

    override fun containsKey(key: K): Boolean {
        return keyToNode.containsKey(key)
    }

    fun getFirst(key: K): V? {
        return keyToNode[key]?.value
    }

    /**
     * it will throw an IllegalArgumentException if the value of the key is not unique
     */
    fun getOne(key: K): V? {
        val node = keyToNode[key] ?: return null
        return when (node.nextSibling) {
            null -> node.value
            else -> throw IllegalArgumentException("$key has more than one value")
        }
    }

    private class EntryCollectionImpl<K, V>(
        private var node: MutableSet<MutableMap.MutableEntry<K, Node<K, V>>>,
        private var count: Int
    ) : AbstractSet<Map.Entry<K, Collection<V>>>() {
        override val size: Int
            get() = count

        override fun iterator(): Iterator<Map.Entry<K, Collection<V>>> {
            return EntryIteratorImpl(node.iterator())
        }
    }

    private class EntryIteratorImpl<K, V>(private var node: MutableIterator<MutableMap.MutableEntry<K, Node<K, V>>>) :
        Iterator<Map.Entry<K, Collection<V>>> {
        override fun hasNext(): Boolean {
            return node.hasNext()
        }

        override fun next(): Map.Entry<K, Collection<V>> {
            return node.next().let { EntryImpl(it.key, ValueSiblingCollectionImpl(it.value)) }
        }
    }

    private class EntryImpl<K, V>(
        override val key: K,
        override val value: Collection<V>
    ) : Map.Entry<K, Collection<V>> {
        override fun toString(): String {
            return "EntryImpl(key=$key, value=$value)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EntryImpl<*, *>

            if (key != other.key) return false
            if (value != other.value) return false

            return true
        }

        override fun hashCode(): Int {
            var result = key?.hashCode() ?: 0
            result = 31 * result + value.hashCode()
            return result
        }

    }

    private class ValueCollectionImpl<E>(
        private var node: MutableCollection<Node<*, E>>,
        private var count: Int
    ) : AbstractCollection<Collection<E>>() {
        override val size: Int
            get() = count

        override fun iterator(): Iterator<Collection<E>> {
            return ValueIteratorImpl(node.iterator())
        }

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is Collection<*>) return false
            if (count != other.size) return false

            val e1 = iterator()
            val e2 = other.iterator()
            while (e1.hasNext() && e2.hasNext()) {
                val o1 = e1.next()
                val o2 = e2.next()
                if (o1 != o2) return false
            }
            return !(e1.hasNext() || e2.hasNext())
        }

        override fun hashCode(): Int {
            var result = node.hashCode()
            result = 31 * result + count
            return result
        }
    }

    private class ValueIteratorImpl<E>(private var node: MutableIterator<Node<*, E>>) : Iterator<Collection<E>> {
        override fun hasNext(): Boolean {
            return node.hasNext()
        }

        override fun next(): Collection<E> {
            return ValueSiblingCollectionImpl(node.next())
        }
    }

    private class ValueFlattenCollectionImpl<E>(
        private var node: Node<*, E>?,
        private var count: Int
    ) : AbstractList<E>() {
        override val size: Int
            get() = count

        override fun iterator(): Iterator<E> {
            return ValueFlattenIteratorImpl(node)
        }

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is Collection<*>) return false
            if (count != other.size) return false

            val e1 = iterator()
            val e2 = other.iterator()
            while (e1.hasNext() && e2.hasNext()) {
                val o1 = e1.next()
                val o2 = e2.next()
                if (o1 != o2) return false
            }
            return !(e1.hasNext() || e2.hasNext())
        }

        override fun hashCode(): Int {
            var result = node.hashCode()
            result = 31 * result + count
            return result
        }

        override fun get(index: Int): E {
            var x = node
            for (a in 1..index) {
                x = x!!.next
            }
            return x!!.value
        }
    }

    private class ValueFlattenIteratorImpl<E>(private var node: Node<*, E>?) : Iterator<E> {
        override fun hasNext(): Boolean {
            return node != null
        }

        override fun next(): E {
            val v = node!!.value
            node = node!!.next
            return v
        }
    }

    private class ValueSiblingCollectionImpl<E>(
        private var node: Node<*, E>?
    ) : AbstractCollection<E>() {
        override val size: Int
            get() = count()

        private fun count(): Int {
            var i = 0
            iterator().forEach { ++i }
            return i
        }

        override fun iterator(): Iterator<E> {
            return ValueSiblingIteratorImpl(node)
        }

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is Collection<*>) return false

            val e1 = iterator()
            val e2 = other.iterator()
            while (e1.hasNext() && e2.hasNext()) {
                val o1 = e1.next()
                val o2 = e2.next()
                if (o1 != o2) return false
            }
            return !(e1.hasNext() || e2.hasNext())
        }

        override fun hashCode(): Int {
            return node.hashCode()
        }
    }

    private class ValueSiblingIteratorImpl<E>(private var node: Node<*, E>?) : Iterator<E> {
        override fun hasNext(): Boolean {
            return node != null
        }

        override fun next(): E {
            val v = node!!.value
            node = node!!.nextSibling
            return v
        }
    }

    override fun toString(): String {
        if (isEmpty()) return "{}"
        val sb = StringBuilder()
        sb.append('{')
        var h: Node<K, V>? = head
        while (h != null) {
            sb.append(if (h.key === this) "(this Map)" else h.key)
            sb.append('=')
            sb.append(if (h.value === this) "(this Map)" else h.value)
            h = h.next
            if (h != null) {
                sb.append(',').append(' ')
            } else {
                break
            }
        }
        return sb.append('}').toString()
    }

    override fun equals(other: Any?): Boolean {

        if (other === this) return true
        if (other !is MultiValuesMap<*, *>) return false
        if (other.size != size) return false

        var h1 = this.head
        var h2 = other.head
        while (h1 != null && h2 != null) {
            if (h1 != h2) {
                return false
            }
            h1 = h1.next
            h2 = h2.next
        }
        return h1 == h2
    }

    override fun hashCode(): Int {
        var h = 0
        val i = entries.iterator()
        while (i.hasNext()) h += i.next().hashCode()
        return h
    }

}

fun <K, V> multiValuesMapOf(vararg pairs: Pair<K, V>): MultiValuesMap<K, V> =
    MultiValuesMap<K, V>().also { map ->
        pairs.forEach { (first, second) -> map.put(first, second) }
    }