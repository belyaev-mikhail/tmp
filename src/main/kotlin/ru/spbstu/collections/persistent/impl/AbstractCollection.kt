package ru.spbstu.collections.persistent.impl

abstract class AbstractCollection<E>: Collection<E>, IterableWithDefaults<E> {
    override fun containsAll(elements: Collection<E>) = elements.all { contains(it) }
    override fun isEmpty() = size == 0

    override fun contains(element: E) = any { it == element }

    override fun toString() = defaultToString()
    override fun equals(other: Any?) = defaultEquals(other)
    override fun hashCode() = defaultHashCode()
}