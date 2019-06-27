package ru.spbstu.collections.persistent

import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class ImTreapTest {

    fun testSimple(size: Int, t: ImTreap<Int>?) {
        assertEquals(size, t.size)
        val tt = t + t
        assertEquals(2 * size, tt.size)
        (0..(size-1)).forEach { tt[it] == t[it] && tt[it + size] == t[it] }
        assertEquals(tt, t.addAll(0, t))
        assertEquals(tt, t.addAll(size, t))

        val trt = t.addAll(size/2, ImTreap(1,2,3))
        assertEquals(size + 3, trt.size)
        (0..(size/2 - 1)).forEach { trt[it] == t[it] }
        assertEquals(1, trt[size/2])
        assertEquals(2, trt[size/2 + 1])
        assertEquals(3, trt[size/2 + 2])
        ((size/2 + 3)..(size - 1)).forEach { trt[it] == t[it - 3] }

        val sub = trt.subList(size/2, size/2 + 3)
        assertEquals(sub, ImTreap.Companion(1,2,3))

        assertEquals(t, t.subList(0, size))
    }

    @Test
    fun testRandom() {
        val rand = Random()

        20.times{
            val size = rand.nextInt(5000) + 1
            val t = rand.ints(size.toLong(), 0, size).toArray()

            val sl = t.fold(ImTreap<Int>()){ i, e -> i.add(e) }

            testSimple(size, sl)
        }

        testSimple(0, null)
        testSimple(1, ImTreap(2))


    }
}