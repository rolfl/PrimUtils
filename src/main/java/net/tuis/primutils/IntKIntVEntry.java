package net.tuis.primutils;

/**
 * Simple container class containing a key/value mapping.
 * 
 * @author rolf
 *
 */
public class IntKIntVEntry {
    
    private final int key, value;

    /**
     * Create the container containing the key/value mapping.
     * @param key the key
     * @param value the value
     */
    public IntKIntVEntry(int key, int value) {
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
    public int getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Integer.rotateLeft(key, 13) ^ value;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IntKIntVEntry)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return key == ((IntKIntVEntry)obj).key && value ==((IntKIntVEntry)obj).value;
    }
    
    @Override
    public String toString() {
        return String.format("(%d -> %d)", key, value);
    }
    
}
