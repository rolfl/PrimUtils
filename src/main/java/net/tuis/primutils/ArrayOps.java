package net.tuis.primutils;

import java.lang.reflect.Array;

/**
 * Common dynamic array tools used by various classes in this package.
 * @author rolf
 *
 */
public class ArrayOps {


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
        if (from <= 0) {
            // some small number.
            return 8;
        }
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
    
    /**
     * Build a standard-width matrix of the specified type and row count initial capacity.
     * 
     * @param <T> the generic type of the values to be stored in the matrix.
     * @param vlass the class instance used for creating the correctly typed arrays
     * @param rows the number of rows to seed the matrix with.
     * @return the seeded matrix.
     */
    public static final <T> T[][] buildMatrix(Class<T> vlass, int rows) {
        T[] row = buildRow(vlass);
        @SuppressWarnings("unchecked")
        T[][] matrix = (T[][])Array.newInstance(row.getClass(), rows);
        matrix[0] = row;
        return matrix;
    }

    
    /**
     * Build a single row of the standard size for a matrix.
     * 
     * @param <T> the generic type of the values.
     * @param tlass the class instance to build the array for. 
     * @return a standard-sized array of the specified type.
     */
    public static final <T> T[] buildRow(Class<T> tlass) {
        return ArrayOps.buildArray(tlass, KVEXTENT);
    }

    
    
    /**
     * Build an array of the specified type, and size.
     * @param <T> the generic type of the array type.
     * @param vlass the class representing the generic type.
     * @param size the size array to create.
     * @return the newly created array.
     */
    public static <T> T[] buildArray(Class<T> vlass, int size) {
        @SuppressWarnings("unchecked")
        T[] array = (T[])Array.newInstance(vlass, size);
        return array;
    }


}
