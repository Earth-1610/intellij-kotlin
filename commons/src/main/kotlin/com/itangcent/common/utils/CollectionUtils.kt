package com.itangcent.common.utils

/**
 *
 * `CollectionUtils` should not normally be instantiated.
 * Provides utility methods and decorators for [Collection] instances.
 *
 *
 * Various utility methods might put the input objects into a Set/Map/Bag. In case
 * the input objects override [Object.equals], it is mandatory that
 * the general contract of the [Object.hashCode] method is maintained.
 *
 *
 * NOTE: From 4.0, method parameters will take [Iterable] objects when possible.
 *
 * @since 1.0
 */
object CollectionUtils {

    /**
     * Returns `true` iff at least one element is in both collections.
     *
     *
     * In other words, this method returns `true` iff the
     * [.intersection] of *coll1* and *coll2* is not empty.
     *
     * @param <T> the type of object to lookup in `coll1`.
     * @param coll1  the first collection, must not be null
     * @param coll2  the second collection, must not be null
     * @return `true` iff the intersection of the collections is non-empty
     * @since 4.2
     * @see .intersection
    </T> */
    fun <T> containsAny(coll1: Collection<*>, vararg coll2: T): Boolean {
        if (coll1.size < coll2.size) {
            for (aColl1 in coll1) {
                if (ArrayUtils.contains(coll2, aColl1)) {
                    return true
                }
            }
        } else {
            for (aColl2 in coll2) {
                if (coll1.contains(aColl2)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Returns `true` iff at least one element is in both collections.
     *
     *
     * In other words, this method returns `true` iff the
     * [.intersection] of *coll1* and *coll2* is not empty.
     *
     * @param coll1  the first collection, must not be null
     * @param coll2  the second collection, must not be null
     * @return `true` iff the intersection of the collections is non-empty
     * @since 2.1
     * @see .intersection
     */
    fun containsAny(coll1: Collection<*>, coll2: Collection<*>): Boolean {
        if (coll1.size < coll2.size) {
            for (aColl1 in coll1) {
                if (coll2.contains(aColl1)) {
                    return true
                }
            }
        } else {
            for (aColl2 in coll2) {
                if (coll1.contains(aColl2)) {
                    return true
                }
            }
        }
        return false
    }

    fun containsAny(coll1: Array<*>, coll2: Collection<*>): Boolean {
        if (coll1.size < coll2.size) {
            for (aColl1 in coll1) {
                if (coll2.contains(aColl1)) {
                    return true
                }
            }
        } else {
            for (aColl2 in coll2) {
                if (coll1.contains(aColl2)) {
                    return true
                }
            }
        }
        return false
    }

    fun containsAll(coll1: Array<*>, coll2: Collection<*>): Boolean {
        for (acol in coll2) {
            if (!coll1.contains(acol)) {
                return false
            }
        }
        return true
    }

    /**
     * Null-safe check if the specified collection is empty.
     *
     *
     * Null returns true.
     *
     * @param coll  the collection to check, may be null
     * @return true if empty or null
     * @since 3.2
     */
    @Deprecated(message = "deprecated", replaceWith = ReplaceWith("Collection.isNullOrEmpty"))
    fun isEmpty(coll: Collection<*>?): Boolean = coll.isNullOrEmpty()

}