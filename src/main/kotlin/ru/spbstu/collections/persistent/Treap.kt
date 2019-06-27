package ru.spbstu.collections.persistent

import kotlinx.warnings.Warnings
import ru.spbstu.collections.persistent.impl.IterableWithDefaults
import ru.spbstu.collections.persistent.impl.Wrapper
import java.util.*
import java.util.concurrent.*

data class Treap<E, P>(
        val root: Node<E, P>? = null,
        val cmp: Comparator<E>,
        val random: Random = Random()
): IterableWithDefaults<E> {
    companion object {}

    constructor(element: E, payload: P, cmp: Comparator<E>, random: Random = Random()):
        this(Node(element, payload, priority = random.nextInt()), cmp, random)

    data class Node<E, P>(
            val key: E,
            val payload: P,
            val left: Node<E, P>? = null,
            val right: Node<E, P>? = null,
            val priority: Int
    )

    override fun iterator(): Iterator<E> = TreapIterator(root)
    override fun equals(other: Any?) = defaultEquals(other)
    override fun hashCode() = defaultHashCode()
    override fun toString() = defaultToString()

    fun isEmpty() = root == null

    operator fun E.compareTo(that: E) = cmp.compare(this, that)

    fun node(
            key: E,
            payload: P,
            left: Node<E, P>? = null,
            right: Node<E, P>? = null,
            priority: Int = random.nextInt()
    ) = Node(key, payload, left, right, priority)

    infix fun Node<E, P>?.merge(that: Node<E, P>?): Node<E, P>? =
            when {
                this == null -> that
                that == null -> this
                this.priority > that.priority -> this.copy(right = this.right merge that)
                else /* this.priority <= that.priority */ -> that.copy(left = this merge that.left)
            }

    fun merge(that: Treap<E, P>) = copy(root = root merge that.root)

    fun Node<E, P>.split(onKey: E): Triple<Node<E, P>?, Node<E, P>?, Boolean> {
        if (key == onKey) return Triple(left, right, true)
        else if (key < onKey) {
            right ?: return Triple(this, null, false)
            val (RL, R, Dup) = right.split(onKey)
            return Triple(this.copy(right = RL), R, Dup)
        } else /* if key > onKey */ {
            left ?: return Triple(null, this, false)
            val (L, LR, Dup) = left.split(onKey)
            return Triple(L, this.copy(left = LR), Dup)
        }
    }

    fun Node<E, P>?.contains(x: E): Boolean =
            when {
                this == null -> false
                key == x -> true
                key > x -> left.contains(x)
                else /* key > x */ -> right.contains(x)
            }

    operator fun contains(key: E) = root.contains(key)

    fun Node<E, P>?.getSubTree(x: E): Node<E, P>? =
            when {
                this == null -> null
                key == x -> this
                key > x -> left.getSubTree(x)
                else /* key > x */ -> right.getSubTree(x)
            }

    fun getSubTree(x: E): Node<E, P>? = root.getSubTree(x)

    fun Node<E, P>?.min(): E? =
            when (this) {
                null -> null
                else -> left.min() ?: key
            }

    fun min() = root.min()

    fun Node<E, P>?.max(): E? =
            when (this) {
                null -> null
                else -> right.max() ?: key
            }

    fun max() = root.max()

    val Node<E, P>?.size: Int
        get() =
        when (this) {
            null -> 0
            else -> 1 + left.size + right.size
        }

    val size: Int get() = root.size

    val Node<E, P>?.height: Int
        get() =
        when (this) {
            null -> 0
            else -> Math.max(left.height, right.height) + 1
        }

    val height: Int get() = root.height

    infix fun Node<E, P>?.union(that: Node<E, P>?): Node<E, P>? {
        this ?: return that
        that ?: return this

        val (T1, T2) =
                if (this.priority < that.priority) Pair(that, this)
                else Pair(this, that)

        val (L, R) = T2.split(T1.key)
        return T1.copy(left = T1.left union L, right = T1.right union R)
    }

    infix fun union(that: Treap<E, P>) = copy(root = root union that.root)

    infix fun Node<E, P>?.intersect(that: Node<E, P>?): Node<E, P>? {
        this ?: return null
        that ?: return null

        if (this.priority < that.priority) return that intersect this

        val (L, R, Dup) = that.split(this.key)
        val Lres = this.left intersect L
        val Rres = this.right intersect R

        if (!Dup) {
            return Lres merge Rres
        } else {
            return this.copy(left = Lres, right = Rres)
        }
    }

    infix fun intersect(that: Treap<E, P>) = copy(root = root intersect that.root)

    fun difference(left: Node<E, P>?, right: Node<E, P>?, rightFromLeft: Boolean): Node<E, P>? {
        if (left == null || right == null) {
            return if (rightFromLeft) left else right
        }

        if (left.priority < right.priority) return difference(right, left, !rightFromLeft)

        val (L, R, Dup) = right.split(left.key)

        val Lres = difference(left.left, L, rightFromLeft)
        val Rres = difference(left.right, R, rightFromLeft)

        if (!Dup && rightFromLeft) {
            return left.copy(left = Lres, right = Rres)
        } else {
            return Lres merge Rres
        }
    }

    infix fun Node<E, P>?.difference(that: Node<E, P>?) = difference(this, that, true)

    infix fun difference(that: Treap<E, P>) = copy(root = root difference that.root)

    @Suppress(Warnings.NOTHING_TO_INLINE)
    inline operator fun <E> ExecutorService.invoke(noinline c: () -> E): Future<E> = submit(c)

    @Suppress(Warnings.NOTHING_TO_INLINE)
    inline operator fun <E> ForkJoinPool.invoke(noinline c: () -> E): ForkJoinTask<E> = submit(c)

    fun Node<E, P>?.punion(that: Node<E, P>?,
                           pool: ExecutorService = Executors.newCachedThreadPool(),
                           factor: Int = Runtime.getRuntime().availableProcessors() / 2): Node<E, P>? {
        if (factor == 0) return this union that

        this ?: return that
        that ?: return this

        val (T1, T2) =
                if (this.priority < that.priority) Pair(that, this)
                else Pair(this, that)

        val (L, R) = T2.split(T1.key)
        val leftTask = pool.invoke { T1.left.punion(L, pool, factor / 2) }
        val right = T1.right.punion(R, pool, factor / 2)

        return T1.copy(left = leftTask.get(), right = right)
    }

    fun punion(that: Treap<E, P>,
               pool: ExecutorService = Executors.newCachedThreadPool(),
               factor: Int = Runtime.getRuntime().availableProcessors() / 2) =
            copy(root = root.punion(that.root, pool, factor))

    fun Node<E, P>?.pintersect(that: Node<E, P>?,
                               pool: ExecutorService = Executors.newCachedThreadPool(),
                               factor: Int = Runtime.getRuntime().availableProcessors() / 2): Node<E, P>? {
        this ?: return null
        that ?: return null

        if (this.priority < that.priority) return that intersect this

        val (L, R, Dup) = that.split(this.key)
        val leftTask = pool.invoke { this.left.pintersect(L, pool, factor / 2) }
        val right = this.right.pintersect(R, pool, factor / 2)

        if (!Dup) {
            return leftTask.get() merge right
        } else {
            return this.copy(left = leftTask.get(), right = right)
        }
    }

    fun pintersect(that: Treap<E, P>,
               pool: ExecutorService = Executors.newCachedThreadPool(),
               factor: Int = Runtime.getRuntime().availableProcessors() / 2) =
            copy(root = root.pintersect(that.root, pool, factor))

    fun pdifference(left: Node<E, P>?, right: Node<E, P>?, rightFromLeft: Boolean,
                    pool: ExecutorService = Executors.newCachedThreadPool(),
                    factor: Int = Runtime.getRuntime().availableProcessors() / 2): Node<E, P>? {
        if (left == null || right == null) {
            return if (rightFromLeft) left else right
        }

        if (left.priority < right.priority) return pdifference(right, left, !rightFromLeft, pool)

        val (L, R, Dup) = right.split(left.key)

        val leftTask = pool.invoke { pdifference(left.left, L, rightFromLeft, pool, factor / 2) }
        val newRight = pdifference(left.right, R, rightFromLeft, pool, factor / 2)

        if (!Dup && rightFromLeft) {
            return left.copy(left = leftTask.get(), right = newRight)
        } else {
            return leftTask.get() merge newRight
        }
    }

    fun pdifference(that: Treap<E, P>,
                    thatFromThis: Boolean,
                    pool: ExecutorService = Executors.newCachedThreadPool(),
                    factor: Int = Runtime.getRuntime().availableProcessors() / 2) =
            copy(root = pdifference(root, that.root, thatFromThis, pool, factor))

}

fun <E, P> Treap<E, P>.add(x: E, p: P): Treap<E, P> {
    root ?: return copy(root = node(x, p))
    val (L, R) = root.split(x)
    val M = node(x, p)
    return copy(root = L merge M merge R)
}

fun <E> Treap<E, Unit>.add(x: E) = add(x, Unit)

fun <E, P> Treap<E, P>.remove(x: E): Treap<E, P> {
    root ?: return this
    val (L, R) = root.split(x)
    return copy(root = L merge R)
}

operator fun<E, P> Treap<E, P>.plus(that: Treap<E, P>) = this union that
operator fun<E, P> Treap<E, P>.minus(that: Treap<E, P>) = this difference that

infix fun <E, P> Treap<E, P>.symDiff(that: Treap<E, P>) = (this + that) - (this intersect that)
infix fun <E, P> Treap<E, P>.pge(that: Treap<E, P>) = (that - this).isEmpty()
infix fun <E, P> Treap<E, P>.plt(that: Treap<E, P>) = !(this pge that)

data class TreapIterator<E, P>(var data: Treap.Node<E, P>?, val nav: Stack<Treap.Node<E, P>> = Stack()) : Iterator<E> {
    override fun hasNext() = nav.isNotEmpty() || data != null

    override fun next(): E {
        while (data != null) {
            nav.push(data)
            data = data?.left
        }
        data = nav.pop()
        val ret = data!!.key
        data = data?.right
        return ret
    }
}

fun <E : Comparable<E>, P> treapOf(): Treap<E, P> = Treap(cmp = naturalOrder() )
fun <E : Comparable<E>> treapOf(e: E): Treap<E, Unit> = Treap(e, Unit, cmp = naturalOrder())
fun <E : Comparable<E>> treapOf(vararg e: E): Treap<E, Unit>
        = e.fold(treapOf()) { t, e -> t.add(e) }
fun <E : Comparable<E>, P> treapOf(e: Pair<E, P>): Treap<E, P> = Treap(e.first, e.second, cmp = naturalOrder())
fun <E : Comparable<E>, P> treapOf(vararg e: Pair<E, P>): Treap<E, P>
        = e.fold(treapOf()) { t, e -> t.add(e.first, e.second) }

fun <E, P> treapOf(cmp: Comparator<E>): Treap<E, P> = Treap(cmp = cmp)
fun <E> treapOf(e: E, cmp: Comparator<E>): Treap<E, Unit> = Treap(e, Unit, cmp = cmp)
fun <E> treapOf(vararg e: E, cmp: Comparator<E>): Treap<E, Unit> =
        e.fold(treapOf(cmp = cmp)) { t, e -> t.add(e) }
fun <E, P> treapOf(e: Pair<E, P>, cmp: Comparator<E>): Treap<E, P> = Treap(e.first, e.second, cmp = cmp)
fun <E, P> treapOf(vararg e: Pair<E, P>, cmp: Comparator<E>): Treap<E, P>
        = e.fold(treapOf(cmp = cmp)) { t, e -> t.add(e.first, e.second) }

fun <E, P> treapOf(cmp: (E, E) -> Int): Treap<E, P> = treapOf(Comparator(cmp))
fun <E> treapOf(e: E, cmp: (E, E) -> Int): Treap<E, Unit> = treapOf(e, Comparator(cmp))
fun <E> treapOf(vararg e: E, cmp: (E, E) -> Int): Treap<E, Unit> =
        e.fold(treapOf(cmp = cmp)) { t, e -> t.add(e) }
fun <E, P> treapOf(e: Pair<E, P>, cmp: (E, E) -> Int): Treap<E, P> = treapOf(e, Comparator(cmp))
fun <E, P> treapOf(vararg e: Pair<E, P>, cmp: (E, E) -> Int): Treap<E, P>
        = e.fold(treapOf(cmp = cmp)) { t, e -> t.add(e.first, e.second) }

class TreapSet<E>(override val inner: Treap<E, Unit>) :
        ru.spbstu.collections.persistent.impl.AbstractSet<E>(),
        Wrapper<Treap<E, Unit>> {
    override val size: Int by lazy { inner.size }

    override fun contains(element: E) = withInner { contains(element) }

    override fun containsAll(elements: Collection<E>) = withInner {
        when (elements) {
            is TreapSet<E> -> this pge elements.inner
            else -> super.containsAll(elements)
        }
    }

    override fun iterator() = withInner { iterator() }
}

fun<E: Comparable<E>> TreapSet() = TreapSet(treapOf<E, Unit>())

class TreapMap<K, V>(override val inner: Treap<K, V>) :
        ru.spbstu.collections.persistent.impl.AbstractMap<K, V>(),
        Wrapper<Treap<K, V>> {
    override fun containsKey(key: K) = key in inner

    override fun get(key: K): V? = inner.getSubTree(key)?.payload

    override val size: Int by lazy { inner.size }
}

fun<K: Comparable<K>, V> TreapMap() = TreapMap(treapOf<K, V>())
