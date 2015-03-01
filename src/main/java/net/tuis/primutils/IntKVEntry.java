package net.tuis.primutils;

import java.util.Objects;

/**
 * Simple container class containing a key/value mapping.
 * 
 * @author rolf
 * @param <V> The generic type of values stored in this entry.
 *
 */
public class IntKVEntry<V> {
    
    private final int key;
    private final V value;

    /**
     * Create the container containing the key/value mapping.
     * @param key the key
     * @param value the value
     */
    public IntKVEntry(int key, V value) {
        super();
        this.key = key;
        this.value = value;
    }

    /**
     * Retrieve the mapped key
     * @return the key
     */
    public int getKey() {
        return key;
    }

    /**
     * Retrieve the value.
     * @return the value.
     */
    public V getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Integer.rotateLeft(key, 13) ^ Objects.hashCode(value);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IntKVEntry)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return key == ((IntKVEntry<?>)obj).key && Objects.equals(value, ((IntKVEntry<?>)obj).value);
    }
    
    @Override
    public String toString() {
        return String.format("(%d -> %d)", key, value);
    }
    
}
