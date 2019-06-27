package ru.spbstu.collections.persistent

import kotlinx.warnings.Warnings

data class PVectorHashtable<E>(private val holder: PersistentVector<SList<E>?>) {

    @Suppress(Warnings.NOTHING_TO_INLINE)
    internal inline fun calcHash(key: Any?): Int {
        key ?: return 0
        val h: Int = key.hashCode()
        return h xor h.ushr(16)
    }

    operator fun contains(value: E) = holder[calcHash(value)].find { it == value } != null
    fun add(value: E) = run {
        val hash = calcHash(value)
        copy(holder = holder.mutate(hash){ list -> list.find { it == value }?.let{ list } ?: list.add(0, value) })
    }
    fun remove(value: E) = run {
        val hash = calcHash(value)
        copy(holder = holder.mutate(hash){ list -> list.removeAt { it == value } })
    }
}