package com.itangcent.intellij.extend.guice

import com.google.inject.Key
import com.google.inject.Provider
import com.google.inject.spi.TypeEncounter


/**
 * Like a [com.google.inject.Provider] but which is also given an [TypeEncounter]
 *
 * @version $Revision: 1.1 $
 */
abstract class EncounterProvider<T> {
    abstract operator fun get(encounter: TypeEncounter<*>): Provider<out T>

    companion object {

        /**
         * Returns a new encounter provider for the given key
         */
        fun <T> encounterProvider(key: Key<T>): EncounterProvider<T> {
            return object : EncounterProvider<T>() {
                override fun get(encounter: TypeEncounter<*>): Provider<out T> {
                    return encounter.getProvider<T>(key)
                }
            }
        }

        /**
         * Returns a new encounter provider for the given type
         */
        fun <T> encounterProvider(type: Class<T>): EncounterProvider<T> {
            return object : EncounterProvider<T>() {
                override fun get(encounter: TypeEncounter<*>): Provider<out T> {
                    return encounter.getProvider<T>(type)
                }
            }
        }

        /**
         * Returns a new encounter provider for the given instance
         */
        fun <T> encounterProvider(instance: T): EncounterProvider<T> {
            return object : EncounterProvider<T>() {
                override fun get(encounter: TypeEncounter<*>): Provider<out T> {
                    return Provider { instance }
                }
            }
        }
    }

}
