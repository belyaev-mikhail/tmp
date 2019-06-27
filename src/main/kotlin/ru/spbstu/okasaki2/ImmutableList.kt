package ru.spbstu.okasaki2

interface ImmutableList<out T>: ImmutableCollection<T>, List<T> {
    fun add(index: Int, element: @UnsafeVariance T): ImmutableList<T>
    fun addAll(index: Int, elements: Collection<@UnsafeVariance T>): ImmutableList<T>
    fun removeAt(index: Int): ImmutableList<T>
    fun set(index: Int, element: @UnsafeVariance T): ImmutableList<T>
}
