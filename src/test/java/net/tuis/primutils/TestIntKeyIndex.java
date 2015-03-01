package net.tuis.primutils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class TestIntKeyIndex {

    @Test
    public void testIntKeyIndexIntIntSmall() {
        IntKeyIndex iim = new IntKeyIndex(0);
        assertEquals(0, iim.size());
        assertTrue(iim.isEmpty());
        assertEquals(1 << 4, iim.getBucketCount());
    }

    @Test
    public void testIntKeyIndexIntIntNegative() {
        IntKeyIndex iim = new IntKeyIndex(-1);
        assertEquals(0, iim.size());
        assertTrue(iim.isEmpty());
        assertEquals(1 << 4, iim.getBucketCount());
    }

    @Test
    public void testIntKeyIndexIntIntHuge() {
        IntKeyIndex iim = new IntKeyIndex(Integer.MAX_VALUE >> 4);
        assertEquals(0, iim.size());
        assertEquals(1 << 21, iim.getBucketCount());
    }

    @Test
    public void testIntKeyIndexInt() {
        IntKeyIndex iim = new IntKeyIndex(-1);
        assertEquals(0, iim.size());
        assertTrue(iim.isEmpty());
        assertEquals(1 << 4, iim.getBucketCount());
        
    }

    @Test
    public void testAdd() {
        IntKeyIndex iim = new IntKeyIndex(-1);
        assertEquals(-1, iim.add(1));
        assertEquals(1, iim.size());
        assertFalse(iim.isEmpty());
        
        assertEquals( 0, iim.add(1));
        assertEquals( 0, iim.add(1));
        
        assertEquals( -2, iim.add(Integer.MAX_VALUE));
        assertEquals(  1, iim.add(Integer.MAX_VALUE));
        
        assertEquals( -3, iim.add(Integer.MIN_VALUE));
        assertEquals(  2, iim.add(Integer.MIN_VALUE));

        assertEquals( 0, iim.add(1));
        assertEquals( 1, iim.add(Integer.MAX_VALUE));
        assertEquals( 2, iim.add(Integer.MIN_VALUE));
    }

    @Test
    public void testSize() {
        IntKeyIndex iim = new IntKeyIndex(-1);
        assertEquals(0, iim.size());
        assertTrue(iim.isEmpty());
        assertEquals(-1, iim.add(1));
        assertEquals(1, iim.size());
        assertFalse(iim.isEmpty());
        assertEquals(-2, iim.add(2));
        assertEquals(2, iim.size());
        assertFalse(iim.isEmpty());

        assertEquals(1, iim.add(2));
        assertEquals(2, iim.size());
        
        assertEquals(-3, iim.add(Integer.MAX_VALUE));
        assertEquals(3, iim.size());
        assertEquals(3, iim.getSumSizes());
        
    }

    @Test
    public void testGet() {
        IntKeyIndex iim = new IntKeyIndex(-1);
        assertEquals(-1, iim.getIndex(1));
        assertEquals(-1, iim.getIndex(2));
        assertEquals(-1, iim.add(1));
        assertEquals( 0, iim.getIndex(1));
        assertEquals(-1, iim.getIndex(2));
        assertEquals(-2, iim.add(2));
        assertEquals( 0, iim.getIndex(1));
        assertEquals( 1, iim.getIndex(2));
    }

    @Test
    public void testContainsKey() {
        IntKeyIndex iim = new IntKeyIndex(-1);

        assertFalse(iim.containsKey(1));
        assertFalse(iim.containsKey(2));

        assertEquals(-1, iim.add(1));
        assertTrue(iim.containsKey(1));
        assertFalse(iim.containsKey(2));

        assertEquals(-2, iim.add(2));
        assertTrue(iim.containsKey(1));
        assertTrue(iim.containsKey(2));
        
    }

    @Test
    public void testClear() {
        IntKeyIndex iim = new IntKeyIndex(-1);

        assertEquals(-1, iim.add(1));
        assertEquals(-2, iim.add(2));
        
        assertEquals(2, iim.size());
        
        iim.clear();
        
        assertTrue(iim.isEmpty());
        assertEquals(0, iim.size());
    }

    @Test
    public void testRemove() {
        IntKeyIndex iim = new IntKeyIndex(-1);

        assertEquals(-1, iim.remove(1));
        assertEquals(-1, iim.add(1));
        assertEquals(-2, iim.add(2));
        assertEquals(2, iim.getSumSizes());
        
        assertEquals(0, iim.getIndex(1));
        assertEquals(0, iim.remove(1));
        assertEquals(1, iim.getSumSizes());
        
        assertEquals(-1, iim.getIndex(1));
        
        assertFalse(iim.isEmpty());
        assertEquals(1, iim.size());
        
        assertEquals(-1, iim.getIndex(5));
        assertEquals(-1, iim.remove(5));
        assertEquals(1, iim.getSumSizes());
        
        assertEquals(1, iim.getIndex(2));
        assertEquals(1, iim.remove(2));
        assertEquals(0, iim.getSumSizes());
        assertEquals(-1, iim.getIndex(2));
        assertEquals(-1, iim.remove(2));
        
        assertTrue(iim.isEmpty());
        assertEquals(0, iim.size());
        
    }

    @Test
    public void testStreamKeys() {
        IntKeyIndex iim = new IntKeyIndex(-1);
        assertEquals(0, iim.streamKeys().count());
        assertEquals(-1, iim.add(1));
        assertEquals(1, iim.streamKeys().count());
        assertEquals(1, iim.streamKeys().findFirst().getAsInt());
    }

    @Test
    public void testGetKeys() {
        IntKeyIndex iim = new IntKeyIndex(-1);
        int[] keys = IntStream.range(0, 1000).toArray();
        IntStream.of(keys).forEach(k -> iim.add(k));
        assertEquals(keys.length, iim.size());
        int[] got = iim.getKeys();
        Arrays.sort(got);
        assertArrayEquals(keys, got);
    }

    @Test
    public void testGetIndices() {
        IntKeyIndex iim = new IntKeyIndex(-1);
        int[] keys = IntStream.range(0, 1000).toArray();
        IntStream.of(keys).forEach(k -> iim.add(k));
        assertEquals(keys.length, iim.size());
        int[] gotk = iim.getKeys();
        int[] gotv = iim.getIndices();
        assertArrayEquals(gotk, gotv);
        Arrays.sort(gotv);
        assertArrayEquals(keys, gotv);
    }

    @Test
    public void testStreamValues() {
        IntKeyIndex iim = new IntKeyIndex(-1);
        assertEquals(0, iim.streamIndices().count());
        assertEquals(-1, iim.add(1));
        assertEquals(1, iim.streamIndices().count());
        assertEquals(0, iim.streamIndices().findFirst().getAsInt());
    }

    @Test
    public void testHCEquals() {
        IntKeyIndex iia = new IntKeyIndex(1);
        IntKeyIndex iib = new IntKeyIndex(2);
        
        assertEquals(iia.hashCode(), iib.hashCode());
        assertEquals(iia, iib);

        iia.add(1);
        
        assertNotEquals(iia.hashCode(), iib.hashCode());
        assertNotEquals(iia, iib);

        iib.add(2);
        
        assertNotEquals(iia.hashCode(), iib.hashCode());
        assertNotEquals(iia, iib);

        iib.add(4);
        
        assertNotEquals(iia.hashCode(), iib.hashCode());
        assertNotEquals(iia, iib);

        iia.add(4);
        
        assertNotEquals(iia.hashCode(), iib.hashCode());
        assertNotEquals(iia, iib);

        iia.add(2);
        
        assertNotEquals(iia.hashCode(), iib.hashCode());
        assertNotEquals(iia, iib);

        iia.remove(1);
        
        assertNotEquals(iia.hashCode(), iib.hashCode());
        assertNotEquals(iia, iib);

    }

}
