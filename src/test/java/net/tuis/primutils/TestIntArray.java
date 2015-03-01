package net.tuis.primutils;

import static org.junit.Assert.*;

import java.util.IntSummaryStatistics;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class TestIntArray {

    @Test
    public void testIntArrayInt() {
        IntArray ia = new IntArray(100);
        assertEquals(-1, ia.getHighWaterMark());
    }

    @Test
    public void testIntArray() {
        IntArray ia = new IntArray();
        assertEquals(-1, ia.getHighWaterMark());
    }

    @Test
    public void testStream() {
        IntArray ia = new IntArray();
        ia.get(10000);
        IntSummaryStatistics stats = ia.stream().summaryStatistics();
        assertEquals(10001, stats.getCount());
        assertEquals(0, stats.getMax());
        assertEquals(0, stats.getMin());
    }

    @Test
    public void testGetHighWaterMark() {
        IntArray ia = new IntArray(100);
        assertEquals(-1, ia.getHighWaterMark());
        for (int i = 0; i < 10; i++) {
            ia.set(i, i);
            assertEquals(i, ia.getHighWaterMark());
        }
        for (int i = 5000; i >= 4000; i--) {
            ia.set(i, i);
            assertEquals(5000, ia.getHighWaterMark());
        }
    }

    @Test
    public void testSet() {
        IntArray ia = new IntArray(100);
        assertEquals(0, ia.set(0, 1));
        assertEquals(1, ia.set(0, 2));
        assertEquals(0, ia.set(10000, 1));
        assertEquals(1, ia.set(10000, 2));
    }

    @Test
    public void testGet() {
        IntArray ia = new IntArray(100);
        assertEquals(0, ia.get(0));
        assertEquals(0, ia.getHighWaterMark());
        
        assertEquals(0, ia.get(10000));
        assertEquals(10000, ia.getHighWaterMark());
        assertEquals(0, ia.set(10000, -1));
        assertEquals(-1, ia.get(10000));
    }

    @Test
    public void testPostApply() {
        IntArray ia = new IntArray(100);
        assertEquals(0, ia.postApply(0, i -> i + 10));
        assertEquals(10, ia.get(0));
        assertEquals(10, ia.postApply(0, i -> i + 10));
        assertEquals(20, ia.get(0));
    }

    @Test
    public void testPreApply() {
        IntArray ia = new IntArray(100);
        assertEquals(10, ia.preApply(0, i -> i + 10));
        assertEquals(10, ia.get(0));
        assertEquals(20, ia.preApply(0, i -> i + 10));
        assertEquals(20, ia.get(0));
    }

    @Test
    public void testPostIncrement() {
        IntArray ia = new IntArray(100);
        assertEquals(0, ia.postIncrement(0));
        assertEquals(1, ia.get(0));
        assertEquals(1, ia.postIncrement(0));
        assertEquals(2, ia.get(0));
    }

    @Test
    public void testPreIncrement() {
        IntArray ia = new IntArray(100);
        assertEquals(1, ia.preIncrement(0));
        assertEquals(1, ia.get(0));
        assertEquals(2, ia.preIncrement(0));
        assertEquals(2, ia.get(0));
    }

    @Test
    public void testPostDecrement() {
        IntArray ia = new IntArray(100);
        assertEquals(0, ia.postDecrement(0));
        assertEquals(-1, ia.get(0));
        assertEquals(-1, ia.postDecrement(0));
        assertEquals(-2, ia.get(0));
    }

    @Test
    public void testPreDecrement() {
        IntArray ia = new IntArray(100);
        assertEquals(-1, ia.preDecrement(0));
        assertEquals(-1, ia.get(0));
        assertEquals(-2, ia.preDecrement(0));
        assertEquals(-2, ia.get(0));
    }

}
