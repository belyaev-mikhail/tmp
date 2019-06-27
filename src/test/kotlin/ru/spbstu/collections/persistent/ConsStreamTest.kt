package ru.spbstu.collections.persistent

import kotlinx.warnings.Warnings
import org.junit.Test
import kotlin.test.assertEquals

class ConsStreamTest {
    fun naiveFib(n: Long): Long = if(n <= 1) 1 else naiveFib(n - 1) + naiveFib(n - 2)

    @Test
    fun testSimple() {
        val rep = ConsStream.repeat(2)
        assertEquals(2, rep[100500])

        var callCount = 0
        val walk = ConsStream.walk(4){ (it + 2).apply { ++callCount } }
        assertEquals(116, walk[56])
        assertEquals(116, walk[56])
        assertEquals(116, walk[56])
        assertEquals(56, callCount)

        val naturals = ConsStream.walk(1){ it + 1 }
        assertEquals(40001, naturals[40000])

        val fibs = ConsStream.recurse(1L){ ConsStream.zip(it, 0L + it, Long::plus) }

        val naiveFibs = (0L..20L).map{naiveFib(it)}

        (0..20).forEach { assertEquals(naiveFibs[it], fibs[it]) }

        @Suppress(Warnings.UNUSED_VARIABLE)
        val ` ` = fibs[400000] // should not throw

        assertEquals(12586269025, fibs[49])

        val interchanges = ConsStream.recurse(1){ 2 + it } // 1,2,1,2,1,2,1,2,1,2,1,2,1,2 ...

        (0..200).forEach { assert(interchanges[it] == 2 || interchanges[it] == 1) }

    }
}