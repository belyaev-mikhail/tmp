package ru.spbstu.collections.persistent

import org.junit.Test
import java.util.*
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream
import kotlin.test.assertEquals

class MathTest {
    @Test
    fun testPowersOf2() {
        val rand = Random()
        val ints = rand.ints(2000, 0, Int.MAX_VALUE / 2).asSequence() + 0 + 1

        ints.forEach { i ->
            val pow2ceil = i.greaterPowerOf2
            val pow2floor = i.lesserPowerOf2

            assert(pow2ceil >= i)
            assert(pow2floor <= i)

            assert(pow2ceil.greaterPowerOf2 == pow2ceil)
            assert(pow2ceil.lesserPowerOf2 == pow2ceil)
            assert(pow2floor == 0 || pow2floor.greaterPowerOf2 == pow2floor)
            assert(pow2floor == 0 || pow2floor.lesserPowerOf2 == pow2floor)

            assert(pow2ceil % 2 == 0 || pow2ceil == 1)
            assert(pow2floor % 2 == 0 || pow2floor == 1)

            assert(log2ceil(pow2ceil) == log2ceil(i))
            assert(log2floor(pow2floor) == log2floor(i))
            if (pow2floor != 0) assert(pow2ceil / pow2floor in (1..2))
        }
    }

    @Test
    fun testPowersOf32() {
        val rand = Random()
        with(PersistentVectorScope) {
            val ints = rand.ints(2000, 0, Int.MAX_VALUE / BF).asSequence() + rand.nextInt(BF) + 0 + 1

            ints.forEach { i -> // random int > 0

                val pow32ceil = i.greaterPowerOfBF
                val pow32floor = i.lesserPowerOfBF

                assert(pow32ceil >= i)
                assert(pow32floor <= i)

                assert(pow32ceil.greaterPowerOf2 == pow32ceil)
                assert(pow32ceil.lesserPowerOf2 == pow32ceil)
                assert(pow32floor == 0 || pow32floor.greaterPowerOf2 == pow32floor)
                assert(pow32floor == 0 || pow32floor.lesserPowerOf2 == pow32floor)

                assert(pow32ceil % 2 == 0)
                assert(pow32floor % 2 == 0 || pow32floor == 1) // don't forget 1 is a legit power of anything

                assert(logBFceil(pow32ceil) == logBFceil(i))
                assert(logBFfloor(pow32floor) == logBFfloor(i))
                if (pow32floor != 0) assert(pow32ceil / pow32floor in listOf(1, BF))

            }
        }
    }

    data class HexInt(val i: Int){
        override fun toString() = Integer.toHexString(i)
    }

    @Test
    fun testBits() {
        val rand = Random()
        val ints = rand.ints(2000).asSequence() + rand.nextInt(32) + 0 + 1
        val longs = rand.longs(2000).asSequence() + rand.nextInt(32).toLong() + 0 + 1

        with(Bits) {
            assertEquals(1, 1[0, 0])
            assertEquals(0, 2[0, 0])

            assertEquals(HexInt(0x0DEADBEE[8, 23]), HexInt(0xEADB))

            ints.forEach { i ->
                assertEquals(i, i[0, 31])
                assertEquals(if(i < 0) 1 else 0, i[31, 31])
                assertEquals(i.toShort(), i[0, 15].toShort())
                assertEquals(i.toByte(), i[0, 7].toByte())
            }

            longs.forEach { i ->
                assertEquals(i, i[0, 63])
                assertEquals(if(i < 0) 1L else 0L, i[63, 63])
                assertEquals(i.toInt(), i[0, 31].toInt())
                assertEquals(i.toShort(), i[0, 15].toShort())
                assertEquals(i.toByte(), i[0, 7].toByte())
            }
        }

    }
}