package net.tuis.primutils;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.tuis.primutils.PrimOps.*;

/**
 * Relate one int value to a Typed Object, in a memory efficient way.
 * <p>
 * 
 * @author rolf
 * 
 * @param <V> The generic type of values stored in this map.
 *
 */
public class IntMap<V> extends AbstractIntKeyIndex {
    
    private V[][] values;
    private final Class<V> klass;
    
    /**
     * Create the IntMap with the specified initial capacity.
     * @param klass an instance of the exact class of values that are to be stored in this map.
     * @param capacity the initial capacity to support.
     */
    public IntMap(Class<V> klass, int capacity) {
        super (capacity);
        this.klass = klass;
        values = buildValueMatrix(klass, 8);
    }
    
    /**
     * Get the number of key/value pairs that are stored in this Map
     * 
     * @return the Map size
     */
    public int size() {
        return kiSize();
    }

    /**
     * Determine whether there are any mappins in the Map
     * 
     * @return true if there are no mappings.
     */
    public boolean isEmpty() {
        return kiIsEmpty();
    }

    /**
     * Identify whether a key is mapped to a value.
     * 
     * @param key
     *            the key to check the mapping for.
     * @return true if the key was previously mapped.
     */
    public boolean containsKey(final int key) {
        return kiContainsKey(key);
    }

    /**
     * Include a key value pair in to the Map
     * 
     * @param key
     *            the key to add
     * @param value
     *            the associated value
     * @return the previous value associated with the key, or null
     *         if the key is not previously mapped.
     */
    public V put(final int key, final V value) {
        // assume it is NOT there already implicit -i-1
        final int index = - kiAdd(key) - 1;
        if (index < 0) {
            // we were wrong, reverse the assumption
            return setValue(values, - index - 1, value);
        }
        
        final int row = getMatrixRow(index);
        final int col = getMatrixColumn(index);
        if (row == values.length) {
            values = Arrays.copyOf(values, extendSize(values.length));
        }
        if (values[row] == null) {
            values[row] = buildValueRow(klass);
        }
        values[row][col] = value;
        return null;
        
    }

    /**
     * Return the value associated with the specified key (if any).
     * 
     * @param key
     *            the key to get the value for.
     * @return the value associated with the key, or null if the
     *         key is not mapped.
     */
    public V get(final int key) {
        final int pos = kiGetIndex(key);
        return pos < 0 ? null : getValue(values, pos);
    }

    /**
     * Remove a key mapping from the map, if it exists.
     * 
     * @param key
     *            the key to remove
     * @return the value previously associated with the key, or
     *         null if the key is not mapped.
     */
    public V remove(final int key) {
        int old = kiRemove(key);
        return old < 0 ? null : setValue(values, old, null);
    }

    /**
     * Remove all key/value mappings from the Map. Capacity and other space
     * reservations will not be affected.
     */
    public void clear() {
        kiClear();
        // GC values.
        Arrays.fill(values, null);
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
        return kiGetKeys();
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
    public V[] getValues() {
        return streamValues().collect(Collectors.toList()).toArray(buildValueArray(klass, 0));
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
        return kiStreamKeys();
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
    public Stream<V> streamValues() {
        return kiStreamIndices().mapToObj(i -> getValue(values, i));
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
    public void forEach(BiFunction<Integer,V, V> operator) {
        streamKeys().forEach(key -> applyOperator(key, operator));
    }

    private void applyOperator(final int key, final BiFunction<Integer,V, V> operator) {
        int index = kiGetIndex(key);
        setValue(values, index, operator.apply(key, getValue(values, index)));
    }

    @Override
    public int hashCode() {
        if (size() == 0) {
            return 0;
        }
        int vhc = streamValues().mapToInt(Objects::hashCode).reduce((x, p) -> x ^ p).getAsInt();
        int khc = kiKeyHashCode();
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
        return kiReport();
    }

    /* *****************************************************************
     * Support methods for implementing the public interface.
     * *****************************************************************
     */

    private boolean same(final IntIntMap them, final int key) {
        final V val = getValue(values, kiGetIndex(key));
        int t = them.get(key);
        if (!Objects.equals(t, val)) {
            return false;
        }
        if (t == them.getNotThere() && !them.containsKey(key)) {
            return false;
        }
        return true;
    }
    
}
