package ru.spbstu.okasaki2

interface Zipper<T, out C: Zippable<T, C>>: Iterator<T> {
    fun rewind(): C

    fun add(value: T)
    fun set(value: T)
    fun remove()

    fun hasPrevious(): Boolean
    fun previous(): T

    override fun hasNext(): Boolean
    override fun next(): T
}

interface Zippable<out T, out C: Zippable<T, C>>: Iterable<T> {
    fun zipper(): Zipper<@UnsafeVariance T, C>
    override fun iterator(): Iterator<T> = zipper()
}
