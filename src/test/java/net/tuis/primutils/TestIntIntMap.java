package net.tuis.primutils;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

import net.tuis.ubench.UBench;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class TestIntIntMap {

    @Test
    public void testIntIntMapIntIntSmall() {
        IntIntMap iim = new IntIntMap(-1, 0);
        assertEquals(0, iim.size());
        assertTrue(iim.isEmpty());
    }

    @Test
    public void testIntIntMapIntIntNegative() {
        IntIntMap iim = new IntIntMap(-1, -1);
        assertEquals(0, iim.size());
        assertTrue(iim.isEmpty());
    }

    @Test
    public void testIntIntMapIntIntHuge() {
        IntIntMap iim = new IntIntMap(-1, Integer.MAX_VALUE >> 4);
        assertEquals(0, iim.size());
    }

    @Test
    public void testIntIntMapInt() {
        IntIntMap iim = new IntIntMap(-1);
        assertEquals(0, iim.size());
        assertTrue(iim.isEmpty());
    }

    @Test
    public void testGetNotThere() {
        IntIntMap iim = new IntIntMap(-1);
        assertEquals(-1, iim.getNotThere());
    }

    @Test
    public void testPut() {
        IntIntMap iim = new IntIntMap(-1);
        assertEquals(-1, iim.put(1, 1));
        assertEquals(1, iim.size());
        assertFalse(iim.isEmpty());
        
        assertEquals( 1, iim.put(1, 5));
        assertEquals( 5, iim.put(1, 3));
        
        assertEquals( -1, iim.put(Integer.MAX_VALUE, 1));
        assertEquals(  1, iim.put(Integer.MAX_VALUE, 5));
        assertEquals(  5, iim.put(Integer.MAX_VALUE, 3));
        
        assertEquals( -1, iim.put(Integer.MIN_VALUE, 1));
        assertEquals(  1, iim.put(Integer.MIN_VALUE, 5));
        assertEquals(  5, iim.put(Integer.MIN_VALUE, 3));
    }

    @Test
    public void testSize() {
        IntIntMap iim = new IntIntMap(-1);
        assertEquals(0, iim.size());
        assertTrue(iim.isEmpty());
        assertEquals(-1, iim.put(1, 1));
        assertEquals(1, iim.size());
        assertFalse(iim.isEmpty());
        assertEquals(-1, iim.put(2, 2));
        assertEquals(2, iim.size());
        assertFalse(iim.isEmpty());

        assertEquals(2, iim.put(2, 20));
        assertEquals(2, iim.size());
        
        assertEquals(-1, iim.put(Integer.MAX_VALUE, 20));
        assertEquals(3, iim.size());
        
    }

    @Test
    public void testGet() {
        IntIntMap iim = new IntIntMap(-1);
        assertEquals(-1, iim.get(1));
        assertEquals(-1, iim.get(2));
        assertEquals(-1, iim.put(1, 1));
        assertEquals( 1, iim.get(1));
        assertEquals(-1, iim.get(2));
        assertEquals(-1, iim.put(2, 2));
        assertEquals( 1, iim.get(1));
        assertEquals( 2, iim.get(2));
    }

    @Test
    public void testContainsKey() {
        IntIntMap iim = new IntIntMap(-1);

        assertFalse(iim.containsKey(1));
        assertFalse(iim.containsKey(2));

        assertEquals(-1, iim.put(1, 1));
        assertTrue(iim.containsKey(1));
        assertFalse(iim.containsKey(2));

        assertEquals(-1, iim.put(2, 2));
        assertTrue(iim.containsKey(1));
        assertTrue(iim.containsKey(2));
        
    }

    @Test
    public void testClear() {
        IntIntMap iim = new IntIntMap(-1);

        assertEquals(-1, iim.put(1, 1));
        assertEquals(-1, iim.put(2, 2));
        
        assertEquals(2, iim.size());
        
        iim.clear();
        
        assertTrue(iim.isEmpty());
        assertEquals(0, iim.size());
    }

    @Test
    public void testRemove() {
        IntIntMap iim = new IntIntMap(-1);

        assertEquals(-1, iim.remove(1));
        assertEquals(-1, iim.put(1, 1));
        assertEquals(-1, iim.put(2, 2));
        
        assertEquals(1, iim.get(1));
        assertEquals(1, iim.remove(1));
        
        assertEquals(-1, iim.get(1));
        
        assertFalse(iim.isEmpty());
        assertEquals(1, iim.size());
        
        assertEquals(-1, iim.get(5));
        assertEquals(-1, iim.remove(5));
        
        assertEquals(2, iim.get(2));
        assertEquals(2, iim.remove(2));
        assertEquals(-1, iim.get(2));
        assertEquals(-1, iim.remove(2));
        
        assertTrue(iim.isEmpty());
        assertEquals(0, iim.size());
        
    }

    @Test
    public void testStreamKeys() {
        IntIntMap iim = new IntIntMap(-1);
        assertEquals(0, iim.streamKeys().count());
        assertEquals(-1, iim.put(1, 1));
        assertEquals(1, iim.streamKeys().count());
        assertEquals(1, iim.streamKeys().findFirst().getAsInt());
    }

    @Test
    public void testGetKeys() {
        IntIntMap iim = new IntIntMap(-1);
        int[] keys = IntStream.range(0, 1000).toArray();
        IntStream.of(keys).forEach(k -> iim.put(k, k));
        assertEquals(keys.length, iim.size());
        int[] got = iim.getKeys();
        Arrays.sort(got);
        assertArrayEquals(keys, got);
    }

    @Test
    public void testGetValues() {
        IntIntMap iim = new IntIntMap(-1);
        int[] keys = IntStream.range(0, 1000).toArray();
        IntStream.of(keys).forEach(k -> iim.put(k, k));
        assertEquals(keys.length, iim.size());
        int[] gotk = iim.getKeys();
        int[] gotv = iim.getValues();
        assertArrayEquals(gotk, gotv);
        Arrays.sort(gotv);
        assertArrayEquals(keys, gotv);
    }

    @Test
    public void testStreamValues() {
        IntIntMap iim = new IntIntMap(-1);
        assertEquals(0, iim.streamValues().count());
        assertEquals(-1, iim.put(1, 1));
        assertEquals(1, iim.streamValues().count());
        assertEquals(1, iim.streamValues().findFirst().getAsInt());
    }

    @Test
    public void testForEach() {
        IntIntMap iim = new IntIntMap(-1);
        int[] keys = IntStream.range(0, 1000).toArray();
        IntStream.of(keys).forEach(k -> iim.put(k, k));
        assertEquals(keys.length, iim.size());
        
        final LongAdder adder = new LongAdder();
        iim.forEach((k,v) -> {
            adder.add(v);
            return 0;
        });
        
        long act = IntStream.of(keys).mapToLong(i -> i).sum();
        assertEquals(act, adder.longValue());
        adder.reset();
        iim.forEach((k,v) -> {
            adder.add(v);
            return k;
        });
        
        assertEquals(0, adder.longValue());
        
        assertEquals(act, iim.streamValues().sum());
        assertEquals(act, iim.streamKeys().sum());
        
    }

    @Test
    public void testStreamEntries() {
        IntIntMap iim = new IntIntMap(-1);
        int[] keys = IntStream.range(0, 1000).toArray();
        IntStream.of(keys).forEach(k -> iim.put(k, -k));
        assertEquals(keys.length, iim.size());
        
        int kexpect = IntStream.of(keys).sum();
        int kgot = iim.streamEntries().mapToInt(e -> e.getKey()).sum();
        assertEquals(kexpect, kgot);
        int vgot = iim.streamEntries().mapToInt(e -> e.getValue()).sum();
        assertEquals(kexpect, -vgot);
        
        assertTrue(iim.streamEntries().allMatch(e -> e.getKey() == - e.getValue()));
        
    }
    
    @Test
    public void testParallel() {
        IntIntMap iim = new IntIntMap(10000);
        int sum = 0;
        for (int i = 0; i < 1000000; i++) {
            sum += i;
            iim.put(i, -i);
        }
        assertEquals(sum, iim.streamKeys().sum());
        assertEquals(sum, iim.streamKeys().parallel().sum());
    }
    
    
    @Test
    public void testHCEquals() {
        IntIntMap iia = new IntIntMap(1);
        IntIntMap iib = new IntIntMap(2);
        
        assertEquals(iia.hashCode(), iib.hashCode());
        assertEquals(iia, iib);

        iia.put(1, 1);
        
        assertNotEquals(iia.hashCode(), iib.hashCode());
        assertNotEquals(iia, iib);

        iib.put(1, 2);
        
        assertNotEquals(iia.hashCode(), iib.hashCode());
        assertNotEquals(iia, iib);

        iib.put(4, 4);
        
        assertNotEquals(iia.hashCode(), iib.hashCode());
        assertNotEquals(iia, iib);

        iia.put(4, 4);
        
        assertNotEquals(iia.hashCode(), iib.hashCode());
        assertNotEquals(iia, iib);

        iia.put(1, 2);
        
        assertEquals(iia.hashCode(), iib.hashCode());
        assertEquals(iia, iib);

        iia.remove(1);
        
        assertNotEquals(iia.hashCode(), iib.hashCode());
        assertNotEquals(iia, iib);

    }
    
    public static void main(String[] args) {
        IntIntMap iim = new IntIntMap(10000);
        int sum = 0;
        for (int i = 0; i < 1000000; i++) {
            sum += i;
            iim.put(i, -i);
        }
        
        final int expect = sum;
        
        UBench bench = new UBench("parallelSum");
        bench.addIntTask("Seq", () -> iim.streamKeys().sum(), s -> s == expect);
        bench.addIntTask("Pll", () -> iim.streamKeys().parallel().sum(), s -> s == expect);
        
        bench.press(1000).report();
            
    }

}
