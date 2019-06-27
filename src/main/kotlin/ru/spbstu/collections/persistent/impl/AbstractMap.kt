package ru.spbstu.collections.persistent.impl

abstract class AbstractMap<K, V> : Map<K, V> {
    class DefaultKeySet<K, V>(val parent: Map<K, V>): AbstractSet<K>() {
        override val size: Int get() = parent.size
        override fun contains(element: K) = parent.contains(element)
        override fun iterator(): Iterator<K> = object : Iterator<K> {
            val inner = parent.iterator()
            override fun hasNext(): Boolean = inner.hasNext()
            override fun next(): K = inner.next().key
        }
    }

    override val keys: Set<K>
        get() = DefaultKeySet<K, V>(this)

    class DefaultValueSet<K, V>(val parent: Map<K, V>): AbstractCollection<V>() {
        override val size: Int get() = parent.size
        override fun iterator(): Iterator<V> = object : Iterator<V> {
            val inner = parent.iterator()
            override fun hasNext(): Boolean = inner.hasNext()
            override fun next(): V = inner.next().value
        }
    }

    override val values: Collection<V>
        get() = DefaultValueSet(this)

    override fun containsValue(value: V) = values.contains(value)

    data class DefaultEntry<K, V>(override val key: K, override val value: V): Map.Entry<K, V>
    class DefaultEntrySet<K, V>(val parent: Map<K, V>): AbstractSet<Map.Entry<K, V>>() {
        override val size: Int get() = parent.size
        override fun contains(element: Map.Entry<K, V>) =
                parent.getOrElse(element.key){ null } == element.value
        override fun iterator(): Iterator<Map.Entry<K, V>> = object : Iterator<Map.Entry<K, V>> {
            val inner = parent.iterator()
            override fun hasNext(): Boolean = inner.hasNext()
            override fun next(): Map.Entry<K, V> = inner.next().let { DefaultEntry(it.key, it.value) }
        }
    }

    override val entries: Set<Map.Entry<K, V>>
        get() = DefaultEntrySet(this)

    override fun isEmpty() = size == 0

    override fun equals(other: Any?) =
        when{
            other === this -> true
            other !is Map<*, *> -> false
            else -> entries == other.entries
        }

    override fun hashCode() = entries.hashCode()

    override fun toString() = entries.toString()
}