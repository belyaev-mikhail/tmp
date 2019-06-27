@file:Suppress(Warnings.NOTHING_TO_INLINE)

package ru.spbstu.collections.persistent

import kotlinx.warnings.Warnings
import ru.spbstu.collections.persistent.HamtScope.BINARY_DIGITS
import java.util.*

import ru.spbstu.collections.persistent.HamtScope.DIGITS_MASK
import ru.spbstu.collections.persistent.HamtScope.popcount
import ru.spbstu.collections.persistent.HamtScope.immInsert
import ru.spbstu.collections.persistent.HamtScope.immSet
import ru.spbstu.collections.persistent.HamtScope.immSetOrRemove

object HamtScope {
    // clojure-style persistent vector is just an implicit segment tree with branching factor of 32
    internal const val BF = 32
    internal const val BINARY_DIGITS = 5 // == log2(BF); number of binary digits needed for one BF digit
    internal const val DIGITS_MASK = (BF - 1) // == '1' bit repeated BINARY_DIGITS times; 0x1F for BF = 32

    internal inline fun logBFceil(v: Int) = (log2ceil(v) - 1) / BINARY_DIGITS + 1
    internal inline fun logBFfloor(v: Int) = log2floor(v) / BINARY_DIGITS
    internal inline fun powBF(v: Int) = 1 shl (v * BINARY_DIGITS)

    internal val Int.greaterPowerOfBF: Int
        get() = powBF(logBFceil(this))
    internal val Int.lesserPowerOfBF: Int
        get() = if (this == 0) 0 else powBF(logBFfloor(this))

    internal fun <E> Array<E>.immSet(index: Int, element: E) =
            if(this[index] === element) this else copyOf().apply{ this[index] = element }
    internal fun <E> Array<E>.immSetOrRemove(index: Int, element: E?) =
            if(element == null) immRemove(index)
            else immSet(index, element)

    @Suppress("UNCHECKED_CAST")
    internal fun <E> Array<E>.immInsert(index: Int, element: E): Array<E> = copyOf(size + 1).apply {
        this[index] = element
        System.arraycopy(this@immInsert, index, this, index + 1, size - index - 1)
    } as Array<E>
    @Suppress("UNCHECKED_CAST")
    internal fun <E> Array<E>.immRemove(index: Int): Array<E> = copyOf(size - 1).apply {
        System.arraycopy(this@immRemove, index, this, index - 1, size - index - 1)
    } as Array<E>

    internal val Int.popcount: Int
            get() = Integer.bitCount(this)
}

data class Hamt<E> internal constructor(val root: HamtNode<E>, val size: Int) {
    operator fun contains(v: E) = root.find(key = v)
    fun add(v: E): Hamt<E> {
        val newRoot = root.put(key = v)
        if(newRoot === root) return this
        else return copy(root = newRoot, size = size + 1)
    }
}

internal data class HamtNode<E>(
        val bitMask: Int = 0,
        val storage: Array<Any?> = Array(0){ null }
) {
    inline fun safeCopy(bitMask: Int = this.bitMask, storage: Array<Any?> = this.storage) =
        if(bitMask === this.bitMask && storage === this.storage) this
        else copy(bitMask = bitMask, storage = storage)

    inline fun squashCopy(bitMask: Int? = this.bitMask, storage: Array<Any?>? = this.storage) =
        if(bitMask == null || storage == null) null
        else safeCopy(bitMask = bitMask, storage = storage)

    internal inline fun Int.digit(index: Int) = (this ushr (index * BINARY_DIGITS)) and DIGITS_MASK
    internal inline fun Int.toIndex() = (bitMask and (this - 1)).popcount
    internal inline fun Int.bitpos(index: Int) = 1 shl digit(index)

    companion object{
        val EMPTY: HamtNode<Any?> = HamtNode(0, arrayOf<Any?>())

        @Suppress(Warnings.UNCHECKED_CAST)
        private inline fun<E> forceNode(v: HamtNode<*>) = v as HamtNode<E>
        @Suppress(Warnings.UNCHECKED_CAST)
        private inline fun<E> forceList(v: SList<*>) = v as SList<E>

        inline fun<E> equiv(lhv: E?, rhv: E?, lhvHashOpt: Int? = null, rhvHashOpt: Int? = null): Boolean {
            if(lhv === rhv) return true
            val lhvHash = lhvHashOpt ?: calcHash(lhv)
            val rhvHash = rhvHashOpt ?: calcHash(rhv)
            if(lhvHash != rhvHash) return false
            return lhv == rhv
        }

        @Suppress(Warnings.NOTHING_TO_INLINE)
        internal inline fun calcHash(key: Any?): Int {
            key ?: return 0
            val h: Int = key.hashCode()
            return h xor h.ushr(16)
        }

        fun<E> makeNode(existing: E?, new: E?, newHash: Int, atDepth: Int): Any? {
            if(equiv(lhv = existing, rhv = new, rhvHashOpt = newHash)) return existing
            val oldHash = calcHash(existing)
            if(newHash == oldHash) return sListOf(existing, new)
            return forceNode<E>(EMPTY)
                    .put(existing, atDepth, oldHash)
                    .put(new, atDepth, newHash)
        }
    }

    fun find(key: E, depth: Int = 0, hash: Int = calcHash(key)): Boolean = with(Bits){
        val bit = 0.setBit(hash.digit(depth))
        if(bitMask and bit != 0) {
            val thing = storage[bit.toIndex()]
            when(thing) {
                is HamtNode<*> -> forceNode<E>(thing).find(key, depth - 1, hash)
                else -> true
            }
        } else false
    }

    fun put(key: E?, depth: Int = 0, hash: Int = calcHash(key)): HamtNode<E> = with(Bits){
        val bit = 0.setBit(hash.digit(depth))
        val index = bit.toIndex()
        if(bitMask and bit != 0) {
            val thing = storage[index]
            when(thing) {
                is HamtNode<*> -> // branch
                    safeCopy(storage = storage.immSet(index, forceNode<E>(thing).put(key, depth + 1, hash)))
                is SList<*> -> { // hash collision
                    val collision = forceList<E>(thing)
                    if(key in collision) this@HamtNode
                    else copy(storage = storage.immSet(index, SList(key, collision)))
                }
                else -> { // single value
                    val existing = @Suppress(Warnings.UNCHECKED_CAST) (thing as? E)
                    if(equiv(lhv = existing, rhv = key, rhvHashOpt = hash)) this@HamtNode
                    else safeCopy(storage = storage.immSet(index, makeNode<E>(existing, key, hash, depth + 1)))
                }
            }
        } else {
            HamtNode(bitMask = bitMask or bit, storage = storage.immInsert(index, key))
        }
    }

    internal fun Array<Any?>.squash(index: Int, element: Any?) =
        if(element == null && size == 1) null
        else immSetOrRemove(index, element)

    internal fun setOrEraseImmediateNode(bit: Int, index: Int, element: Any?) =
        if(element != null) copy(storage = storage.immSet(index, element))
        else squashCopy(bitMask = bitMask and bit.inv(), storage = storage.squash(index, element))

    fun erase(key: E?, depth: Int = 0, hash: Int = calcHash(key)): HamtNode<E>? = with(Bits){
        val bit = 0.setBit(hash.digit(depth))
        val index = bit.toIndex()
        if(bitMask and bit != 0) {
            val thing = storage[index]
            when(thing) {
                is HamtNode<*> -> { // branch
                    val sub = forceNode<E>(thing).erase(key, depth + 1, hash)
                    setOrEraseImmediateNode(bit, index, sub)
                }
                is SList<*> -> { // hash collision
                    val collision = forceList<E>(thing)
                    if(key !in collision) this@HamtNode
                    else setOrEraseImmediateNode(bit, index, collision.removeAt { key == it })
                }
                else -> { // single value
                    val existing = @Suppress(Warnings.UNCHECKED_CAST) (thing as? E)
                    if(equiv(lhv = existing, rhv = key, rhvHashOpt = hash)) {
                        setOrEraseImmediateNode(bit, index, null)
                    } else this@HamtNode
                }
            }
        } else this@HamtNode
    }

    override fun equals(other: Any?) = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

internal fun<E> HamtNode<E>.toSequence(): Sequence<E> {
    val subNodes = storage.asSequence().filterIsInstance<HamtNode<*>>()
    val subValues = storage.asSequence().filter { it !is HamtNode<*> && it != null }

    @Suppress(Warnings.UNCHECKED_CAST)
    return (subValues + subNodes.flatMap { it.toSequence() }) as Sequence<E>
}

operator fun<E> Hamt<E>.iterator() = root.toSequence().iterator()

fun<E> hamtOf(): Hamt<E> = Hamt<E>(@Suppress(Warnings.UNCHECKED_CAST)(HamtNode.EMPTY as HamtNode<E>), 0)
fun<E> hamtOf(vararg elements: E): Hamt<E> = elements.fold(hamtOf<E>()){ hamt, e -> hamt.add(e) }

class HamtSet<E>(val inner: Hamt<E>): ru.spbstu.collections.persistent.impl.AbstractSet<E>() {
    override val size: Int
        get() = inner.size

    override fun iterator(): Iterator<E> = inner.iterator()

    override fun contains(element: E) = inner.contains(element)
}
