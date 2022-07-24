package com.itangcent.intellij.util

import com.itangcent.common.spi.SpiUtils

interface AppropriateStringMatcher {

    fun getPoint(source: String, candidate: String): Int

    companion object {

        private val matchers: List<AppropriateStringMatcher> by lazy {
            val ms = ArrayList<AppropriateStringMatcher>()
            ms.addAll(Matcher.values())
            SpiUtils.loadServices(AppropriateStringMatcher::class)?.forEach { ms.add(it) }
            ms
        }

        fun findAppropriate(source: String, candidates: Collection<String>): String? {
            if (candidates.isEmpty()) {
                return null
            }
            val distinctCandidates = candidates.distinct()
            if (distinctCandidates.size == 1) {
                return distinctCandidates[0]
            }
            val sortedCandidates = distinctCandidates.map { it to computePoint(source, it) }
                .sortedByDescending { it.second }
            if (sortedCandidates[0].second > sortedCandidates[1].second) {
                return sortedCandidates[0].first
            }
            return null

        }

        private fun computePoint(source: String, candidate: String): Int {
            return matchers.sumOf { it.getPoint(source, candidate) }
        }
    }
}

enum class Matcher : AppropriateStringMatcher {
    SAME {
        override fun match(source: String, candidate: String): Boolean {
            return source == candidate
        }

        override val point: Int
            get() = 100
    },
    SAME_IGNORE_CASE {
        override fun match(source: String, candidate: String): Boolean {
            return source.equals(candidate, ignoreCase = true)
        }

        override val point: Int
            get() = 90
    },
    ENDS_WITH {
        override fun match(source: String, candidate: String): Boolean {
            return source.endsWith(candidate, ignoreCase = true)
        }

        override val point: Int
            get() = 50
    },
    START_WITH {
        override fun match(source: String, candidate: String): Boolean {
            return source.startsWith(candidate, ignoreCase = true)
        }

        override val point: Int
            get() = 50
    },
    CONTAIN {
        override fun match(source: String, candidate: String): Boolean {
            return source.contains(candidate, ignoreCase = true)
        }

        override val point: Int
            get() = 30
    }
    ;

    abstract fun match(source: String, candidate: String): Boolean

    abstract val point: Int

    override fun getPoint(source: String, candidate: String): Int {
        if (match(source, candidate)) {
            return point
        }
        return 0
    }
}