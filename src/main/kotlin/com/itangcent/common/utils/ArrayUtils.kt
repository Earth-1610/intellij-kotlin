package com.itangcent.common.utils

object ArrayUtils {

    /**
     * The index value when an element is not found in a list or array: `-1`.
     * This value is returned by methods in this class and can also be used in comparisons with values returned by
     * various method from [java.util.List].
     */
    val INDEX_NOT_FOUND = -1

    /**
     *
     * Checks if the object is in the given array.
     *
     *
     * The method returns `false` if a `null` array is passed in.
     *
     * @param array  the array to search through
     * @param objectToFind  the object to find
     * @return `true` if the array contains the object
     */
    fun contains(array: Array<*>, objectToFind: Any?): Boolean {
        return indexOf(array, objectToFind) != INDEX_NOT_FOUND
    }

    //-----------------------------------------------------------------------
    /**
     *
     * Finds the index of the given object in the array.
     *
     *
     * This method returns [.INDEX_NOT_FOUND] (`-1`) for a `null` input array.
     *
     * @param array  the array to search through for the object, may be `null`
     * @param objectToFind  the object to find, may be `null`
     * @return the index of the object within the array,
     * [.INDEX_NOT_FOUND] (`-1`) if not found or `null` array input
     */
    fun indexOf(array: Array<*>, objectToFind: Any?): Int {
        return indexOf(array, objectToFind, 0)
    }

    /**
     *
     * Finds the index of the given object in the array starting at the given index.
     *
     *
     * This method returns [.INDEX_NOT_FOUND] (`-1`) for a `null` input array.
     *
     *
     * A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return [.INDEX_NOT_FOUND] (`-1`).
     *
     * @param array  the array to search through for the object, may be `null`
     * @param objectToFind  the object to find, may be `null`
     * @param startIndex  the index to start searching at
     * @return the index of the object within the array starting at the index,
     * [.INDEX_NOT_FOUND] (`-1`) if not found or `null` array input
     */
    fun indexOf(array: Array<*>?, objectToFind: Any?, startIndex: Int): Int {
        var startIndex = startIndex
        if (array == null) {
            return INDEX_NOT_FOUND
        }
        if (startIndex < 0) {
            startIndex = 0
        }
        if (objectToFind == null) {

            for (i in startIndex until array.size) {
                if (array[i] == null) {
                    return i
                }
            }
        } else if (array.javaClass.getComponentType().isInstance(objectToFind)) {
            for (i in startIndex until array.size) {
                if (objectToFind == array[i]) {
                    return i
                }
            }
        }
        return INDEX_NOT_FOUND
    }

}