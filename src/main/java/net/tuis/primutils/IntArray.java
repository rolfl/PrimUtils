package net.tuis.primutils;

import static net.tuis.primutils.ArrayOps.KVEXTENT;
import static net.tuis.primutils.ArrayOps.KVMASK;
import static net.tuis.primutils.ArrayOps.extendSize;
import static net.tuis.primutils.ArrayOps.getMatrixColumn;
import static net.tuis.primutils.ArrayOps.getMatrixRow;
import static net.tuis.primutils.ArrayOps.getRowsFor;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A dynamic primitive int array that expands as needed.
 * 
 * @author rolf
 *
 */
public class IntArray {
    
    private int[][] data;
    private int hwm = -1; // high water mark

    /**
     * Create a dynamic array of int values with preinitialized capacity.
     * @param capacity the initial capacity to budget for.
     */
    public IntArray(int capacity) {
        data = buildIntMatrix(getRowsFor(capacity));
    }
    
    /**
     * Create a dynamic array of int values with default capacity.
     */
    public IntArray() {
        this(1);
    }
    
    /**
     * Stream all values in this array to the high-water-mark.
     * @return an IntStream which accesses, in order, all values.
     */
    public IntStream stream() {
        return IntStream.rangeClosed(0, hwm).map(index -> getValue(index));
    }
    
    /**
     * Identify the high-water-mark for this array, which is the largest index accessed
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
     * @param index the index to set
     * @param value the value to set at that index.
     * @return the previous value at that index. Note that all values are initialized to 0.
     */
    public int set(int index, int value) {
        accessed(index);
        return setValue(index, value);
    }
    
    /**
     * Get the value at a particular index.
     * @param index the index to get the value of.
     * @return the value at that index.
     */
    public int get(int index) {
        accessed(index);
        return getValue(index);
    }
    
    /**
     * Retrieve the value at the given index, and then apply the supplied operation to that value.
     * @param index the index of the value to operate on.
     * @param op the operation to perform.
     * @return the value before the operation was performed.
     */
    public int postApply(int index, IntUnaryOperator op) {
        accessed(index);
        final int row = getMatrixRow(index);
        final int col = getMatrixColumn(index);
        ensureRow(row);
        int[] line = data[row];
        final int rv = line[col];
        line[col] = op.applyAsInt(line[col]);
        return rv;
    }
    
    /**
     * Apply the supplied operation to the value at the given index, and then return the result.
     * @param index the index of the value to operate on.
     * @param op the operation to perform.
     * @return the value after the operation was performed.
     */
    public int preApply(int index, IntUnaryOperator op) {
        accessed(index);
        final int row = getMatrixRow(index);
        final int col = getMatrixColumn(index);
        ensureRow(row);
        int[] line = data[row];
        line[col] = op.applyAsInt(line[col]);
        return line[col];
    }
    
    /**
     * Increment the value at the given index, and return the value as it was before the increment
     * @param index the index of the value to increment.
     * @return the previous value.
     */
    public int postIncrement(int index) {
        return postApply(index, IntOps.INCREMENT);
    }
    
    /**
     * Increment the value at the given index, and return the value as it is after the increment
     * @param index the index of the value to increment.
     * @return the incremented value.
     */
    public int preIncrement(int index) {
        return preApply(index, IntOps.INCREMENT);
    }

    /**
     * Decrement the value at the given index, and return the value as it was before the decrement
     * @param index the index of the value to decrement.
     * @return the previous value.
     */
    public int postDecrement(int index) {
        return postApply(index, IntOps.DECREMENT);
    }
    
    /**
     * Decrement the value at the given index, and return the value as it is after the decrement
     * @param index the index of the value to decrement.
     * @return the decremented value.
     */
    public int preDecrement(int index) {
        return preApply(index, IntOps.DECREMENT);
    }

    private final int setValue(final int index, final int value) {
        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException("No index " + index);
        }
        final int row = getMatrixRow(index);
        final int col = getMatrixColumn(index);
        ensureRow(row);
        final int old = data[row][col];
        data[row][col] = value;
        return old;
    }

    private final int getValue(final int index) {
        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException("No index " + index);
        }
        final int r = getMatrixRow(index);
        if (r >= data.length) {
            return 0;
        }
        final int[] row = data[r];
        return row == null ? 0 : row[index & KVMASK];
    }

    private final void ensureRow(final int row) {
        if (row >= data.length) {
            data = Arrays.copyOf(data, extendSize(row));
        }
        if (data[row] == null) {
            data[row] = buildIntRow();
        }
    }

    private static final int[][] buildIntMatrix(int rows) {
        return new int[Math.max(1, rows)][];
    }

    private static final int[] buildIntRow() {
        return new int[KVEXTENT];
    }
    
    @Override
    public String toString() {
        return String.format("IntArray(hwm: %d, alloc: %d)", hwm, Stream.of(data).mapToInt(row -> row == null ? 0 : row.length).sum());
    }
    
    @Override
    public int hashCode() {
        // because of the convenient row lengths, we can do:
        int hash = 0;
        for (final int[] row : data) {
            if (row == null) {
                continue;
            }
            int rh = 0;
            for (final int v : row) {
                Integer.rotateLeft(rh, 13);
                rh ^= v;
            }
            hash ^= rh;
        }
        return hash;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IntArray)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        final IntArray them = (IntArray)obj;
        if (them.hwm != this.hwm) {
            return false;
        }
        final int limit = getMatrixRow(hwm);
        for (int r = 0; r <= limit; r++) {
            final int[] a = r < this.data.length ? this.data[r] : null;
            final int[] b = r < them.data.length ? them.data[r] : null;
            if (a == null && b == null) {
                continue;
            }
            if (a == null && !IntStream.of(b).allMatch(IntOps.ISZERO)) {
                return false;
            }
            if (b == null && !IntStream.of(a).allMatch(IntOps.ISZERO)) {
                return false;
            }
            if (!Arrays.equals(a, b)) {
                return false;
            }
        }
        return true;
    }
    
}
