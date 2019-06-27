package ru.spbstu.okasaki2

import kotlin.random.Random

class Treap<K, out V>: ImmutableMap<K, V>, AbstractMap<K, V> {
    val cmp: Comparator<K>
    val random: Random
    val root: Node? = null

    override val size: Int by lazy { root?.size ?: 0 }

    constructor(cmp: Comparator<K>, random: Random = Random.Default): super() {
        this.cmp = cmp
        this.random = random
    }

    private constructor(cmp: Comparator<K>, random: Random = Random.Default, root: Treap<K, V>.Node?): super() {
        this.cmp = cmp
        this.random = random
    }

    fun copy(cmp: Comparator<K> = this.cmp, random: Random = this.random, root: Node? = this.root) =
            Treap(cmp, random, root)

    inner class Node : Map.Entry<K, V> {
        override val key: K
        override val value: V

        val left: Node?
        val right: Node?

        val priority: Int

        constructor(key: K, value: V, left: Node? = null, right: Node? = null, priority: Int = random.nextInt()) {
            this.key = key
            this.value = value
            this.left = left
            this.right = right

            this.priority = priority
        }

        fun copy(key: K = this.key,
                 value: @UnsafeVariance V = this.value,
                 left: Node? = this.left,
                 right: Node? = this.right,
                 priority: Int = this.priority): Node =
                Node(key, value, left, right, priority)

        val size: Int get() = (left?.size ?: 0) + (right?.size ?: 0) + 1
    }

    operator fun K.compareTo(that: K) = cmp.compare(this, that)

    fun find(key: K): Node? {
        var cur = root
        while (cur != null) {
            val cmp = cur.key.compareTo(key)
            when {
                cmp == 0 -> return cur
                cmp < 0 -> cur = cur.left
                cmp > 0 -> cur = cur.right
            }
        }
        return null
    }
    override fun containsKey(key: K): Boolean = find(key) !== null
    override fun get(key: K): V? = find(key)?.value


    fun Node?.split(onKey: K): Triple<Node?, Node?, V?> {
        when {
            null === this -> return Triple(null, null, null)
            onKey == key -> return Triple(left, right, value)
            onKey < key -> {
                left ?: return Triple(null, this, null)
                val (ll, lr, v) = left.split(onKey)
                return Triple(ll, copy(left = lr), v)
            }
            else /* onKey > key */ -> {
                right ?: return Triple(this, null, null)
                val (rl, rr, v) = right.split(onKey)
                return Triple(copy(right = rl), rr, v)
            }
        }
    }

    infix fun Node?.merge(that: Node?): Node? = when {
        null === this -> that
        null === that -> this
        this.priority > that.priority ->
            this.copy(right = this.right.merge(that))
        else ->
            that.copy(left = this.merge(that.left))
    }


    override fun put(key: K, value: @UnsafeVariance V): Treap<K, V> = run {
        val (l, r, _) = root.split(key)
        copy(root = l merge Node(key, value) merge r)
    }

    override fun remove(key: K, value: @UnsafeVariance V): Treap<K, V> = run {
        val (l, r, v) = root.split(key)
        if(v == value) copy(root = l merge r)
        else this
    }

    override fun remove(key: K): Treap<K, V> = run {
        val (l, r, v) = root.split(key)
        if(v !== null) copy(root = l merge r)
        else this
    }

    infix fun Node?.union(that: Node?): Node? {
        this ?: return that
        that ?: return this

        return if(this.priority < that.priority) {
            val (l, r, _) = this.split(that.key)
            that.copy(left = l union that.left, right = r union that.right)
        } else {
            val (l, r, v) = that.split(this.key)
            this.copy(left = this.left union l, right = this.right union r, value = v ?: this.value)
        }
    }

    override fun putAll(map: Map<K, @UnsafeVariance V>): Treap<K, V> = when {
        map is Treap && map.random === random && map.cmp == cmp -> {
            copy(root = root union map.root)
        }
        else -> {
            var res = this
            map.forEach { res = res.put(it.key, it.value) }
            res
        }
    }

    override fun removeAll(map: Map<K, @UnsafeVariance V>): Treap<K, V> {
        var res = this
        map.forEach { (k, v) -> res = res.remove(k, v) }
        return res
    }

    override fun removeAll(keys: Collection<K>): ImmutableMap<K, V> {
        var res = this
        keys.forEach { k -> res = res.remove(k) }
        return res
    }

    operator fun Node?.iterator(): Iterator<Map.Entry<K, V>> = let { node ->
        iterator {
            if(node !== null) {
                yieldAll(node.left.iterator())
                yield(node as Map.Entry<K, V>)
                yieldAll(node.right.iterator())
            }
        }
    }

    inner class EntrySet : AbstractSet<Map.Entry<K, V>>() {
        override val size: Int
            get() = this@Treap.size

        override fun contains(element: Map.Entry<K, @UnsafeVariance V>): Boolean =
                get(element.key) == element.value

        override fun iterator(): Iterator<Map.Entry<K, V>> = root.iterator()
    }

    override val entries: Set<Map.Entry<K, V>>
        get() = EntrySet()
}

class TreapSet<T>(val delegate: Treap<T, Unit>) : ImmutableSet<T>, AbstractSet<T>() {
    constructor(cmp: Comparator<T>, random: Random = Random.Default): this(Treap(cmp, random))

    override fun add(element: T): TreapSet<T> = TreapSet(delegate.put(element, Unit))
    override fun remove(element: T): TreapSet<T> = TreapSet(delegate.remove(element))
    override fun addAll(elements: Collection<T>): TreapSet<T> =
        when {
            elements is TreapSet -> TreapSet(delegate.putAll(elements.delegate))
            else -> elements.fold(this) { acc, t -> acc.add(t) }
        }

    override fun removeAll(elements: Collection<T>): TreapSet<T> =
            when {
                elements is TreapSet -> TreapSet(delegate.removeAll(elements.delegate))
                else -> elements.fold(this) { acc, t -> acc.remove(t) }
            }

    override fun retainAll(elements: Collection<T>): TreapSet<T> =
            elements.fold(TreapSet(delegate.cmp, delegate.random)) { acc, e ->
                if(e in this) acc.add(e)
                else acc
            }

    override val size: Int
        get() = delegate.size

    override fun contains(element: T): Boolean = delegate.containsKey(element)
    override fun containsAll(elements: Collection<T>): Boolean = elements.all { delegate.containsKey(it) }
    override fun iterator(): Iterator<T> = iterator {
        val inner = delegate.iterator()
        for((k, _) in inner) yield(k)
    }
}

fun <K : Comparable<K>, V> Treap(random: Random = Random.Default): Treap<K, V> =
        Treap(naturalOrder(), random)

fun <T : Comparable<T>> TreapSet(random: Random = Random.Default): TreapSet<T> =
        TreapSet(naturalOrder(), random)

