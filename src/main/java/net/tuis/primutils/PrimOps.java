package net.tuis.primutils;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Collection of static methods useful when performing operations in this
 * package.
 * <p>
 * <h2>Matrix Storage</h2>
 * One major collection of operators in here is the use of a 2-dimensional
 * linear storage system using arrays as a matrix. There are a number of methods
 * related to this system, and a broader description is needed.
 * <p>
 * Arrays are a convenient storage system for many data types. Unfortunately
 * they are static, and cannot be resized. You need to create a new array, and
 * copy the values across. For large arrays, there will be a time when there are
 * two copies of the array in memory, the old, and new copies. For large arrays
 * this is problematic, and extending the size is slow, and requires large
 * sequential areas in memory to be available.
 * <p>
 * Consider the need to store 1024 int values:
 * 
 * <pre>
 * int[] values = new int[1024];
 * </pre>
 * 
 * That would create storage space for all values (and being int, would be about
 * 4KB). Extending the system to have value 1025, would require a new array:
 * 
 * <pre>
 * values = Arrays.copyOf(values, 2048);
 * </pre>
 * 
 * A better solution is to have many smaller arrays, and daisy-chain them, and
 * create an abstracted storage sequence. Consider an int[][] matrix, where
 * 'rows' are added to the matrix as needed, to grow. If each row is size 256,
 * an initial matrix could be:
 * 
 * <pre>
 * int[][] matrix = new int[4][];
 * </pre>
 * 
 * The above would create 4 empty (null) rows. Then we could use the code:
 * 
 * <pre>
 * int row = getMatrixRow(index);
 * if (row &gt;= matrix.length) {
 *     matrix = Arrays.copyOf(matrix, extendSize(row);
 * }
 * if (matrix[row] == null) {
 *     matrix[row] = new int[KVEXTENT];
 * }
 * matrix[row][getMatrixColumn(index)] = value;
 * </pre>
 * 
 * The benefit of this is that the only array that grows is the container array,
 * and rows inside the matrix never grow. They are also small, and don't need
 * large spans of memory. You allocate memory progressively, and steadily,
 * rather than in large chunks.
 * 
 * 
 * @author rolf
 *
 */
public class PrimOps {

    /**
     * KVSHIFT, KVEXTENT, and KVMASK are used together when creating an
     * efficient 2D matrix to store values in based on indexes. Treat the matrix
     * as a collection of rows and columns. As your data grows, you add rows. To
     * store data at any location:
     * 
     * <pre>
     * int row = index &gt;&gt;&gt; KVSHIFT;
     * if (row &gt;= matrix.length) {
     *     matrix = Arrays.copyOf(matrix, extendSize(row);
     * }
     * if (matrix[row] == null) {
     *     matrix[row] = new SomeType[KVEXTENT];
     * }
     * matrix[row][index &amp; KVMASK] = value;
     * </pre>
     * 
     * Using this system allows data to grow in an efficient way, and with
     * limited memory overhead and garbage collection.
     * <p>
     * This IntKeyIndex, because of the way that it creates incrementing Index
     * values for key associations, allows you to use an efficient linear
     * storage system like the above to map arbitrary key values to linear
     * storage.
     */
    public static final int KVSHIFT = 8;
    /**
     * @see #KVSHIFT
     */
    public static final int KVEXTENT = 1 << KVSHIFT;
    /**
     * @see #KVSHIFT
     */
    public static final int KVMASK = KVEXTENT - 1;
    /**
     * Simple alias for Integer.MAX_VALUE;
     */
    public static final int MAX_SIZE = Integer.MAX_VALUE;

    /**
     * Return the number of rows required to contain a dataset of the given
     * size.
     * 
     * @param size
     *            the size of the data to contain.
     * @return the number of rows required to contain that size.
     */
    public static final int getRowsFor(final int size) {
        if (size <= 0) {
            return 0;
        }
        // for a size of that, the last item will be at index (size - 1).
        // what row would that last index be in?
        // we need 1 more than that.
        return 1 + ((size - 1) >> KVSHIFT);
    }

    /**
     * Assuming a matrix as described by {@link #KVSHIFT}, set the value at a
     * particular index, and return the previous value there.
     * 
     * @param matrix
     *            the matrix to set the value in.
     * @param index
     *            the location to set
     * @param value
     *            the new value to set
     * @return the previous value.
     */
    public static final int setValue(final int[][] matrix, final int index, final int value) {
        final int old = matrix[index >> KVSHIFT][index & KVMASK];
        matrix[index >> KVSHIFT][index & KVMASK] = value;
        return old;
    }

    /**
     * Assuming a matrix as described by {@link #KVSHIFT}, get the value at a
     * particular index.
     * 
     * @param matrix
     *            the matrix to get the value in.
     * @param index
     *            the location to get
     * @return the current value.
     */
    public static final int getValue(final int[][] matrix, final int index) {
        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException("No index " + index);
        }
        final int r = index >> KVSHIFT;
        if (r > matrix.length) {
            return 0;
        }
        final int[] row = matrix[r];
        return row == null ? null : row[index & KVMASK];
    }

    /**
     * Assuming a matrix as described by {@link #KVSHIFT}, set the value at a
     * particular index, and return the previous value there.
     * 
     * @param <T>
     *            The type of the value to set/retrieve
     * @param matrix
     *            the matrix to set the value in.
     * @param index
     *            the location to set
     * @param value
     *            the new value to set
     * @return the previous value.
     */
    public static final <T> T setValue(final T[][] matrix, final int index, final T value) {
        final T old = matrix[index >> KVSHIFT][index & KVMASK];
        matrix[index >> KVSHIFT][index & KVMASK] = value;
        return old;
    }

    /**
     * Assuming a matrix as described by {@link #KVSHIFT}, get the value at a
     * particular index.
     * 
     * @param <T>
     *            The type of the value to retrieve
     * @param matrix
     *            the matrix to get the value in.
     * @param index
     *            the location to get
     * @return the current value.
     */
    public static final <T> T getValue(final T[][] matrix, final int index) {
        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException("No index " + index);
        }
        final int r = index >> KVSHIFT;
        if (r > matrix.length) {
            return null;
        }
        final T[] row = matrix[r];
        return row == null ? null : row[index & KVMASK];
    }

    /**
     * Build a simple int[][] matrix as described by {@link #KVSHIFT}
     * 
     * @param rows
     *            the number of rows to prepare, only row 0 will be populated
     *            though
     * @return a matrix with the specified capacity, and with the first row
     *         prepared.
     */
    public static final int[][] buildIntMatrix(int rows) {
        int[][] data = new int[Math.max(1, rows)][];
        data[0] = buildIntRow();
        return data;
    }

    /**
     * Ensure that there is a location in the data matrix that will support the
     * supplied index.
     * 
     * @param matrix
     *            the matrix to check
     * @param index
     *            the index required.
     * @return a possibly new matrix which has been extended to support the
     *         index, or the old instance if it was large enough.
     */
    public static final int[][] ensure(int[][] matrix, final int index) {
        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException("No index " + index);
        }
        final int row = index >>> KVSHIFT;
        if (row >= matrix.length) {
            matrix = Arrays.copyOf(matrix, extendSize(row));
        }
        if (matrix[row] == null) {
            matrix[row] = buildIntRow();
        }
        return matrix;
    }

    /**
     * Create a single int[] row of the right length used in a value matrix as
     * described by {@link #KVSHIFT}
     * 
     * @return the row as created for the matrix.
     */
    public static final int[] buildIntRow() {
        return new int[KVEXTENT];
    }

    /**
     * Build a V[][] matrix as described by {@link #KVSHIFT}
     * 
     * @param rows
     *            the number of rows to prepare, only row 0 will be populated
     *            though
     * @param vlass
     *            a concrete instance of the class type that will be stored in
     *            the resulting matrix.
     * @param <V>
     *            the generic type of the contents of the array.
     * @return a matrix with the specified capacity, and with the first row
     *         prepared.
     */
    public static final <V> V[][] buildValueMatrix(Class<V> vlass, int rows) {
        V[] row = buildValueRow(vlass);
        @SuppressWarnings("unchecked")
        V[][] data = (V[][]) Array.newInstance(row.getClass(), Math.max(1, rows));
        data[0] = row;
        return data;
    }

    /**
     * Ensure that there is a location in the data matrix that will support the
     * supplied index.
     * 
     * @param <V>
     *            the generic type of the contents of the array.
     * @param vlass
     *            a concrete instance of the class type that will be stored in
     *            the resulting matrix.
     * @param matrix
     *            the matrix to check
     * @param index
     *            the index required.
     * @return a possibly new matrix which has been extended to support the
     *         index, or the old instance if it was large enough.
     */
    public static final <V> V[][] ensure(final Class<V> vlass, V[][] matrix, final int index) {
        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException("No index " + index);
        }
        final int row = index >>> KVSHIFT;
        if (row >= matrix.length) {
            matrix = Arrays.copyOf(matrix, extendSize(row));
        }
        if (matrix[row] == null) {
            matrix[row] = buildValueRow(vlass);
        }
        return matrix;
    }

    /**
     * Create a single row of the right length used in a value matrix as
     * described by {@link #KVSHIFT}
     * 
     * @param <V>
     *            the generic type of values in the matrix.
     * @param vlass
     *            the class used to create the appropriate array instances.
     * @return the row as created for the matrix.
     */
    public static final <V> V[] buildValueRow(Class<V> vlass) {
        return buildValueArray(vlass, KVEXTENT);
    }

    /**
     * Create a single dimension array of the specified length. This is a
     * generics-safe convenience method.
     * 
     * @param <V>
     *            the generic type of values in the array.
     * @param vlass
     *            the class used to create the appropriate array instances.
     * @param length
     *            the length of the array to create.
     * @return the row as created for the matrix.
     */
    public static final <V> V[] buildValueArray(Class<V> vlass, int length) {
        @SuppressWarnings("unchecked")
        V[] row = (V[]) Array.newInstance(vlass, length);
        return row;
    }

    /**
     * Identify which row an index would appear in a value matrix as described
     * by {@link #KVSHIFT}
     * 
     * @param index
     *            the index to get the row for.
     * @return the row in which that index would appear.
     */
    public static final int getMatrixRow(int index) {
        return index >>> KVSHIFT;
    }

    /**
     * Identify which column an index would appear in a value matrix as
     * described by {@link #KVSHIFT}
     * 
     * @param index
     *            the index to get the column for.
     * @return the column in which that index would appear.
     */
    public static final int getMatrixColumn(int index) {
        return index & KVMASK;
    }

    /**
     * A simple helper method that returns a new size to use for a growing
     * array. It checks for overflow conditions, and expands the size by
     * approximately 25% each time.
     * 
     * @param from
     *            the current size
     * @return the recommended new size, with a hard limit at @link
     *         {@link #MAX_SIZE}
     */
    public static final int extendSize(final int from) {
        int ns = from + (from >>> 2) + 1;
        if (ns < from || ns > MAX_SIZE) {
            // overflow conditions.
            ns = MAX_SIZE;
        }
        if (ns == from) {
            // unable to extend
            throw new IllegalStateException("Unable to have more than " + MAX_SIZE + " values in the Map");
        }
        return ns;
    }

    private PrimOps() {
        // inaccessible constructor.
    }

}
