package net.tuis.primutils;

import static net.tuis.primutils.ArrayOps.KVMASK;
import static net.tuis.primutils.ArrayOps.extendSize;
import static net.tuis.primutils.ArrayOps.getMatrixColumn;
import static net.tuis.primutils.ArrayOps.getMatrixRow;
import static net.tuis.primutils.ArrayOps.getRowsFor;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A dynamic primitive int array that expands as needed.
 * <p>
 * In order to support setting the value at index Integer.MAX_VALUE (which would
 * require an array of size Integer.MAX_VALUE + 1), and because this is dynamic,
 * there is no concept of size, only a concept of the "High Water Mark", the
 * largest index that has been accessed in the array.
 * <p>
 * Memory for int values is allocated in a relatively sparse way. There is a
 * cost associated for having a large range in set value indexes.
 * <code>set(Integer.MAX_VALUE,1)</code> will result in allocating an array of
 * size 8 million or so, or, 64MB. Since the array space is allocated in
 * 'chunks', a worst case distribution of set values (1 value set for every 256
 * indexes), would require the full 8GB or so).
 * <p>
 * On the other hand, for small ranges of indexes, the array is very compact.
 * 
 * @author rolf
 * @param <V> The generic type of data stored in this VArray
 *
 */
public class VArray<V> {

    private V[][] data;
    private final Class<V> vlass;
    private int hwm = -1; // high water mark

    /**
     * Create a dynamic array of int values with preinitialized capacity.
     * 
     * @param vlass
     *            the generic type of data stored in the VArray 
     * @param capacity
     *            the initial capacity to budget for.
     */
    public VArray(final Class<V> vlass, int capacity) {
        this.vlass = vlass;
        data = ArrayOps.buildMatrix(vlass, getRowsFor(capacity));
    }

    /**
     * Create a dynamic array of int values with default capacity.
     * 
     * @param vlass
     *            the generic type of data stored in the VArray 
     */
    public VArray(final Class<V> vlass) {
        this(vlass, 1);
    }

    /**
     * Clear the array, reset the high water mark, and reinitialize all values.
     */
    public void clear() {
        hwm = -1;
        Arrays.fill(data, null);
    }

    /**
     * Stream all values in this array to the high-water-mark.
     * 
     * @return an IntStream which accesses, in order, all values.
     */
    public Stream<V> stream() {
        return IntStream.rangeClosed(0, hwm).mapToObj(index -> getValue(index));
    }

    /**
     * Identify the high-water-mark for this array, which is the largest index
     * accessed
     * 
     * @return the high water mark.
     */
    public int getHighWaterMark() {
        return hwm;
    }

    private void accessed(int index) {
        if (index > hwm) {
            hwm = index;
        }
    }

    /**
     * Set the value at a particular index, to a particular value.
     * 
     * @param index
     *            the index to set
     * @param value
     *            the value to set at that index.
     * @return the previous value at that index. Note that all values are
     *         initialized to 0.
     */
    public V set(int index, V value) {
        accessed(index);
        return setValue(index, value);
    }

    /**
     * Get the value at a particular index.
     * 
     * @param index
     *            the index to get the value of.
     * @return the value at that index.
     */
    public V get(int index) {
        accessed(index);
        return getValue(index);
    }

    /**
     * Retrieve the value at the given index, and then apply the supplied
     * operation to that value.
     * 
     * @param index
     *            the index of the value to operate on.
     * @param op
     *            the operation to perform.
     * @return the value before the operation was performed.
     */
    public V postApply(int index, UnaryOperator<V> op) {
        accessed(index);
        final int row = getMatrixRow(index);
        final int col = getMatrixColumn(index);
        ensureRow(row);
        V[] line = data[row];
        final V rv = line[col];
        line[col] = op.apply(line[col]);
        return rv;
    }

    /**
     * Apply the supplied operation to the value at the given index, and then
     * return the result.
     * 
     * @param index
     *            the index of the value to operate on.
     * @param op
     *            the operation to perform.
     * @return the value after the operation was performed.
     */
    public V preApply(int index, UnaryOperator<V> op) {
        accessed(index);
        final int row = getMatrixRow(index);
        final int col = getMatrixColumn(index);
        ensureRow(row);
        V[] line = data[row];
        line[col] = op.apply(line[col]);
        return line[col];
    }

    private final V setValue(final int index, final V value) {
        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException("No index " + index);
        }
        final int row = getMatrixRow(index);
        final int col = getMatrixColumn(index);
        ensureRow(row);
        final V old = data[row][col];
        data[row][col] = value;
        return old;
    }

    private final V getValue(final int index) {
        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException("No index " + index);
        }
        final int r = getMatrixRow(index);
        if (r >= data.length) {
            return null;
        }
        final V[] row = data[r];
        return row == null ? null : row[index & KVMASK];
    }

    private final void ensureRow(final int row) {
        if (row >= data.length) {
            data = Arrays.copyOf(data, extendSize(row));
        }
        if (data[row] == null) {
            data[row] = ArrayOps.buildRow(vlass);
        }
    }

    @Override
    public String toString() {
        return String.format("IntArray(hwm: %d, alloc: %d)", hwm,
                Stream.of(data).mapToInt(row -> row == null ? 0 : row.length).sum());
    }

    @Override
    public int hashCode() {
        // because of the convenient row lengths, we can do:
        int hash = 0;
        for (final V[] row : data) {
            if (row == null) {
                continue;
            }
            int rh = 0;
            for (final V v : row) {
                Integer.rotateLeft(rh, 13);
                rh ^= Objects.hash(v);
            }
            hash ^= rh;
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof VArray)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        final VArray<?> them = (VArray<?>) obj;
        if (them.hwm != this.hwm) {
            return false;
        }
        final int limit = getMatrixRow(hwm);
        for (int r = 0; r <= limit; r++) {
            final Object[] a = r < this.data.length ? this.data[r] : null;
            final Object[] b = r < them.data.length ? them.data[r] : null;
            if (a == null && b == null) {
                continue;
            }
            if (a == null && !Stream.of(b).allMatch(d -> d != null)) {
                return false;
            }
            if (b == null && !Stream.of(a).allMatch(d -> d != null)) {
                return false;
            }
            if (!Arrays.equals(a, b)) {
                return false;
            }
        }
        return true;
    }

}
