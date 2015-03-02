package net.tuis.primutils;

import java.util.function.IntBinaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Relate one int value to another, in a memory efficient way.
 * <p>
 * In addition to a simple int[] array for each of the keys, and values, there
 * is approximately 1 additional cell in an int array structure for each key.
 * <p>
 * The memory used by this class, then, is about 3 ints per entry * 4 bytes per
 * int, a 1000 member map will use 12000 bytes. Compare that with a
 * Map&lt;Integer,Integer&gt; which would consume about....100 bytes per entry.
 * <p>
 * Due to odd Java implementations, you cannot create arrays with as many as
 * Integer.MAX_VALUE entries, but this class, can support up to almost that
 * amount.
 * 
 * @author rolf
 *
 */
public final class IntIntMap {

    private final IntKeyIndex keyindex;
    private final IntArray values;

    private final int notThere;

    /**
     * Create an IntIntMap with the specified "Not There" value, and the
     * specified initial capacity.
     * 
     * @param notThere
     *            the value to return for gets and puts when the key does not
     *            exist in the Map
     * @param capacity
     *            the initial capacity to budget for.
     */
    public IntIntMap(final int notThere, final int capacity) {
        keyindex = new IntKeyIndex(capacity);
        this.notThere = notThere;
        values = new IntArray();
    }

    /**
     * Create an IntIntMap with a default initial capacity (128).
     * 
     * @param notThere
     *            The value to return when a get or put requests a value for a
     *            key that's not in the Map.
     */
    public IntIntMap(final int notThere) {
        this(notThere, 128);
    }

    /**
     * Each IntIntMap has a value that is returned when a key is not there. That
     * value is specified at construction time.
     * 
     * @return the NotThere value
     */
    public int getNotThere() {
        return notThere;
    }

    /**
     * Get the number of key/value pairs that are stored in this Map
     * 
     * @return the Map size
     */
    public int size() {
        return keyindex.size();
    }

    /**
     * Determine whether there are any mappins in the Map
     * 
     * @return true if there are no mappings.
     */
    public boolean isEmpty() {
        return keyindex.isEmpty();
    }

    /**
     * Identify whether a key is mapped to a value.
     * 
     * @param key
     *            the key to check the mapping for.
     * @return true if the key was previously mapped.
     */
    public boolean containsKey(final int key) {
        return keyindex.containsKey(key);
    }

    /**
     * Include a key value pair in to the Map
     * 
     * @param key
     *            the key to add
     * @param value
     *            the associated value
     * @return the previous value associated with the key, or {@link #getNotThere()}
     *         if the key is not previously mapped.
     */
    public int put(final int key, final int value) {
        // assume it is NOT there already implicit -i-1
        final int index = - keyindex.add(key) - 1;
        if (index < 0) {
            // we were wrong, reverse the assumption
            return values.set(- index - 1, value);
        }
        
        values.set(index, value);
        return notThere;
        
    }

    /**
     * Return the value associated with the specified key (if any).
     * 
     * @param key
     *            the key to get the value for.
     * @return the value associated with the key, or {@link #getNotThere()} if the
     *         key is not mapped.
     */
    public int get(final int key) {
        final int pos = keyindex.getIndex(key);
        return pos < 0 ? notThere : values.get(pos);
    }

    /**
     * Remove a key mapping from the map, if it exists.
     * 
     * @param key
     *            the key to remove
     * @return the value previously associated with the key, or
     *         {@link #getNotThere()} if the key is not mapped.
     */
    public int remove(final int key) {
        int old = keyindex.remove(key);
        return old < 0 ? notThere : values.get(old);
    }

    /**
     * Remove all key/value mappings from the Map. Capacity and other space
     * reservations will not be affected.
     */
    public void clear() {
        keyindex.clear();
    }

    /**
     * Get all the keys that are mapped in this Map.
     * <p>
     * There is no guarantee or specification about the order of the keys in the
     * results.
     * 
     * @return the mapped keys.
     */
    public int[] getKeys() {
        return keyindex.getKeys();
    }

    /**
     * Get all values that are mapped in this Map.
     * <p>
     * There is a guarantee that the values represented in this array have a
     * 1-to-1 positional mapping to their respective keys returned from
     * {@link #getKeys()} if no modifications to the map have been made between
     * the calls
     * 
     * @return all values in the map in the matching order as {@link #getKeys()}
     */
    public int[] getValues() {
        return streamValues().toArray();
    }

    /**
     * Stream all the keys that are mapped in this Map.
     * <p>
     * There is no guarantee or specification about the order of the keys in the
     * results.
     * 
     * @return the mapped keys.
     */
    public IntStream streamKeys() {
        return keyindex.streamKeys();
    }

    /**
     * Stream all values that are mapped in this Map.
     * <p>
     * There is a guarantee that the values represented in this array have a
     * 1-to-1 positional mapping to their respective keys returned from
     * {@link #streamKeys()} if no modifications to the map have been made
     * between the calls
     * 
     * @return all values in the map in the matching order as {@link #getKeys()}
     */
    public IntStream streamValues() {
        return keyindex.streamIndices().map(i -> values.get(i));
    }
    
    /**
     * Return all key/value mappings in this Map as a stream.
     * @return the stream of all mappings.
     */
    public Stream<IntKIntVEntry> streamEntries() {
        return keyindex.streamEntries().map(e -> new IntKIntVEntry(e.getKey(), values.get(e.getValue())));
    }

    /**
     * Apply the specified binary operator to each mapped key/value pair in the
     * Map, and save the resulting value back to the Map.
     * <p>
     * For example:
     * 
     * <pre>
     * myMap.forEach((k, v) -&gt; k * v);
     * </pre>
     * 
     * the above will replace each value in the Map with the product of the key
     * and value.
     * <p>
     * As a result, if you intend to just use the forEach method as a
     * convenience way to iterate through the associated values, you should
     * ensure that you return the supplied value:
     * 
     * <pre>
     * myMap.forEach((k, v) -&gt; {
     *     performOperation(k, v);
     *     return v;
     * });
     * </pre>
     * 
     * @param operator
     *            the operator to perform, and store back in to the Map
     */
    public void forEach(IntBinaryOperator operator) {
        streamKeys().forEach(key -> applyOperator(key, operator));
    }

    private void applyOperator(final int key, final IntBinaryOperator operator) {
        int index = keyindex.getIndex(key);
        values.preApply(index, v -> operator.applyAsInt(key, v));
    }

    @Override
    public int hashCode() {
        if (size() == 0) {
            return 0;
        }
        int vhc = streamValues().reduce((x, p) -> x ^ p).getAsInt();
        int khc = keyindex.getKeyHashCode();
        return Integer.rotateRight(khc, 13) ^ vhc;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IntIntMap)) {
            // includes null check.
            return false;
        }
        if (obj == this) {
            return false;
        }
        final IntIntMap them = (IntIntMap) obj;
        if (them.size() != size()) {
            return false;
        }
        return streamKeys().allMatch(k -> same(them, k));
    }

    @Override
    public String toString() {
        return keyindex.report();
    }

    /* *****************************************************************
     * Support methods for implementing the public interface.
     * *****************************************************************
     */

    private boolean same(final IntIntMap them, final int key) {
        final int val = values.get(keyindex.getIndex(key));
        int t = them.get(key);
        if (t != val) {
            return false;
        }
        if (t == them.getNotThere() && !them.containsKey(key)) {
            return false;
        }
        return true;
    }

}
