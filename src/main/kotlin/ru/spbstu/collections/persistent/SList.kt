package ru.spbstu.collections.persistent

import kotlinx.warnings.Warnings
import ru.spbstu.collections.persistent.impl.IterableWithDefaults
import java.util.*

data class SList<out E>(val head: E, val tail: SList<E>? = null) : IterableWithDefaults<E> {

    companion object {}

    override fun iterator() = SListIterator(this)

    override fun toString() = defaultToString()
    override fun equals(other: Any?) = defaultEquals(other)
    override fun hashCode() = defaultHashCode()
}

fun <E> sListOf(): SList<E>? = null
fun <E> sListOf(vararg els: E): SList<E>? = SList.ofCollection(els.asList())
fun <E> SList.Companion.ofCollection(c: Collection<E>): SList<E>? {
    var acc: SList<E>? = null
    for (e in c) acc = SList(e, acc)
    return acc.reverse()
}

inline fun <E, R> SList<E>?.foldLeft(acc: R, trans: (R, E) -> R): R {
    var mutAcc = acc
    var mutList = this
    while (mutList != null) {
        mutAcc = trans(mutAcc, mutList.head)
        mutList = mutList.tail
    }
    return mutAcc
}

operator fun<E> SList<E>?.iterator() = SListIterator(this)
fun<E> SList<E>?.asIterable(): Iterable<E> = if(this == null) emptyList() else this

fun <E> SList<E>?.reverse() = foldLeft(sListOf<E>()) { a, b -> SList(b, a) }

inline fun <E> SList<E>?.filter(predicate: (E) -> Boolean) =
        foldLeft(sListOf<E>()) { a, b -> if (predicate(b)) SList(b, a) else a }.reverse()

inline fun <E, R> SList<E>?.map(f: (E) -> R): SList<R>? =
        foldLeft(sListOf<R>()) { a, b -> SList(f(b), a) }.reverse()

val <E> SList<E>?.size: Int
    get() = foldLeft(0) { a, `#` -> a + 1 }

fun <E> SList<E>?.splitRevAt(index: Int): Pair<SList<E>?, SList<E>?> {
    var mutIndex = index
    var mutList = this
    var mutBackList: SList<E>? = null
    while (mutList != null && mutIndex != 0) {
        mutBackList = SList(mutList.head, mutBackList)
        mutList = mutList.tail
        --mutIndex
    }
    return Pair(mutBackList, mutList)
}

inline fun <E> SList<E>?.splitRevAt(predicate: (E) -> Boolean): Pair<SList<E>?, SList<E>?> {
    var mutList = this
    var mutBackList: SList<E>? = null
    while (mutList != null && !predicate(mutList.head)) {
        mutBackList = SList(mutList.head, mutBackList)
        mutList = mutList.tail
    }
    return Pair(mutBackList, mutList)
}

@Suppress(Warnings.NOTHING_TO_INLINE)
inline fun <E> mergeRev(reversed: SList<E>?, rest: SList<E>?) =
        reversed.foldLeft(rest) { a, b -> SList(b, a) }

inline fun <E> SList<E>?.mutateAt(index: Int, f: (SList<E>?) -> SList<E>?): SList<E>? {
    val (l, r) = splitRevAt(index)
    return mergeRev(l, f(r))
}

inline fun <E> SList<E>?.mutateAt(predicate: (E) -> Boolean, f: (SList<E>?) -> SList<E>?): SList<E>? {
    val (l, r) = splitRevAt(predicate)
    return mergeRev(l, f(r))
}

inline fun <E> SList<E>?.find(predicate: (E) -> Boolean): E? = splitRevAt(predicate).second?.head

operator fun <E> SList<E>?.contains(element: Any?) = asIterable().any { it == element }

inline fun <E> SList<E>?.removeAt(predicate: (E) -> Boolean): SList<E>? =
        mutateAt(predicate) { it?.tail }

fun <E> SList<E>?.drop(index: Int): SList<E>? =
        if (index == 0) this
        else (0..(index - 1)).fold(this) { l, ` ` -> l?.tail }

fun <E> SList<E>?.take(index: Int): SList<E>? =
        splitRevAt(index).first.reverse()

fun <E> SList<E>?.subList(from: Int, to: Int) =
        drop(from).take(to - from)

fun <E> SList<E>?.addAll(that: SList<E>?) = mergeRev(reverse(), that)
fun <E> SList<E>?.add(element: E) = addAll(SList(element))

fun <E> SList<E>?.addAll(index: Int, that: SList<E>?) =
        when (index) {
            0 -> that.addAll(this)
            else -> {
                val (l, r) = splitRevAt(index)
                mergeRev(l, that).addAll(r)
            }
        }

fun <E> SList<E>?.add(index: Int, element: E) = addAll(index, SList(element))

operator fun <E> SList<E>?.plus(that: SList<E>?) = addAll(that)
operator fun <E> E.plus(that: SList<E>?) = SList(this, that)
operator fun <E> SList<E>?.plus(e: E) = addAll(SList(e))!!
operator fun <E> SList<E>?.get(index: Int) = splitRevAt(index).second?.head

data class SListIterator<out E>(var list: SList<@UnsafeVariance E>?) : Iterator<E> {
    override fun hasNext() = list != null

    override fun next(): E {
        if (list == null) throw NoSuchElementException()
        val ret = list!!.head
        list = list!!.tail
        return ret
    }
}

class SListList<E>(private val impl: SList<E>? = null) : ru.spbstu.collections.persistent.impl.AbstractList<E>() {
    override val size: Int by lazy { impl.size }

    override fun get(index: Int): E {
        if (impl == null || index < 0)
            throw IndexOutOfBoundsException()
        var ix = index
        for (e in this) {
            if (ix == 0) return e
            --ix
        }
        throw IndexOutOfBoundsException()
    }

    override fun iterator() = SListIterator(impl)

    override fun listIterator(): ListIterator<E> =
            SZipper(impl).iterator()

    override fun listIterator(index: Int) =
            SZipper(impl).iterator(index)

    override fun subList(fromIndex: Int, toIndex: Int) =
            SListList(impl.subList(fromIndex, toIndex))
}

