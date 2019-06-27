package ru.spbstu.collections.persistent

import java.util.*

class AmortizedQueue<E> private constructor(val inputs: SList<E>?, val outputs: SList<E>?) {

    constructor() : this(null, null)

    fun copy(inputs: SList<E>? = this.inputs, outputs: SList<E>? = this.outputs) =
            AmortizedQueue(inputs, outputs)

    fun empty() = outputs == null

    fun push(x: E) =
            when (outputs) {
                null -> copy(outputs = SList(x))
                else -> copy(inputs = SList(x, inputs))
            }

    fun pop() =
            when (outputs?.tail) {
                null -> copy(outputs = inputs.reverse())
                else -> copy(outputs = outputs?.tail)
            }

    val top: E
        get() = outputs!!.head

    fun toSList() = inputs + outputs.reverse()

    override fun toString() = "$inputs><$outputs"

    companion object
}

