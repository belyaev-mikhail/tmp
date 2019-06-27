package ru.spbstu.collections.persistent

import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class BinomialHeapTest {

    fun testSimple(size: Int, t: BinomialHeap<Int>, elements: List<Int>) {
        assertEquals(size, t.size)
        val tt = t + t
        assertEquals(2 * size, tt.size)

        val sortedInput = elements.sorted().reversed()
        assertEquals(sortedInput.first(), tt.max)

        val tts = tt.popMax()
        assertEquals(sortedInput.first(), tts.max)
        val tts2 = tts.popMax()
        if(size == 1) {
            assertEquals(0, tts2.size)
        } else {
            assertEquals(sortedInput[1], tts2.max)
        }
    }

    @Test
    fun testRandom() {
        val rand = Random()

        20 times {
            val size = rand.nextInt(500) + 1
            val t = rand.ints(size.toLong(), 0, 600000).toArray().toList()

            val sl = BinomialHeap(t)

            testSimple(size, sl, t)
        }

        testSimple(1, binomialHeapOf(2), listOf(2))
    }
}

