package ru.spbstu.okasaki2

import kotlin.random.Random

class ImTreap<T> : ImmutableList<T>, AbstractList<T> {
    val random: Random
    val root: Node?

    private constructor(random: Random = Random.Default, root: ImTreap<T>.Node? = null) {
        this.random = random
        this.root = root
    }

    constructor(random: Random = Random.Default): this(random, null)

    fun copy(random: Random = this.random, root: Node? = this.root) =
            ImTreap(random, root)

    inner class Node {
        val value: T

        val left: Node?
        val right: Node?

        val priority: Int

        val size: Int

        constructor(payload: T,
                    left: Node? = null,
                    right: Node? = null,
                    priority: Int = random.nextInt()) {
            this.value = payload
            this.left = left
            this.right = right
            this.priority = priority

            this.size = 1 + (left?.size ?: 0) + (right?.size ?: 0)
        }

        fun copy(
                payload: T = this.value,
                left: Node? = this.left,
                right: Node? = this.right,
                priority: Int = this.priority
        ): Node = Node(payload, left, right, priority)


    }

    inline val Node?.currentIndex get() = this?.left?.size ?: 0

    fun Node?.split(index: Int): Triple<Node?, Node?, T?> {
        when {
            null === this -> return Triple(null, null, null)
            index == currentIndex -> return Triple(left, right, value)
            index < currentIndex -> {
                left ?: return Triple(null, this, null)
                val (ll, lr, v) = left.split(index)
                return Triple(ll, copy(left = lr), v)
            }
            else /* index > currentIndex */ -> {
                right ?: return Triple(this, null, null)
                val (rl, rr, v) = right.split(currentIndex)
                return Triple(copy(right = rl), rr, v)
            }
        }
    }

    infix fun Node?.merge(that: Node?): Node? = when {
        this === null -> that
        that === null -> this
        this.priority > that.priority ->
            this.copy(right = this.right merge that)
        else ->
            that.copy(left = this merge that.left)
    }

    override val size: Int
        get() = root?.size ?: 0

    private val Node?.size get() = this?.size ?: 0

    fun Node?.find(index: Int): Node? = when {
        this === null -> null
        index == currentIndex -> this
        index < currentIndex -> left.find(index)
        else -> right.find(index - currentIndex)
    }

    override fun get(index: Int): T =
            root.find(index)?.value ?: throw IndexOutOfBoundsException()

    override fun add(element: T): ImTreap<T> =
            copy(root = root merge Node(element))

    override fun remove(element: T): ImTreap<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun Iterable<T>.asNode(): Node? = when {
        this is ImTreap && random === this@ImTreap.random -> root
        else -> fold(null as ImTreap<T>.Node?) { acc, e -> acc merge Node(e) }
    }

    override fun addAll(elements: Collection<T>): ImTreap<T> =
            copy(root = root merge elements.asNode())

    override fun removeAll(elements: Collection<T>): ImTreap<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun retainAll(elements: Collection<T>): ImTreap<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}