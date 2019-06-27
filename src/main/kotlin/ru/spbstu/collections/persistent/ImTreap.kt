package ru.spbstu.collections.persistent

import java.util.*

data class ImTreap<out E>(
        val payload: E,
        val left: ImTreap<E>? = null,
        val right: ImTreap<E>? = null,
        val random: Random = Random(),
        val priority: Int = random.nextInt()
) {
    companion object{}
    val realSize: Int = left.size + right.size + 1

    fun copy(): ImTreap<E> = this

    override fun equals(other: Any?): Boolean =
        if(other is ImTreap<*>)
            iteratorEquals(iterator(), other.iterator())
        else false

    override fun hashCode() =
        iteratorHash(iterator())

    override fun toString(): String =
            iterator().asSequence().joinToString(prefix = "[", postfix = "]")
}

val<E> ImTreap<E>?.size: Int
        get() = this?.realSize ?: 0

infix fun<E> ImTreap<E>?.merge(that: ImTreap<E>?): ImTreap<E>? =
    when {
        this == null                              -> that
        that == null                              -> this
        this.priority > that.priority             -> this.copy(right = this.right merge that)
        else /* this.priority <= that.priority */ -> that.copy(left  = this merge that.left)
    }

fun<E> ImTreap<E>.split(onIndex: Int): Triple<ImTreap<E>?, ImTreap<E>?, E?> {
    val currentIndex = left.size
    if(currentIndex == onIndex) return Triple(left, right, payload)
    else if(currentIndex < onIndex) {
        right ?: return Triple(this, null, null)
        val(RL, R, El) = right.split(onIndex - left.size - 1)
        return Triple(this.copy(right = RL), R, El)
    } else /* if currentIndex > onIndex */ {
        left ?: return Triple(null, this, null)
        val(L, LR, El) = left.split(onIndex)
        return Triple(L, this.copy(left = LR), El)
    }
}

operator fun <E> ImTreap<E>?.get(index: Int): E =
    when {
        index !in (0..size)     -> throw IndexOutOfBoundsException()
        this == null            -> throw IndexOutOfBoundsException()
        index == this.left.size -> payload
        index <  this.left.size -> this.left.get(index)
        else                    -> this.right.get(index - this.left.size - 1)
    }

fun<E> ImTreap<E>?.addAll(index: Int, elems: ImTreap<E>?): ImTreap<E>? {
    this ?: return elems
    val(L, R, El) = split(index)
    val M = El?.let { ImTreap(it, random = random) }
    return (L merge elems merge M merge R) ?: elems
}

fun<E> ImTreap<E>?.add(index: Int, elem: E): ImTreap<E> = addAll(index, ImTreap(elem))!!

fun<E> ImTreap<E>?.remove(index: Int): ImTreap<E>? {
    this ?: return null
    val(L, R) = split(index)
    return (L merge R)
}

fun<E> ImTreap<E>?.addAll(that: ImTreap<E>?): ImTreap<E>? {
    this ?: return that
    that ?: return this
    return this merge that
}

fun<E> ImTreap<E>?.add(elem: E): ImTreap<E> = (this merge ImTreap(elem))!!

operator fun<E> ImTreap<E>?.plus(that: ImTreap<E>?): ImTreap<E>? = addAll(that)
operator fun<E> E.plus(that: ImTreap<E>?): ImTreap<E> = that.add(0, this)
operator fun<E> ImTreap<E>?.plus(e: E): ImTreap<E> = add(e)

fun<E> ImTreap<E>?.drop(ix: Int) = this?.split(ix - 1)?.second
fun<E> ImTreap<E>?.take(ix: Int) = this?.split(ix)?.first
fun<E> ImTreap<E>?.subList(from: Int, to: Int) = drop(from).take(to - from)

data class ImTreapIterator<E>(var data: ImTreap<E>?, val nav: Stack<ImTreap<E>> = Stack()): Iterator<E> {
    override fun hasNext() = nav.isNotEmpty() || data != null

    override fun next(): E {
        while(data != null) {
            nav.push(data)
            data = data?.left
        }
        data = nav.pop()
        val ret = data!!.payload
        data = data?.right
        return ret
    }
}

operator fun<E> ImTreap<E>?.iterator() = ImTreapIterator(this)

operator fun<E> ImTreap.Companion.invoke(): ImTreap<E>? = null
operator fun<E> ImTreap.Companion.invoke(e: E): ImTreap<E> = ImTreap(e)
operator fun<E> ImTreap.Companion.invoke(vararg e: E): ImTreap<E>?
        = e.fold(invoke()){ t, e -> t.add(e) }

class ImTreapList<E>(val inner: ImTreap<E>? = null): ru.spbstu.collections.persistent.impl.AbstractList<E>() {
    override val size: Int
        get() = inner.size

    override fun get(index: Int) = inner.get(index)

    override fun iterator() = ImTreapIterator(inner)

    override fun subList(fromIndex: Int, toIndex: Int): List<E> =
            ImTreapList(inner.subList(fromIndex, toIndex))
}
