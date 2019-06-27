package ru.spbstu.collections.persistent.impl

import ru.spbstu.collections.persistent.andReturn
import ru.spbstu.collections.persistent.butAlso
import ru.spbstu.collections.persistent.iteratorEquals
import java.util.*

abstract class AbstractList<E> : List<E>, AbstractCollection<E>() {
    override fun iterator(): Iterator<E> = listIterator()

    override fun indexOf(element: E): Int {
        var ix = 0
        for (e in this) {
            if (e == element) return ix
            ++ix
        }
        return -1
    }

    override fun lastIndexOf(element: E): Int {
        var ix = 0
        var ret = -1
        for (e in this) {
            if (e == element) {
                ret = ix
            }
            ++ix
        }
        return ret
    }

    override fun listIterator() = listIterator(0)

    data class DefaultListIterator<E>(val data: List<E>, var index: Int = 0) : ListIterator<E> {
        override fun hasNext() = index < data.size
        override fun hasPrevious() = index > 0
        override fun next() = data[index] butAlso { ++index }
        override fun nextIndex() = index
        override fun previous() = { --index } andReturn data[index]
        override fun previousIndex() = index - 1
    }

    override fun listIterator(index: Int): ListIterator<E> = DefaultListIterator(this, index)

    class DefaultSubList<E>(val inner: List<E>, val fromIndex: Int, val toIndex: Int) : AbstractList<E>() {
        override val size: Int get() = toIndex - fromIndex
        override fun get(index: Int): E = inner.get(fromIndex + index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<E> = DefaultSubList<E>(this, fromIndex, toIndex)

    override fun equals(other: Any?) =
            when {
                other === this -> true
                other !is List<*> -> false
                else -> iteratorEquals(iterator(), other.iterator())
            }
}