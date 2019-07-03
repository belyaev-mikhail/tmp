package ru.spbstu.okasaki2

import ru.spbstu.wheels.Stack
import ru.spbstu.wheels.isNotEmpty
import ru.spbstu.wheels.stack
import java.lang.IllegalStateException
import kotlin.random.Random

class ImplicitTreap<T> : ImmutableList<T>, AbstractList<T>, Zippable<T, ImplicitTreap<T>> {
    val random: Random
    val root: Node?

    private constructor(random: Random = Random.Default, root: ImplicitTreap<T>.Node? = null) {
        this.random = random
        this.root = root
    }

    constructor(random: Random = Random.Default): this(random, null)

    fun copy(random: Random = this.random, root: Node? = this.root) =
            ImplicitTreap(random, root)

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

    override fun add(element: T): ImplicitTreap<T> =
            copy(root = root merge Node(element))

    fun Iterable<T>.asNode(): Node? = when {
        this is ImplicitTreap && random === this@ImplicitTreap.random -> root
        else -> fold(null as ImplicitTreap<T>.Node?) { acc, e -> acc merge Node(e) }
    }

    override fun add(index: Int, element: T): ImplicitTreap<T> {
        val (l, r, e) = root.split(index)
        e ?: throw IndexOutOfBoundsException("ImplicitTreap.add()")
        return copy(root = l merge Node(element) merge Node(e) merge r)
    }

    override fun addAll(index: Int, elements: Collection<T>): ImplicitTreap<T> {
        val (l, r, e) = root.split(index)
        e ?: throw IndexOutOfBoundsException("ImplicitTreap.addAll()")
        return copy(root = l merge elements.asNode() merge Node(e) merge r)
    }

    override fun removeAt(index: Int): ImplicitTreap<T> {
        val (l, r, e) = root.split(index)
        e ?: throw IndexOutOfBoundsException("ImplicitTreap.removeAt()")
        return copy(root = l merge r)
    }

    override fun set(index: Int, element: T): ImplicitTreap<T> {
        val (l, r, e) = root.split(index)
        e ?: throw IndexOutOfBoundsException("ImplicitTreap.set()")
        return copy(root = l merge Node(element) merge r)
    }

    override fun addAll(elements: Collection<T>): ImplicitTreap<T> =
            copy(root = root merge elements.asNode())

    private enum class Direction { LEFT, RIGHT }
    private data class Element<T>(val dir: Direction, val node: ImplicitTreap<T>.Node)

    private inner class ImTreapZipper(
            var node: Node?,
            val backstack: Stack<Element<T>> = stack()): Zipper<T, ImplicitTreap<T>> {

        fun goLeft() {
            node = node?.let { node ->
                backstack.push(Element(Direction.LEFT, node))
                node.left
            }
        }

        fun goRight() {
            node = node?.let { node ->
                backstack.push(Element(Direction.RIGHT, node))
                node.right
            }
        }

        fun goUp() {
            val parent = backstack.pop()
            node = when (parent.dir) {
                Direction.LEFT -> {
                    if(parent.node.left === node) parent.node
                    else parent.node.copy(left = node)
                }
                Direction.RIGHT -> {
                    if(parent.node.right === node) parent.node
                    else parent.node.copy(right = node)
                }
            }
        }

        override fun rewind(): ImplicitTreap<T> {
            while(backstack.isNotEmpty()) goUp()
            return when {
                this@ImplicitTreap.root === node -> this@ImplicitTreap
                else -> copy(root = node)
            }
        }

        override fun add(value: T) {
            node = node?.let { node ->
                node.left merge Node(node.value) merge Node(value) merge node.right
            }
        }

        override fun set(value: T) {
            node = node?.let { node ->
                node.left merge Node(value) merge node.right
            }
        }

        override fun remove() {
            this.node = this.node?.let { node ->
                node.left merge node.right
            }
        }

        override fun hasPrevious(): Boolean = node?.left != null

        override fun previous(): T {
            if(node?.left != null) goLeft()
            else {
                while(backstack.top?.dir == Direction.LEFT) goUp()
                goUp()
            }
            return this.node?.value ?: throw NoSuchElementException("previous()")
        }

        override fun hasNext(): Boolean = node != null

        override fun next(): T {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }


    override fun zipper(): Zipper<T, ImplicitTreap<T>> = ImTreapZipper(root)
}

