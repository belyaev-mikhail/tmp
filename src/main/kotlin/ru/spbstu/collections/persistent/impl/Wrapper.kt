package ru.spbstu.collections.persistent.impl

interface Wrapper<E> {
    val inner: E

    fun<R> withInner(body: E.() -> R) = inner.body()
}