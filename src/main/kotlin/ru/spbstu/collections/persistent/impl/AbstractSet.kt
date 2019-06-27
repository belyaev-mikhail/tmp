package ru.spbstu.collections.persistent.impl

import java.util.Objects.*

abstract class AbstractSet<E>: Set<E>, AbstractCollection<E>() {
    override fun equals(other: Any?) =
        when {
            other === this -> true
            other !is Set<*> -> false
            else -> (size == other.size) && containsAll(other)
        }

    abstract override fun contains(element: E): Boolean

    override fun hashCode() = sumBy { hashCode(it) } // set hashcode
}