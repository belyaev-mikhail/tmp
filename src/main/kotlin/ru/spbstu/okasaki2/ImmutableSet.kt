package ru.spbstu.okasaki2

interface ImmutableSet<T> : ImmutableCollection<T>, Set<T> {
    override fun add(element: @UnsafeVariance T): ImmutableSet<T>
    override fun remove(element: @UnsafeVariance T): ImmutableSet<T>
    override fun addAll(elements: Collection<@UnsafeVariance T>): ImmutableSet<T>
    override fun removeAll(elements: Collection<@UnsafeVariance T>): ImmutableSet<T>
    override fun retainAll(elements: Collection<@UnsafeVariance T>): ImmutableSet<T>
}

operator fun <T> ImmutableSet<T>.plus(that: Collection<T>): ImmutableSet<T> = addAll(that)
operator fun <T> ImmutableSet<T>.plus(element: T): ImmutableSet<T> = add(element)
operator fun <T> ImmutableSet<T>.minus(element: T): ImmutableSet<T> = remove(element)
operator fun <T> ImmutableSet<T>.minus(that: Collection<T>): ImmutableSet<T> = removeAll(that)
