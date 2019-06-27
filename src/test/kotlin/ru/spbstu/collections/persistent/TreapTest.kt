package ru.spbstu.collections.persistent

import org.junit.Test
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TreapTest {

    fun testSimple(t0: Treap<Int, Unit>, t1: Treap<Int, Unit>) {
        val intersection0 = t0 intersect t1
        for(e in intersection0) assert(e in t0 && e in t1)

        val intersection1 = t1 intersect t0
        for(e in intersection1) assert(e in t0 && e in t1)

        assertEquals(intersection0, intersection1)
        assertEquals(intersection0, intersection0 intersect intersection1)
        assertEquals(intersection0, intersection0 union intersection1)
        assertEquals(treapOf(), intersection0 difference intersection1)

        for(e in t0) assert(e in intersection0 && e in t0 && e in t1 || e !in intersection0)
        for(e in t1) assert(e in intersection0 && e in t0 && e in t1 || e !in intersection0)

        val union0 = t0 union t1
        for(e in union0) assert(e in t0 || e in t1)

        val union1 = t1 union t0
        for(e in union1) assert(e in t0 || e in t1)

        for(e in t0) assert(e in union0)
        for(e in t1) assert(e in union1)

        assertEquals(union0, union1)
        assertEquals(union1, (union0 intersect union1))
        assertEquals(union1, (union0 union union1))
        assertEquals(treapOf(), (union0 difference union1))

        for(e in intersection0) assert(e in union0)
        assertEquals(t0 intersect union0, t0)
        assertEquals(t1 intersect union0, t1)

        val difference0 = t0 difference t1
        for(e in difference0) assert(e in t0 && e !in t1)
        val difference1 = t1 difference t0
        for(e in difference1) assert(e in t1 && e !in t0)

        assertEquals(treapOf(), difference0 intersect difference1)
        assertEquals(treapOf(), (difference0 + difference1) intersect intersection0)
        assertEquals(difference0 + difference1 + intersection0, union0)

        val t0max = t0.max() ?: 0
        for(e in t0) assert(t0max >= e)

        val t0min = t0.min() ?: 0
        for(e in t0) assert(t0min <= e)
    }

    @Test
    fun testRandom() {
        val rand = Random()
        testSimple(treapOf(), treapOf())
        testSimple(treapOf(2,2,2,2,2), treapOf(2))
        10.times{
            val r = rand.nextInt()
            testSimple(treapOf(r), treapOf(r))
            testSimple(treapOf(r), treapOf(-r))
        }

        100.times{
            val threshold = rand.nextInt(5000) + 1 // should not be zero
            val data0 = rand.ints(threshold.toLong(), -threshold, threshold).toArray()
            val data1 = rand.ints(threshold.toLong(), -threshold, threshold).toArray()
            val t0 = data0.fold(treapOf<Int, Unit>()){ t, e -> t.add(e) }
            val t1 = data1.fold(treapOf<Int, Unit>()){ t, e -> t.add(e) }

            assertTrue(t0.height <= log2ceil(t0.size) * 4)
            assertTrue(t1.height <= log2ceil(t1.size) * 4)

            for(e in data0) { assert(e in t0) }
            for(e in data1) { assert(e in t1) }
            testSimple(t0, t1)

            testSimple(t0, treapOf())
            testSimple(treapOf(), t1)
            testSimple(t0, treapOf(rand.nextInt(threshold)))
            testSimple(t0, treapOf(rand.nextInt()))
            testSimple(t0, t1 - t0)
            testSimple(t0, t1 + t0)
        }

    }

    @Test
    fun testHandCrafted() {
        val t = treapOf(1,2,2,2,3,2)
        assertEquals(treapOf(3,2,1), t)
        assertEquals(treapOf(), t - t)
        assertEquals(t, t + t)
        assertEquals(t, t intersect t)
        assert(2 in t)
        assert(5 !in t)

        val t23 = t - treapOf(1)
        assert(1 !in t23)

        assertNotEquals(treapOf(1,2,3,4), treapOf(1,2,3))

    }

}