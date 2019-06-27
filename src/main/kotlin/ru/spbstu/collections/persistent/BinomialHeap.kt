package ru.spbstu.collections.persistent

import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

data class BinomialHeapNode<E>(val max: E, val subNodes: SList<BinomialHeapNode<E>>? = null) {
    override fun toString() = "(" + debugString() + ")"
    fun debugString(): String = "$max" + (subNodes?.joinToString(prefix = ", "){ it.debugString() } ?: "")
}

data class BinomialHeap<E> internal constructor(
        val nodes: SList<BinomialHeapNode<E>?>? = null,
        internal val cmpOpt: Comparator<E?>,
        val max: E? = nodes.foldLeft(null as E?) { e, t -> cmpOpt.max(e, t?.max) }
) {
    override fun toString() = "Heap<$max,$nodes>"
    fun debugString() = nodes?.joinToString{ "(" + (it?.debugString() ?: "") + ")"} ?: ""

    internal operator fun E.compareTo(that: E) = cmpOpt.compare(this, that)
    internal fun BinomialHeapNode<E>.asHeap() =
            if (subNodes == null) BinomialHeap(cmpOpt = cmpOpt)
            else BinomialHeap(subNodes.reverse(), cmpOpt)

    internal infix fun BinomialHeapNode<E>.merge(that: BinomialHeapNode<E>) =
            if (this.max > that.max) this.copy(subNodes = SList(that, this.subNodes))
            else that.copy(subNodes = SList(this, that.subNodes))

    internal infix fun BinomialHeapNode<E>?.mergeWithCarry(that: BinomialHeapNode<E>?):
            Pair<BinomialHeapNode<E>?, BinomialHeapNode<E>?> =
            when{
                this == null -> Pair(that, null)
                that == null -> Pair(this, null)
                else -> Pair(null, this merge that)
            }

    val size: Int
        get() = with(Bits){
            var ret: Int = 0
            nodes?.forEachIndexed { i, e ->
                if(e != null) ret = ret.setBit(i)
            }
            ret
        }

}

infix fun <E> BinomialHeap<E>.merge(that: BinomialHeap<E>): BinomialHeap<E> {
    this.max ?: return that
    that.max ?: return this

    var nodes: SList<BinomialHeapNode<E>?>? = null
    var carry: BinomialHeapNode<E>? = null
    val thisIt = this.nodes.iterator()
    val thatIt = that.nodes.iterator()
    while (thisIt.hasNext() || thatIt.hasNext()) {
        val thisVal = thisIt.nextOrNull()
        val thatVal = thatIt.nextOrNull()
        val (sum0, carry0) = thisVal mergeWithCarry thatVal
        val (sum1, carry1) = sum0 mergeWithCarry carry
        nodes = SList(sum1, nodes)
        carry = (carry0 mergeWithCarry carry1).apply { assert(second == null) }.first
    }
    if(carry != null) nodes = SList(carry, nodes)

    val res = copy(max = cmpOpt.max(this.max, that.max), nodes = nodes.reverse())
    return res
}

fun <E> binomialHeapOf(element: E, cmp: Comparator<E>) =
        BinomialHeap(SList(BinomialHeapNode(element)), cmp.nullsFirst())

fun <E> binomialHeapOf(cmp: Comparator<E>) =
        BinomialHeap(sListOf(), cmp.nullsFirst())

fun <E> binomialHeapOf(element: E, cmp: (E, E) -> Int) =
        BinomialHeap(sListOf(BinomialHeapNode(element)), Comparator(cmp).nullsFirst())

fun <E> binomialHeapOf(vararg element: E, cmp: Comparator<E>) =
        element.map { binomialHeapOf(it, cmp) }.reduce { lh, rh -> lh merge rh }

fun <E : Comparable<E>> binomialHeapOf(element: E) = binomialHeapOf(element, cmp = naturalOrder())
fun <E : Comparable<E>> binomialHeapOf() = binomialHeapOf(cmp = naturalOrder<E>())
fun <E : Comparable<E>> binomialHeapOf(vararg element: E) = binomialHeapOf(*element, cmp = naturalOrder<E>())

fun <E : Comparable<E>> BinomialHeap(elements: Collection<E>) =
        elements.fold(binomialHeapOf<E>()) { col, e -> col.add(e) }

fun <E> BinomialHeap(elements: Collection<E>, cmp: Comparator<E>) =
        elements.fold(binomialHeapOf<E>(cmp)) { col, e -> col.add(e) }

fun <E> BinomialHeap(elements: Collection<E>, cmp: (E, E) -> Int) =
        elements.fold(binomialHeapOf<E>(Comparator(cmp))) { col, e -> col.add(e) }

fun <E> BinomialHeap<E>.add(element: E) = this merge BinomialHeap(sListOf(BinomialHeapNode(element)), cmpOpt, element)

fun <E> BinomialHeap<E>.addAll(that: BinomialHeap<E>) = this merge that

inline fun Int.toBigInteger() = toLong().toBigInteger()
inline fun Long.toBigInteger() = BigInteger.valueOf(this)
inline fun Short.toBigInteger() = toLong().toBigInteger()
inline fun Double.toBigInteger() = toBigDecimal().toBigInteger()
inline fun Float.toBigInteger() = toBigDecimal().toBigInteger()

inline fun Int.toBigDecimal() = toDouble().toBigDecimal()
inline fun Long.toBigDecimal() = toDouble().toBigDecimal()
inline fun Short.toBigDecimal() = toDouble().toBigDecimal()
inline fun Double.toBigDecimal() = BigDecimal.valueOf(this)
inline fun Float.toBigDecimal() = toDouble().toBigDecimal()

fun bar() {
    val bi = BigInteger.valueOf(2)
    val rs = 3.15 / 2
}

fun <E> BinomialHeap<E>.popMax(): BinomialHeap<E> {
    this.max ?: return this

    val maxTree = nodes.foldLeft<BinomialHeapNode<E>?, BinomialHeapNode<E>?>(null) { acc, t ->
        when {
            acc == null -> t
            t == null -> acc
            (t.max > acc.max) -> t
            else -> acc
        }
    }
    maxTree ?: return this
    val maxless = BinomialHeap(nodes = nodes.removeAt { it === maxTree }, cmpOpt = cmpOpt)
    val maxHeap = maxTree.asHeap()

    return maxless merge maxHeap
}

infix operator fun <E> BinomialHeap<E>.plus(t: BinomialHeap<E>) = addAll(t)

