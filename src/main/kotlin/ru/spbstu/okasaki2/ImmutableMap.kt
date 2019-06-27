package ru.spbstu.okasaki2

interface ImmutableMap<K, out V> : Map<K, V> {
    fun put(key: K, value: @UnsafeVariance V): ImmutableMap<K, V>
    fun remove(key: K, value: @UnsafeVariance V): ImmutableMap<K, V>
    fun remove(key: K): ImmutableMap<K, V>

    fun putAll(map: Map<K, @UnsafeVariance V>): ImmutableMap<K, V>

    fun removeAll(map: Map<K, @UnsafeVariance V>): ImmutableMap<K, V>
    fun removeAll(keys: Collection<K>): ImmutableMap<K, V>

}

operator fun <K, V> ImmutableMap<K, V>.plus(map: Map<K, @UnsafeVariance V>): ImmutableMap<K, V> = putAll(map)
operator fun <K, V> ImmutableMap<K, V>.plus(pair: Pair<K, V>): ImmutableMap<K, V> = put(pair.first, pair.second)
operator fun <K, V> ImmutableMap<K, V>.minus(key: K): ImmutableMap<K, V> = remove(key)
operator fun <K, V> ImmutableMap<K, V>.minus(map: Map<K, @UnsafeVariance V>): ImmutableMap<K, V> = removeAll(map)
