package ru.spbstu.collections.persistent

import ru.spbstu.collections.persistent.impl.IterableWithDefaults
import java.util.*

data class SZipper<E>
private constructor(
        val right: SList<E>? = null,
        val left: SList<E>? = null,
        val cursor: Int = left.size,
        val size: Int = cursor + right.size) : IterableWithDefaults<E> {
    constructor(inner: SList<E>?) : this(right = inner)

    fun goLeft() =
            if (left == null) this
            else copy(
                    left = left.tail,
                    right = SList(left.head, right),
                    cursor = cursor - 1
            )

    fun goRight() =
            if (right == null) this
            else copy(
                    right = right.tail,
                    left = SList(right.head, left),
                    cursor = cursor + 1
            )

    fun gotoEnd(): SZipper<E> {
        var wha = this
        while (wha.right != null) wha = wha.goRight()
        return wha
    }

    fun gotoFront() = setCursor(0)

    fun asSList() = gotoFront().right

    fun setCursor(newCursor: Int) =
            when {
                (cursor == newCursor) -> this
                (cursor > newCursor) -> {
                    val shift: Int = cursor - newCursor
                    val (lright, lleft) = left.splitRevAt(shift)
                    // now we have
                    // lleft == list[0..newCursor].reverse()
                    // lright == list[newCursor..cursor]
                    // right == list[cursor, size]
                    copy(left = lleft, right = lright + right, cursor = newCursor)
                }
                (cursor < newCursor) -> {
                    val shift = newCursor - cursor
                    val (rleft, rright) = right.splitRevAt(shift)
                    // now we have
                    // left == list[0..cursor].reverse()
                    // rleft == list[cursor..newCursor].reverse()
                    // rright == list[newCursor..size]
                    copy(left = mergeRev(rleft.reverse(), left), right = rright, cursor = newCursor)
                }
                else -> this
            }

    val current: E
        get() = right!!.head

    override operator fun iterator() = gotoFront().iteratorHere()

    override fun equals(other: Any?) =
            if (other is SZipper<*>) iteratorEquals(iterator(), other.iterator())
            else false

    infix fun strEquals(other: SZipper<E>)
            = left == other.left && right == other.right && cursor == other.cursor && size == other.size

    override fun hashCode() = iteratorHash(iterator())

    override fun toString() = "$left{$cursor}$right"

    companion object
}


fun <E> SZipper<E>.addHere(e: E) =
        copy(right = SList(e, right), size = size + 1)

fun <E> SZipper<E>.addAllHere(elements: SZipper<E>) =
        copy(right = elements.asSList() + right, size = size + elements.size)

fun <E> SZipper<E>.removeHere() =
        copy(right = right?.tail, size = size - 1)

fun <E> SZipper<E>.reverse() =
        copy(right = left, left = right, cursor = size - cursor)

fun <E> SZipper<E>.add(where: Int, e: E) = setCursor(where).addHere(e)
fun <E> SZipper<E>.remove(where: Int) = setCursor(where).removeHere()

fun <E> SZipper<E>.addAll(where: Int, elements: SZipper<E>) =
        setCursor(where).addAllHere(elements)

fun <E> SZipper<E>.add(e: E) =
        gotoEnd().addHere(e)

fun <E> SZipper<E>.addAll(elements: SZipper<E>) =
        gotoEnd().addAllHere(elements)

fun <E> SZipper<E>.iteratorHere() = SZipperIterator(this)

fun <E> SZipper<E>.iterator(where: Int) = setCursor(where).iteratorHere()

fun <E> SZipper<E>.take(where: Int): SZipper<E> {
    val l = setCursor(where).left
    return copy(right = null, left = l, cursor = where, size = where)
}

fun <E> SZipper<E>.drop(where: Int): SZipper<E> {
    val r = setCursor(where).right
    return copy(right = r, left = null, cursor = 0, size = size - where)
}

fun <E> SZipper<E>.subList(from: Int, to: Int) =
        drop(from).take(to - from)

operator fun <E> SZipper<E>.plus(that: SZipper<E>) = addAll(that)
operator fun <E> E.plus(that: SZipper<E>) = that.add(0, this)
operator fun <E> SZipper<E>.plus(e: E) = add(e)
operator fun <E> SZipper<E>.get(index: Int) = setCursor(index).current

fun <E> sZipperOf(e: E) = SZipper(SList(e))
fun <E> sZipperOf(vararg e: E) = SZipper(sListOf(*e))

data class SZipperIterator<E>(var data: SZipper<E>) : ListIterator<E> {
    override fun hasPrevious() = data.left != null
    override fun previousIndex() = nextIndex() - 1
    override fun next() = data.current butAlso { data = data.goRight() }
    override fun previous() = { data = data.goLeft() } andReturn data.current

    override fun nextIndex() = data.cursor
    override fun hasNext() = data.right != null
}

class SZipperList<E>(val zipper: SZipper<E>) : ru.spbstu.collections.persistent.impl.AbstractList<E>() {
    override fun subList(fromIndex: Int, toIndex: Int) =
            SZipperList(zipper.subList(fromIndex, toIndex))

    override fun listIterator(): ListIterator<E> =
            zipper.iterator()

    override fun listIterator(index: Int): ListIterator<E> =
            zipper.iterator(index)

    override fun get(index: Int): E =
            zipper.get(index)

    override val size: Int
        get() = zipper.size
}
