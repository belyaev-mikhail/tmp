package ru.spbstu.collections.persistent

import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class PersistentVectorTest {
    val rand = Random()

    fun testSimple(size: Int, t: PersistentVector<Int>) {
        assertEquals(size, t.size)

        assertEquals(size + 2, t.add(50).add(13).size)
        assertEquals(size + 1, t.add(220).add(14).removeLast().size)
    }

    @Test
    fun testRandom() {
        testSimple(0, PersistentVector.ofCollection(emptyList<Int>()))
        testSimple(1, PersistentVector.ofCollection(listOf(345)))
        testSimple(3, PersistentVector.ofCollection(listOf(1,2,3)))

        20.times{
            val size = rand.nextInt(5000) + 1
            val t = rand.ints(size.toLong()).toArray()

            val sl = PersistentVector.ofCollection(t.toList())

            for(i in (0..size-1)) {
                assertEquals(t[i], sl[i])
            }

            val sl2 = sl.add(600)
            assertEquals(600, sl2[size])
            for(i in (0..size-1)) {
                assertEquals(t[i], sl2[i])
            }

            var ix = 0
            // this is actually testing the iterator implementation, not anything else)
            for(e in sl) {
                assertEquals(t[ix], e)
                ++ix
            }
            assertEquals(size, ix)

            testSimple(size, sl)
        }


    }

}