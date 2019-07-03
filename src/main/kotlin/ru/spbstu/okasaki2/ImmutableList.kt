package ru.spbstu.okasaki2

interface ImmutableList<out T>: ImmutableCollection<T>, List<T> {
    fun add(index: Int, element: @UnsafeVariance T): ImmutableList<T>
    fun addAll(index: Int, elements: Collection<@UnsafeVariance T>): ImmutableList<T> {
        var i = index
        var res = this
        for(e in elements) {
            res = res.add(i, e)
            ++i
        }
        return res
    }
    fun removeAt(index: Int): ImmutableList<T>
    fun set(index: Int, element: @UnsafeVariance T): ImmutableList<T>

    override fun remove(element: @UnsafeVariance T): ImmutableList<T> = removeAt(indexOf(element))
}
