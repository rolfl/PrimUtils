package net.tuis.primutils;

import static net.tuis.primutils.ArrayOps.KVEXTENT;
import static net.tuis.primutils.ArrayOps.KVMASK;
import static net.tuis.primutils.ArrayOps.extendSize;
import static net.tuis.primutils.ArrayOps.getMatrixColumn;
import static net.tuis.primutils.ArrayOps.getMatrixRow;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A dynamic primitive int array that expands as needed.
 * <p>
 * In order to support setting the value at index Integer.MAX_VALUE (which would
 * require an array of size Integer.MAX_VALUE + 1), and because this is dynamic,
 * there is no concept of size, only a concept of the "Bound", the largest index
 * that can be accessed in the array. The bound can be relocated by calling the
 * {@link #setBound(int)} method, or it can be shifted using the
 * {@link #push(int)} and {@link #pop()} methods. Note that the bound is always
 * "inclusive", so it is possible for the bound to be Integer.MAX_VALUE.
 * Additionally, when there is just 1 item in the IntArray, it is at index 0, so
 * the bound for a 1-sized IntArray is 0. It follows that the bound for an empty
 * IntArray is -1.
 * <p>
 * Memory for int values is allocated in a relatively sparse way. There is a
 * cost associated for having a large range in set value indexes.
 * <code>setBound(Integer.MAX_VALUE)</code> will result in allocating an array
 * of size 8 million or so, or, 64MB. Since the array space is allocated in
 * 'chunks', a worst case distribution of set values (1 value set for every 256
 * indexes), would require the full 8GB or so).
 * <p>
 * On the other hand, for small ranges of indexes, the array is very compact.
 * 
 * @author rolf
 *
 */
public class IntArray {

    private int[][] data;
    private int bound;

    /**
     * Create a dynamic array of int values with the specified initial bound
     * 
     * @param bound
     *            The initial bound to allow in the instance.
     * 
     */
    public IntArray(int bound) {
        if (bound < -1) {
            throw new IllegalArgumentException("Illegal bound " + bound);
        }
        this.bound = bound;
        data = new int[1 + getMatrixRow(Math.max(bound, 1024))][];
    }

    /**
     * Create an empty, but dynamic array of int values.
     */
    public IntArray() {
        this(-1);
    }

    /**
     * Clear the array, reset the high water mark, and reinitialize all values.
     * This is logically the same as <code>setBound(-1)</code>
     */
    public void clear() {
        bound = -1;
        Arrays.fill(data, null);
    }

    /**
     * Identify whether this IntArray has any values. All IntArrays are
     * initialized as empty, and can be returned to the empty state with a call
     * to {@link #clear()}
     * 
     * @return true if this instance has no data.
     */
    public boolean isEmpty() {
        return bound == -1;
    }

    /**
     * Identify whether this IntArray has reached its maximum capacity. Adding
     * additional items will fail with AArrayIndexOutOfBounds exception if an
     * attempt is made to add to a full IntArray
     * 
     * @return true if this instance can no longer be added to.
     */
    public boolean isFull() {
        return bound == Integer.MAX_VALUE;
    }

    /**
     * Stream all values in this array to the high-water-mark.
     * 
     * @return an IntStream which accesses, in order, all values.
     */
    public IntStream stream() {
        return IntStream.rangeClosed(0, bound).map(index -> getValue(index));
    }

    /**
     * Get the value of the largest index that is allowed in get/set operations
     * without throwing an IndexOutOfBounds exception. The bound may be -1 for
     * an empty IntArray because you cannot get, or set any positions since there
     * are none.
     * 
     * The low bound of the IntArray is always 0.
     * 
     * @return the high bound index of this IntArray.
     */
    public int getBound() {
        return bound;
    }

    /**
     * Expand the boundary to include the specified index, or, if the current
     * bound is larger, truncate the array to include nothing more than the
     * specified index.
     * 
     * @param index
     *            the index to include in the new bounds. -1 is allowed and it
     *            means the IntArray will be empty.
     */
    public void setBound(int index) {
        if (index < -1) {
            throw new IllegalArgumentException("Bound cannot be " + index);
        }
        if (index < bound) {
            // truncation. Need to clear out data.
            int row = getMatrixRow(index);
            int from = getMatrixColumn(index) + 1;
            if (data[row] != null && from < data[row].length) {
                Arrays.fill(data[row], from, data[row].length, 0);
            }
            for (int r = row + 1; r < data.length; r++) {
                data[r] = null;
            }
        }
        bound = index;
    }

    /**
     * Set the value at a particular index, to a specified value. The index must
     * be within the dynamic bounds.
     * 
     * @param index
     *            the index to set
     * @param value
     *            the value to set at that index.
     * @return the previous value at that index. Note that all values are
     *         initialized to 0.
     * @throws ArrayIndexOutOfBoundsException
     *             if the index is larger than {@link #getBound()} or less than
     *             0;
     */
    public int set(int index, int value) {
        checkBound(index);
        return setValue(index, value);
    }

    /**
     * Get the value at a particular index. The index must be within the dynamic
     * bounds.
     * 
     * @param index
     *            the index to get the value of.
     * @return the value at that index.
     * @throws ArrayIndexOutOfBoundsException
     *             if the index is larger than {@link #getBound()} or less than
     *             0;
     */
    public int get(int index) {
        checkBound(index);
        return getValue(index);
    }

    /**
     * Increase the current bound of the IntArray, and store the specified value
     * at the new bound index
     * 
     * @param value
     *            the value to add.
     * @return the index the value was added at (the new bound)
     * @throws ArrayIndexOutOfBoundsException
     *             if the IntArray was full.
     */
    public int push(int value) {
        if (isFull()) {
            throw new ArrayIndexOutOfBoundsException("Cannot push() to a full IntArray");
        }
        bound++;
        setValue(bound, value);
        return bound;
    }

    /**
     * Reduce the current bound of the IntArray, and return the value at the
     * previous bound index
     * 
     * @return the value is newly out of bound
     * @throws ArrayIndexOutOfBoundsException
     *             if the IntArray was empty.
     */
    public int pop() {
        if (isEmpty()) {
            throw new ArrayIndexOutOfBoundsException("Cannot pop() from an empty IntArray");
        }
        return setValue(bound--, 0);
    }

    /**
     * Locate the value at the given index, and then apply the supplied
     * operation to that value, storing the result back at the index.
     * 
     * @param index
     *            the index of the value to operate on.
     * @param op
     *            the operation to perform.
     */
    public void apply(int index, IntUnaryOperator op) {
        checkBound(index);
        final int col = getMatrixColumn(index);
        final int[] line = ensureRow(getMatrixRow(index));
        line[col] = op.applyAsInt(line[col]);
    }

    /**
     * Locate the value at the given index, and then apply the supplied
     * operation to that value, storing the result back at the index.
     * 
     * @param index
     *            the index of the value to operate on.
     * @param op
     *            the operation to perform.
     * @return the value before the operation was performed.
     */
    public int postApply(int index, IntUnaryOperator op) {
        checkBound(index);
        final int col = getMatrixColumn(index);
        final int[] line = ensureRow(getMatrixRow(index));
        final int rv = line[col];
        line[col] = op.applyAsInt(line[col]);
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
    public int preApply(int index, IntUnaryOperator op) {
        checkBound(index);
        final int col = getMatrixColumn(index);
        final int[] line = ensureRow(getMatrixRow(index));
        line[col] = op.applyAsInt(line[col]);
        return line[col];
    }

    /**
     * Increment the value at the given index, and return the value as it was
     * before the increment
     * 
     * @param index
     *            the index of the value to increment.
     * @return the previous value.
     */
    public int postIncrement(int index) {
        return postApply(index, IntOps.INCREMENT);
    }

    /**
     * Increment the value at the given index, and return the value as it is
     * after the increment
     * 
     * @param index
     *            the index of the value to increment.
     * @return the incremented value.
     */
    public int preIncrement(int index) {
        return preApply(index, IntOps.INCREMENT);
    }

    /**
     * Decrement the value at the given index, and return the value as it was
     * before the decrement
     * 
     * @param index
     *            the index of the value to decrement.
     * @return the previous value.
     */
    public int postDecrement(int index) {
        return postApply(index, IntOps.DECREMENT);
    }

    /**
     * Decrement the value at the given index, and return the value as it is
     * after the decrement
     * 
     * @param index
     *            the index of the value to decrement.
     * @return the decremented value.
     */
    public int preDecrement(int index) {
        return preApply(index, IntOps.DECREMENT);
    }

    private final int setValue(final int index, final int value) {
        final int col = getMatrixColumn(index);
        final int[] line = ensureRow(getMatrixRow(index));
        final int old = line[col];
        line[col] = value;
        return old;
    }

    private final int getValue(final int index) {
        final int r = getMatrixRow(index);
        if (r >= data.length) {
            return 0;
        }
        final int[] row = data[r];
        return row == null ? 0 : row[index & KVMASK];
    }

    private final int[] ensureRow(final int row) {
        if (row >= data.length) {
            data = Arrays.copyOf(data, extendSize(row));
        }
        if (data[row] == null) {
            data[row] = buildIntRow();
        }
        return data[row];
    }

    private static final int[] buildIntRow() {
        return new int[KVEXTENT];
    }

    @Override
    public String toString() {
        return String.format("IntArray(bound: %d, space: %d)", bound,
                Stream.of(data).mapToInt(row -> row == null ? 0 : row.length).sum());
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
        final IntArray them = (IntArray) obj;
        if (them.bound != this.bound) {
            return false;
        }
        final int limit = getMatrixRow(bound);
        for (int r = 0; r <= limit; r++) {
            final int[] a = r < this.data.length ? this.data[r] : null;
            final int[] b = r < them.data.length ? them.data[r] : null;
            if (!rowsEqual(a, b)) {
                return false;
            }
        }
        return true;
    }
    
    private final boolean rowsEqual(int[] a, int[] b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null && !IntStream.of(b).allMatch(IntOps.ISZERO)) {
            return false;
        }
        if (b == null && !IntStream.of(a).allMatch(IntOps.ISZERO)) {
            return false;
        }
        return Arrays.equals(a, b);
    }

    private void checkBound(int index) {
        if (index < 0 || index > bound) {
            throw new ArrayIndexOutOfBoundsException(String.format("Illegal index %d in IntArray with bound %d", index,
                    bound));
        }
    }

}
