package net.tuis.primutils;

import static org.junit.Assert.*;

import java.util.IntSummaryStatistics;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class TestIntArray {

    @Test
    public void testIntArray() {
        IntArray ia = new IntArray();
        assertEquals(-1, ia.getBound());
    }

    @Test
    public void testStream() {
        IntArray ia = new IntArray();
        ia.setBound(10000);
        IntSummaryStatistics stats = ia.stream().summaryStatistics();
        assertEquals(10001, stats.getCount());
        assertEquals(0, stats.getMax());
        assertEquals(0, stats.getMin());
    }

    @Test
    public void testStreamMax() {
        long expect = (long)Integer.MAX_VALUE + 1L;
        long start = System.nanoTime();
        IntArray ia = new IntArray(Integer.MAX_VALUE);
        
        assertEquals(expect, ia.stream().parallel().count());
        long time = System.nanoTime() - start;
        System.out.printf("Scanned %d values in %.3fs\n", expect, time / 1000000.0);
    }

    @Test
    public void testGetHighWaterMark() {
        IntArray ia = new IntArray();
        assertEquals(-1, ia.getBound());
        for (int i = 0; i < 10; i++) {
            ia.push(i);
            assertEquals(i, ia.getBound());
        }
        ia.setBound(5000);
        for (int i = 5000; i >= 4000; i--) {
            ia.set(i, i);
            assertEquals(5000, ia.getBound());
        }
    }

    @Test
    public void testSet() {
        IntArray ia = new IntArray();
        ia.setBound(10000);
        assertEquals(0, ia.set(0, 1));
        assertEquals(1, ia.set(0, 2));
        assertEquals(0, ia.set(10000, 1));
        assertEquals(1, ia.set(10000, 2));
    }

    @Test
    public void testGet() {
        IntArray ia = new IntArray();
        ia.push(0);
        assertEquals(0, ia.get(0));
        assertEquals(0, ia.getBound());
        
        ia.setBound(10000);
        assertEquals(0, ia.get(10000));
        assertEquals(10000, ia.getBound());
        assertEquals(0, ia.set(10000, -1));
        assertEquals(-1, ia.get(10000));
    }

    @Test
    public void testPostApply() {
        IntArray ia = new IntArray();
        ia.setBound(20);
        assertEquals(0, ia.postApply(0, i -> i + 10));
        assertEquals(10, ia.get(0));
        assertEquals(10, ia.postApply(0, i -> i + 10));
        assertEquals(20, ia.get(0));
    }

    @Test
    public void testPreApply() {
        IntArray ia = new IntArray();
        ia.setBound(0);
        assertEquals(10, ia.preApply(0, i -> i + 10));
        assertEquals(10, ia.get(0));
        assertEquals(20, ia.preApply(0, i -> i + 10));
        assertEquals(20, ia.get(0));
    }

    @Test
    public void testPostIncrement() {
        IntArray ia = new IntArray();
        ia.setBound(0);
        assertEquals(0, ia.postIncrement(0));
        assertEquals(1, ia.get(0));
        assertEquals(1, ia.postIncrement(0));
        assertEquals(2, ia.get(0));
    }

    @Test
    public void testPreIncrement() {
        IntArray ia = new IntArray();
        ia.setBound(0);
        assertEquals(1, ia.preIncrement(0));
        assertEquals(1, ia.get(0));
        assertEquals(2, ia.preIncrement(0));
        assertEquals(2, ia.get(0));
    }

    @Test
    public void testPostDecrement() {
        IntArray ia = new IntArray();
        ia.setBound(0);
        assertEquals(0, ia.postDecrement(0));
        assertEquals(-1, ia.get(0));
        assertEquals(-1, ia.postDecrement(0));
        assertEquals(-2, ia.get(0));
    }

    @Test
    public void testPreDecrement() {
        IntArray ia = new IntArray();
        ia.setBound(0);
        assertEquals(-1, ia.preDecrement(0));
        assertEquals(-1, ia.get(0));
        assertEquals(-2, ia.preDecrement(0));
        assertEquals(-2, ia.get(0));
    }

}
