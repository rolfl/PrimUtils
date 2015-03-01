package net.tuis.primutils;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Implement a tool that maps arbitrary int key values in the range
 * [Integer.MIN_VALUE ... Integer.MAX_VALUE] inclusive to sequential int index
 * values from 0.
 * <p>
 * Lightweight concrete implementation of @link {@link AbstractIntKeyIndex}. See
 * that class for documentation on memory structures.
 * 
 * @author rolf
 *
 */
public class IntKeyIndex extends AbstractIntKeyIndex {

    /**
     * Create a new instance with the anticipated initial capacity.
     * 
     * @param capacity
     *            the initial capacity to budget for. Capacity will grow, if
     *            needed.
     */
    public IntKeyIndex(int capacity) {
        super(capacity);
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
     * Determine whether there are any mappings in the Map
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
     * Identify whether an index is mapped to a key.
     * 
     * @param index
     *            the index to check the mapping for.
     * @return true if the key was previously mapped.
     */
    public boolean containsIndex(final int index) {
        return kiContainsIndex(index);
    }

    /**
     * Include a key in to the Map
     * 
     * @param key
     *            the key to add
     * @return the existing index associated with the key, or the new key in an
     *         insertion-point form (- key - 1)
     */
    public int add(final int key) {
        return kiAdd(key);
    }

    /**
     * Return the index associated with the specified key (if any).
     * 
     * @param key
     *            the key to get the value for.
     * @return the index associated with the key, or -1 if the key is not
     *         mapped.
     */
    public int getIndex(final int key) {
        return kiGetIndex(key);
    }

    /**
     * Return the key value that maps to the specified index, if any.
     * 
     * @param index
     *            The index to lookup
     * @param notThere
     *            the value to return if the index is not associated to a key.
     * @return the key mapping to this index, or notThere if the index is not
     *         associated. Use {@link #kiContainsIndex(int)} to check.
     */
    public int getKey(final int index, final int notThere) {
        return kiGetKey(index, notThere);
    }

    /**
     * Remove a key mapping from the map, if it exists.
     * 
     * @param key
     *            the key to remove
     * @return the index previously associated with the key, or -1 if the key is
     *         not mapped.
     */
    public int remove(final int key) {
        return kiRemove(key);
    }

    /**
     * Remove all key/value mappings from the Map. Capacity and other space
     * reservations will not be affected.
     */
    public void clear() {
        kiClear();
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
     * Get all indices that are mapped in this Map (the order of the indices is
     * not sequential).
     * <p>
     * There is a guarantee that the values represented in this array have a
     * 1-to-1 positional mapping to their respective keys returned from
     * {@link #kiGetKeys()} if no modifications to the map have been made
     * between the calls
     * 
     * @return all values in the map in the matching order as
     *         {@link #kiGetKeys()}
     */
    public int[] getIndices() {
        return kiGetIndices();
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
     * Stream all indices that are mapped in this Map.
     * <p>
     * There is a guarantee that the values represented in this array have a
     * 1-to-1 positional mapping to their respective keys returned from
     * {@link #kiStreamKeys()} if no modifications to the map have been made
     * between the calls
     * 
     * @return all values in the map in the matching order as
     *         {@link #kiGetKeys()}
     */
    public IntStream streamIndices() {
        return kiStreamIndices();
    }

    /**
     * Stream all entries in an Entry container.
     * @return a stream of all Key to Index mappings.
     */
    public Stream<IntKIntVEntry> streamEntries() {
        return kiStreamEntries();
    }

    
    @Override
    public int hashCode() {
        return Integer.rotateLeft(kiKeyHashCode(), 13) ^ kiIndexHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IntKeyIndex)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return kiEquals((AbstractIntKeyIndex) obj);
    }

    @Override
    public String toString() {
        return kiReport();
    }

}
