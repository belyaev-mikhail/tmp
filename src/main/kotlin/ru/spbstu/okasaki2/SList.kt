package ru.spbstu.okasaki2

sealed class SList<out E>: ImmutableCollection<E>, AbstractCollection<E>(), Zippable<E, SList<E>> {
    object Empty: SList<Nothing>() {
        override val size: Int = 0
        override fun iterator(): Iterator<Nothing> = iterator<Nothing> {  }
    }

    override fun iterator(): Iterator<E> = iterator<E> { forEachElement { yield(it) } }
    override val size: Int get() = fold(0) { a, _ -> a + 1 }

    override fun zipper(): Zipper<@UnsafeVariance E, SList<E>> = SListZipper(this)

    override fun add(element: @UnsafeVariance E): SList<E> {
        val z = zipper()
        while(z.hasNext()) z.next()
        z.add(element)
        return z.rewind()
    }
    override fun remove(element: @UnsafeVariance E): SList<E> {
        val z = zipper()
        while(z.hasNext()) {
            val v = z.next()
            if(v == element) {
                z.remove()
            }
        }
        return z.rewind()
    }

    override fun addAll(elements: Collection<@UnsafeVariance E>): SList<E> = when(elements) {
        is SList -> SListZipper(current = elements, history = this).rewind()
        else -> {
            val z = zipper()
            while (z.hasNext()) z.next()
            for (e in elements) {
                z.add(e)
                z.next()
            }
            z.rewind()
        }
    }

    override fun removeAll(elements: Collection<@UnsafeVariance E>): ImmutableCollection<E> {
        val z = zipper()
        while(z.hasNext()) {
            val v = z.next()
            if(v in elements) {
                z.remove()
            }
        }
        return z.rewind()
    }

    override fun retainAll(elements: Collection<@UnsafeVariance E>): ImmutableCollection<E> {
        val z = zipper()
        while(z.hasNext()) {
            val v = z.next()
            if(v !in elements) {
                z.remove()
            }
        }
        return z.rewind()
    }
}

inline fun <E> SList<E>.forEachElement(body: (E) -> Unit) {
    var current: SList<E> = this
    while(current is Cons) {
        body(current.head)
        current = current.tail
    }
}

inline fun <E, A> SList<E>.fold(acc: A, body: (A, E) -> A): A {
    var current: SList<E> = this
    var result = acc
    while(current is Cons) {
        result = body(result, current.head)
        current = current.tail
    }
    return result
}

class Cons<out E>(val head: E, val tail: SList<E> = SList.Empty): SList<E>()

private class SListZipper<E>(var current: SList<E>, var history: SList<E> = SList.Empty): Zipper<E, SList<E>> {

    override fun add(value: E) {
        current = Cons(value, current)
    }

    override fun set(value: E) {
        when (val c = current) {
            is SList.Empty -> throw IndexOutOfBoundsException()
            is Cons -> current = Cons(value, c.tail)
        }
    }

    override fun remove() {
        when (val c = current) {
            is SList.Empty -> throw IndexOutOfBoundsException()
            is Cons -> current = c.tail
        }
    }

    override fun hasPrevious(): Boolean = history !is SList.Empty
    override fun previous(): E = when(val h = history) {
        is SList.Empty -> throw IndexOutOfBoundsException()
        is Cons -> {
            current = Cons(h.head, current)
            history = h.tail
            h.head
        }
    }

    override fun hasNext(): Boolean = current !is SList.Empty
    override fun next(): E = when(val c = current) {
        is SList.Empty -> throw IndexOutOfBoundsException()
        is Cons -> {
            history = Cons(c.head, history)
            current = c.tail
            c.head
        }
    }

    override fun rewind(): SList<E> {
        while(hasPrevious()) previous()
        return current
    }

}

fun <T> slistOf(): SList<T> = SList.Empty
fun <T> slistOf(v: T): SList<T> = Cons(v)
fun <T> slistOf(vararg vs: T): SList<T> {
    var res: SList<T> = SList.Empty
    for(i in 1..vs.size) {
        res = Cons(vs[vs.size - i], res)
    }
    return res
}


fun main() {
    println(slistOf(1,2,3))

    println(slistOf(1,2,3).add(4).add(5).add(6))
    println(slistOf(1,2,3).addAll(listOf(4, 5, 6)))
}
