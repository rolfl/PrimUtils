package net.tuis.primutils;

import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

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
public class IntOps {

    /**
     * INCREMENT is a function that returns the input value, plus one.
     */
    public static final IntUnaryOperator INCREMENT = (i) -> i + 1;
    /**
     * DECREMENT is a function that returns the input value, minus one.
     */
    public static final IntUnaryOperator DECREMENT = (i) -> i - 1;
    
    /**
     * ISZERO is a simple zero-check.
     */
    public static final IntPredicate ISZERO = (i) -> i == 0;


    private IntOps() {
        // inaccessible constructor.
    }

}
