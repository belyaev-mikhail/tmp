package ru.spbstu.collections.persistent.impl

import ru.spbstu.collections.persistent.iteratorEquals
import ru.spbstu.collections.persistent.iteratorHash

interface IterableWithDefaults<out E> : Iterable<E> {
    fun defaultEquals(other: Any?): Boolean =
            when {
                other === this -> true
                other !is Iterable<*> -> false
                else -> iteratorEquals(iterator(), other.iterator())
            }

    fun defaultHashCode(): Int =
            iteratorHash(iterator())

    fun defaultToString() =
            joinToString(prefix = "[", postfix = "]")
}