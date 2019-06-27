package ru.spbstu.collections.persistent

import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class SZipperTest {
    fun testSimple(size: Int, t: SZipper<Int>) {
        assertEquals(t, t.gotoEnd())
        assertEquals(t, t.gotoFront())
        assertEquals(t.gotoEnd(), t.gotoFront())
        assertEquals(t, t.goRight())
        assertEquals(t, t.goLeft())
        assertEquals(t.cursor, t.left.size)
        assertEquals(size, t.size)
        assertEquals(t, t.reverse().reverse())
        val tr = t.reverse()
        (0..(size - 1)).forEach { assertEquals(t[it], tr[size - 1 - it]) }

        assert(t.setCursor(size) strEquals t.gotoEnd())
        assert(t.setCursor(0) strEquals t.gotoFront())

        assertEquals(t.addAll(size, t), t + t)
        assertEquals(t.addAll(0, t), t + t)

        val tt = t + t
        assertEquals(2 * size, tt.size)
        (0..(size-1)).forEach { tt[it] == t[it] && tt[it + size] == t[it] }
        assertEquals(tt, t.addAll(0, t))
        assertEquals(tt, t.addAll(size, t))

        val trt = t.addAll(size/2, sZipperOf(1, 2, 3))
        assertEquals(size + 3, trt.size)
        (0..(size/2 - 1)).forEach { trt[it] == t[it] }
        assertEquals(1, trt[size / 2])
        assertEquals(2, trt[size / 2 + 1])
        assertEquals(3, trt[size / 2 + 2])
        ((size/2 + 3)..(size - 1)).forEach { trt[it] == t[it - 3] }

        val sub = trt.subList(size/2, size/2 + 3)
        assertEquals(sub, sZipperOf(1, 2, 3))

        assertEquals(t, t.subList(0, size))
    }

    @Test
    fun testRandom() {
        val rand = Random()

        val hadBug = sZipperOf(1, 2, 3)
        testSimple(3, hadBug.setCursor(2))

        20.times {
            val size = rand.nextInt(500) + 1
            val t = rand.ints(size.toLong()).toArray()

            val sl = SZipper(SList.Companion.ofCollection(t.toList())).setCursor(rand.nextInt(size))

            testSimple(size, sl)
        }

        testSimple(0, sZipperOf())
        testSimple(1, sZipperOf(2))


    }
}
