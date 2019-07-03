package ru.spbstu.okasaki2

interface ImmutableSet<T> : ImmutableCollection<T>, Set<T> {
    override fun add(element: @UnsafeVariance T): ImmutableSet<T>
    override fun remove(element: @UnsafeVariance T): ImmutableSet<T>
    override fun addAll(elements: Collection<@UnsafeVariance T>): ImmutableSet<T> =
            elements.fold(this) { acc, e -> acc.add(e) }
    override fun removeAll(elements: Collection<@UnsafeVariance T>): ImmutableSet<T> =
            elements.fold(this) { acc, e -> acc.remove(e) }
    override fun retainAll(elements: Collection<@UnsafeVariance T>): ImmutableSet<T> {
        var result = this
        for (e in this) if(e !in elements) result = result.remove(e)
        return result
    }
}

operator fun <T> ImmutableSet<T>.plus(that: Collection<T>): ImmutableSet<T> = addAll(that)
operator fun <T> ImmutableSet<T>.plus(element: T): ImmutableSet<T> = add(element)
operator fun <T> ImmutableSet<T>.minus(element: T): ImmutableSet<T> = remove(element)
operator fun <T> ImmutableSet<T>.minus(that: Collection<T>): ImmutableSet<T> = removeAll(that)
