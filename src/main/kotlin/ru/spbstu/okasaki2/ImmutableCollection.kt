package ru.spbstu.okasaki2

interface ImmutableCollection<out T>: Collection<T> {
    fun add(element: @UnsafeVariance T): ImmutableCollection<T>
    fun remove(element: @UnsafeVariance T): ImmutableCollection<T>
    fun addAll(elements: Collection<@UnsafeVariance T>): ImmutableCollection<T>
    fun removeAll(elements: Collection<@UnsafeVariance T>): ImmutableCollection<T>
    fun retainAll(elements: Collection<@UnsafeVariance T>): ImmutableCollection<T>
}

operator fun <T> ImmutableCollection<T>.plus(that: Collection<T>): ImmutableCollection<T> = addAll(that)
operator fun <T> ImmutableCollection<T>.plus(element: T): ImmutableCollection<T> = add(element)
operator fun <T> ImmutableCollection<T>.minus(element: T): ImmutableCollection<T> = remove(element)
operator fun <T> ImmutableCollection<T>.minus(that: Collection<T>): ImmutableCollection<T> = removeAll(that)
