package ru.spbstu.collections.persistent

import kotlinx.warnings.Warnings
import ru.spbstu.collections.persistent.Bits.get
import ru.spbstu.collections.persistent.impl.Wrapper
import ru.spbstu.collections.persistent.log2ceil
import java.util.*

import ru.spbstu.collections.persistent.PersistentVectorScope.logBFceil
import ru.spbstu.collections.persistent.PersistentVectorScope.greaterPowerOfBF
import ru.spbstu.collections.persistent.PersistentVectorScope.immSet
import ru.spbstu.collections.persistent.PersistentVectorScope.BF
import ru.spbstu.collections.persistent.PersistentVectorScope.BINARY_DIGITS
import ru.spbstu.collections.persistent.PersistentVectorScope.DIGITS_MASK

object PersistentVectorScope {
    // clojure-style persistent vector is just an implicit segment tree with branching factor of 32
    internal const val BF = 32
    internal const val BINARY_DIGITS = 5 // == log2(BF); number of binary digits needed for one BF digit
    internal const val DIGITS_MASK = (BF - 1) // == '1' bit repeated BINARY_DIGITS times; 0x1F for BF = 32

    @Suppress(Warnings.NOTHING_TO_INLINE)
    internal inline fun logBFceil(v: Int) = (log2ceil(v) - 1) / BINARY_DIGITS + 1
    @Suppress(Warnings.NOTHING_TO_INLINE)
    internal inline fun logBFfloor(v: Int) = log2floor(v) / BINARY_DIGITS
    @Suppress(Warnings.NOTHING_TO_INLINE)
    internal inline fun powBF(v: Int) = 1 shl (v * BINARY_DIGITS)

    internal val Int.greaterPowerOfBF: Int
        get() = powBF(logBFceil(this))
    internal val Int.lesserPowerOfBF: Int
        get() = if (this == 0) 0 else powBF(logBFfloor(this))

    internal fun <E> Array<E>.immSet(index: Int, element: E) =
            this.copyOf().apply { set(index, element) }
}

data class PersistentVector<E> internal constructor(val size: Int = 0, val root: PersistentVectorNode<E> = PersistentVectorNode()) {
    val capacity: Int
        get() = size.greaterPowerOfBF
    val depth: Int
        get() = logBFceil(capacity)

    fun resize(newSize: Int) =
            copy(size = newSize, root = root.getLeftNodeOfSize(capacity, newSize))

    fun set(index: Int, value: E) =
            if (index > size) throw IndexOutOfBoundsException()
            else copy(root = root.set(index, value, depth - 1))

    fun mutate(index: Int, f: (E?) -> E?) =
            copy(root = root.mutate(index, f, depth - 1))

    operator fun get(index: Int) = root.get(index, depth - 1)

    companion object{}

    override fun equals(other: Any?) =
            if(other is PersistentVector<*>) iteratorEquals(iterator(), other.iterator())
            else if(other is Iterable<*>) iteratorEquals(iterator(), other.iterator())
            else false

    override fun hashCode() = iteratorHash(iterator())
    override fun toString() = iterator().asSequence().joinToString(prefix = "[", postfix = "]")
}

fun <E> PersistentVector<E>.add(element: E) =
        resize(size + 1).set(size, element)

fun <E> PersistentVector<E>.removeLast() =
        resize(size - 1)

fun <E> PersistentVector.Companion.ofCollection(elements: Collection<E>) =
        elements.fold(PersistentVector<E>()) { v, e -> v.add(e) }

internal data class PersistentVectorNode<E>(val data: Array<Any?> = Array<Any?>(BF) { null }) {
    @Suppress(Warnings.NOTHING_TO_INLINE)
    internal inline fun Int.adjusted() = this and DIGITS_MASK

    @Suppress(Warnings.NOTHING_TO_INLINE)
    internal inline fun Int.digit(at: Int) = (this ushr (at * BINARY_DIGITS)) and DIGITS_MASK

    internal fun getNode(index: Int): PersistentVectorNode<E> = with(Bits) {
        val realIndex = index.adjusted()
        val ret = data[realIndex] ?: return PersistentVectorNode()
        @Suppress(Warnings.UNCHECKED_CAST)
        return ret as PersistentVectorNode<E>
    }


    internal fun getElement(index: Int): E? =
            with(Bits) { @Suppress(Warnings.UNCHECKED_CAST)(data[index.adjusted()] as? E?) }

    internal fun getLeftNodeOfSize(yourSize: Int, size: Int): PersistentVectorNode<E> = with(this) {
        val adjustedSize = size.greaterPowerOfBF
        if (yourSize == adjustedSize) this
        else if (yourSize > adjustedSize) getNode(0).getLeftNodeOfSize(yourSize / BF, size)
        else PersistentVectorNode<E>(data = Array(BF) { if (it == 0) this else null })
    }

    fun get(index: Int, depthToGo: Int): E? =
            with(Bits) {
                val localIx = index.digit(depthToGo)
                if (data[localIx] == null) null
                else if (depthToGo == 0) getElement(localIx)
                else getNode(localIx).get(index, depthToGo - 1)
            }

    private fun setOrErase(index: Int, element: E?, depthToGo: Int): PersistentVectorNode<E> =
            with(Bits) {
                val localIx = index.digit(depthToGo)
                if (depthToGo == 0) copy(data = data.immSet(localIx, element))
                else {
                    val node = getNode(localIx).setOrErase(index, element, depthToGo - 1)
                    copy(data = data.immSet(localIx, node))
                }
            }

    fun mutate(index: Int, mut: (E?) -> E?, depthToGo: Int): PersistentVectorNode<E> =
            with(Bits) {
                val localIx = index.digit(depthToGo)
                if (depthToGo == 0) {
                    val oldVal = getElement(localIx)
                    val newVal = mut(oldVal)
                    if(oldVal == newVal) this@PersistentVectorNode
                    else {
                        val mutated = data.immSet(localIx, mut(@Suppress(Warnings.UNCHECKED_CAST)(data[localIx] as? E?)))
                        copy(data = mutated)
                    }
                }
                else {
                    val node = getNode(localIx)
                    val newNode = node.mutate(index, mut, depthToGo - 1)
                    if(node === newNode) this@PersistentVectorNode
                    else copy(data = data.immSet(localIx, newNode))
                }
            }

    fun set(index: Int, element: E, depthToGo: Int) = setOrErase(index, element, depthToGo)
    fun erase(index: Int, depthToGo: Int) = setOrErase(index, null, depthToGo)

    // these guys don't really work
    override fun equals(other: Any?) = this === other
    override fun hashCode() = System.identityHashCode(this)
}

data class PersistentVectorIterator<E>(var data: PersistentVector<E>) : Iterator<E?> {
    internal data class IterationState<E>(val depth: Int, var index: Int, val curNode: PersistentVectorNode<E>) {
        val next: IterationState<E>
            get() = IterationState(depth - 1, 0, curNode.getNode(index))
        val currentElement: E?
            get() = curNode.getElement(index)
    }

    private var currentState: IterationState<E> = IterationState(data.depth - 1, 0, data.root)
    private val backStack: Stack<IterationState<E>> = Stack()
    private var totalIx: Int = 0

    private fun ensureLeaf() {
        while (currentState.depth > 0) {
            backStack.push(currentState)
            currentState = currentState.next
        }
    }

    private fun ensureNonEmptyBranch() {
        while (currentState.index == BF) {
            currentState = backStack.pop()
            ++currentState.index
        }
    }

    override fun hasNext() = totalIx < data.size
    override fun next(): E? {
        ++totalIx
        if (totalIx > data.size) {
            throw NoSuchElementException()
        }
        ensureLeaf()

        val ret = currentState.currentElement
        ++currentState.index

        ensureNonEmptyBranch()

        return ret
    }
}

operator fun <E> PersistentVector<E>.iterator() = PersistentVectorIterator(this)

class PersistentVectorList<E>(override val inner: PersistentVector<E>):
        ru.spbstu.collections.persistent.impl.AbstractList<E>(),
        Wrapper<PersistentVector<E>> {
    override val size: Int
        get() = inner.size

    override fun get(index: Int) = withInner { get(index)!! }

    override fun subList(fromIndex: Int, toIndex: Int): List<E> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
