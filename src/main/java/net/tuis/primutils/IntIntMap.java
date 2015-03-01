package net.tuis.primutils;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Relate one int value to another, in a memory efficient way.
 * <p>
 * In addition to a simple int[] array for each of the keys, and values, there
 * is approximately 1 additional cell in an int array structure for each key.
 * <p>
 * The memory used by this class, then, is about 3 ints per entry * 4 bytes per
 * int, a 1000 member map will use 12000 bytes. Compare that with a
 * Map<Integer,Integer> which would consume about....100 bytes per entry.
 * <p>
 * Due to odd Java implementations, you cannot create arrays with as many as
 * Integer.MAX_VALUE entries, but this class, can support up to almost that
 * amount.
 * 
 * @author rolf
 *
 */
public class IntIntMap {

    private static final int IDEAL_BUCKET_SIZE = 64;
    private static final int INITIAL_BUCKET_SIZE = 8;
    private static final int MAX_SIZE = Integer.MAX_VALUE;
    private static final int KVSHIFT = 10;
    private static final int KVEXTENT = 1 << KVSHIFT;
    private static final int KVMASK = KVEXTENT - 1;

    private int[][] bucketData;
    private int[] bucketSize;
    private int size;
    private int mask;
    private int[] deletedIndices = new int[10];
    private int deletedCount = 0;
    private int modCount = 0;

    private int[][] keys;
    private int[][] values;

    private final int notThere;

    /**
     * Create an IntIntMap with the specified "Not There" value, and the
     * specified initial capacity.
     * 
     * @param notThere
     *            the value to return for gets and puts when the key does not
     *            exist in the Map
     * @param capacity
     *            the initial capacity to budget for.
     */
    public IntIntMap(final int notThere, final int capacity) {
        this.notThere = notThere;

        int nxtp2 = nextPowerOf2(capacity / IDEAL_BUCKET_SIZE);
        int bCount = Math.max(IDEAL_BUCKET_SIZE, nxtp2);
        bucketData = new int[bCount][];
        bucketSize = new int[bCount];
        mask = bCount - 1;
        keys = new int[bCount][];
        values = new int[bCount][];
    }

    /**
     * Create an IntIntMap with a default initial capacity (128).
     * 
     * @param notThere
     *            The value to return when a get or put requests a value for a
     *            key that's not in the Map.
     */
    public IntIntMap(final int notThere) {
        this(notThere, 128);
    }

    /**
     * Each IntIntMap has a value that is returned when a key is not there. That
     * value is specified at construction time.
     * 
     * @return the NotThere value
     */
    public int getNotThere() {
        return notThere;
    }

    /**
     * Get the number of key/value pairs that are stored in this Map
     * 
     * @return the Map size
     */
    public int size() {
        return size - deletedCount;
    }

    /**
     * Determine whether there are any mappins in the Map
     * 
     * @return true if there are no mappings.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Identify whether a key is mapped to a value.
     * 
     * @param key
     *            the key to check the mapping for.
     * @return true if the key was previously mapped.
     */
    public boolean containsKey(final int key) {
        final int bucket = bucketId(key);
        final int pos = locate(bucketData[bucket], bucketSize[bucket], key);
        return pos >= 0;
    }

    /**
     * Include a key value pair in to the Map
     * 
     * @param key
     *            the key to add
     * @param value
     *            the associated value
     * @return the previous value associated with the key, or {@link #notThere}
     *         if the key is not previously mapped.
     */
    public int put(final int key, final int value) {
        final int bucket = bucketId(key);
        final int bucketPos = locate(bucketData[bucket], bucketSize[bucket], key);
        if (bucketPos >= 0) {
            return setValue(values, bucketData[bucket][bucketPos], value);
        }
        // only changes to the actual key values make a difference on the
        // iteration.
        // addKeyValue is the only place where max size is actually checked.
        int keyIndex = addKeyValue(key, value);
        modCount++;
        insertBucketIndex(bucket, -bucketPos - 1, keyIndex);
        return notThere;
    }

    /**
     * Return the value associated with the specified key (if any).
     * 
     * @param key
     *            the key to get the value for.
     * @return the value associated with the key, or {@link #notThere} if the
     *         key is not mapped.
     */
    public int get(final int key) {
        final int bucket = bucketId(key);
        final int pos = locate(bucketData[bucket], bucketSize[bucket], key);
        return pos < 0 ? notThere : getValue(values, bucketData[bucket][pos]);
    }

    /**
     * Remove a key mapping from the map, if it exists.
     * 
     * @param key
     *            the key to remove
     * @return the value previously associated with the key, or
     *         {@link #notThere} if the key is not mapped.
     */
    public int remove(final int key) {
        final int bucket = bucketId(key);
        final int pos = locate(bucketData[bucket], bucketSize[bucket], key);
        if (pos < 0) {
            return notThere;
        }
        // only changes to the actual key values make a difference on the
        // iteration.
        modCount++;
        final int index = bucketData[bucket][pos];
        deleteIndex(index);
        bucketSize[bucket]--;
        System.arraycopy(bucketData[bucket], pos + 1, bucketData[bucket], pos, bucketSize[bucket] - pos);
        return getValue(values, index);
    }

    /**
     * Remove all key/value mappings from the Map. Capacity and other space
     * reservations will not be affected.
     */
    public void clear() {
        if (size == 0) {
            return;
        }
        modCount++;
        Arrays.fill(bucketSize, 0);
        size = 0;
        deletedCount = 0;
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
        return streamKeys().toArray();
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
    public int[] getValues() {
        return streamValues().toArray();
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
        return liveIndices().map(i -> getValue(keys, i));
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
    public IntStream streamValues() {
        return liveIndices().map(i -> getValue(values, i));
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
    public void forEach(IntBinaryOperator operator) {
        liveIndices().forEach(
                index -> setValue(values, index, operator.applyAsInt(getValue(keys, index), getValue(values, index))));
    }

    @Override
    public int hashCode() {
        return size() == 0 ? 0 : liveIndices().map(i -> hashPair(i)).reduce((x, p) -> x ^ p).getAsInt();
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
        return liveIndices().allMatch(i -> same(them, i));
    }

    @Override
    public String toString() {
        long allocated = Stream.of(bucketData).filter(b -> b != null).mapToLong(b -> b.length).sum();
        long max = IntStream.of(bucketSize).max().getAsInt();
        long vals = Stream.of(keys).filter(vs -> vs != null).count() * KVEXTENT;
        return String.format("IntIntMap size %s (used %d, deleted %d) buckets %d hashspace %d longest %d valspace %d",
                size(), size, deletedCount, bucketSize.length, allocated, max, vals);
    }

    /* *****************************************************************
     * Support methods for implementing the public interface.
     * *****************************************************************
     */

    private int setValue(final int[][] matrix, final int index, final int value) {
        final int old = matrix[index >> KVSHIFT][index & KVMASK];
        matrix[index >> KVSHIFT][index & KVMASK] = value;
        return old;
    }

    private int getValue(final int[][] matrix, final int index) {
        return matrix[index >> KVSHIFT][index & KVMASK];
    }

    private int hashPair(final int index) {
        final int k = getValue(keys, index);
        final int v = getValue(values, index);
        return Integer.rotateLeft(k, 13) ^ v;
    }

    private boolean same(final IntIntMap them, final int index) {
        final int k = getValue(keys, index);
        int t = them.get(k);
        if (t != getValue(values, index)) {
            return false;
        }
        if (t == them.getNotThere() && !them.containsKey(k)) {
            return false;
        }
        return true;
    }

    private static int nextPowerOf2(final int value) {
        return Integer.highestOneBit((value - 1) * 2);
    }

    private static final int extendSize(final int from) {
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

    private static final int hashShift(final int key) {
        /**
         * This hash is a way of shifting 4-bit blocks, nibbles in a way that
         * the resulting nibbles are the XOR value of itself and all nibbles to
         * the left. Start with key (each letter represents a nibble, each line
         * represents an XOR)
         * 
         * <pre>
         *    A B C D E F G H
         * </pre>
         */
        final int four = key ^ (key >>> 16);

        /**
         * four is now:
         * 
         * <pre>
         *    A B C D E F G H
         *            A B C D
         * </pre>
         */
        final int two = four ^ (four >>> 8);
        /**
         * Two is now
         * 
         * <pre>
         *    A B C D E F G H
         *            A B C D
         *        A B C D E F
         *                A B
         * </pre>
         */
        final int one = two ^ (two >>> 4);
        /**
         * One is now:
         * 
         * <pre>
         *     A B C D E F G H
         *             A B C D
         *         A B C D E F
         *                 A B
         *       A B C D E F G
         *               A B C
         *           A B C D E
         *                   A
         * </pre>
         */
        return one;
    }

    private void deleteIndex(final int index) {
        if (deletedCount == deletedIndices.length) {
            deletedIndices = Arrays.copyOf(deletedIndices, extendSize(deletedIndices.length));
        }
        deletedIndices[deletedCount++] = index;
    }

    private int bucketId(final int key) {
        return mask & hashShift(key);
    }

    private int locate(final int[] bucket, final int bsize, final int key) {
        // keep buckets in sorted order, by the key value. Unfortunately, the
        // bucket contents are the index to the key, not the actual key,
        // otherwise Arrays.binarySearch would work.
        // Instead, re-implement binary search with the indirection.
        int left = 0;
        int right = bsize - 1;
        while (left <= right) {
            int mid = left + ((right - left) >> 1);
            int k = getValue(keys, bucket[mid]);
            if (k == key) {
                return mid;
            } else if (k < key) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return -left - 1;
    }

    private int addKeyValue(final int key, final int value) {
        if (deletedCount > 0) {
            // There's a previously deleted spot, reuse it.
            deletedCount--;
            final int pos = deletedIndices[deletedCount];
            setValue(keys, pos, key);
            setValue(values, pos, value);
            return pos;
        }
        if (size == MAX_SIZE) {
            throw new IllegalStateException("Cannot have more than Integer.MAX_VALUE members in the Map");
        }
        final int row = size >>> KVSHIFT;
        final int col = size & KVMASK;

        if (keys.length == row) {
            int sz = extendSize(row);
            keys = Arrays.copyOf(keys, sz);
            values = Arrays.copyOf(values, sz);
        }
        if (keys[row] == null) {
            keys[row] = new int[KVEXTENT];
            values[row] = new int[KVEXTENT];
        }
        keys[row][col] = key;
        values[row][col] = value;
        return size++;
    }

    private void insertBucketIndex(final int bucket, final int bucketPos, final int keyIndex) {
        if (bucketSize[bucket] == 0 && bucketData[bucket] == null) {
            bucketData[bucket] = new int[INITIAL_BUCKET_SIZE];
        } else if (bucketSize[bucket] == bucketData[bucket].length) {
            bucketData[bucket] = Arrays.copyOf(bucketData[bucket], extendSize(bucketData[bucket].length));
        }
        if (bucketPos < bucketSize[bucket]) {
            System.arraycopy(bucketData[bucket], bucketPos, bucketData[bucket], bucketPos, bucketSize[bucket]
                    - bucketPos);
        }
        bucketData[bucket][bucketPos] = keyIndex;
        bucketSize[bucket]++;
        if (bucketSize[bucket] > IDEAL_BUCKET_SIZE) {
            rebucket();
        }
    }

    private void rebucket() {
        // because of the "clever" hashing system used, we go from a X-bit to an
        // X+2-bit bucket count.
        // in effect, what this means, is that each bucket in the source is
        // split in to 4 buckets in the destination.
        // There is no overlap in the new bucket allocations, and the order of
        // the results in the new buckets will be the same relative order as the
        // source. This makes for a very fast rehash.... no sorting, searching,
        // or funny stuff needed. O(n).
        int[][] buckets = new int[bucketData.length * 4][];
        int[] sizes = new int[buckets.length];
        int msk = buckets.length - 1;
        for (int b = 0; b < bucketData.length; b++) {
            for (int p = 0; p < bucketSize[b]; p++) {
                addNewBucket(bucketData[b][p], buckets, sizes, msk);
            }
            // clear out crap as soon as we can,
            bucketData[b] = null;
        }
        bucketData = buckets;
        bucketSize = sizes;
        mask = msk;
    }

    private void addNewBucket(final int index, final int[][] buckets, final int[] sizes, final int msk) {
        int b = msk & hashShift(getValue(keys, index));
        if (sizes[b] == 0) {
            buckets[b] = new int[INITIAL_BUCKET_SIZE];
        } else if (sizes[b] == buckets[b].length) {
            buckets[b] = Arrays.copyOf(buckets[b], extendSize(buckets[b].length));
        }
        buckets[b][sizes[b]++] = index;
    }

    /* *****************************************************************
     * Implement streams over the indices of non-deleted keys in the Map
     * *****************************************************************
     */

    private IntStream liveIndices() {
        return StreamSupport.intStream(new IndexSpliterator(modCount, size(), 0, bucketData.length), false);
    }

    private class IndexSpliterator extends Spliterators.AbstractIntSpliterator {

        private int lastBucket;
        private int bucket;
        private int pos = 0;
        private final int gotModCount;

        protected IndexSpliterator(int gotModCount, int expect, int from, int limit) {
            // index values are unique, so DISTINCT
            // we throw concurrentmod on change, so assume IMMUTABLE
            super(expect, Spliterator.IMMUTABLE + Spliterator.DISTINCT + Spliterator.SIZED + Spliterator.SUBSIZED);
            this.gotModCount = gotModCount;
            bucket = from;
            lastBucket = limit;
        }

        private void checkConcurrent() {
            if (modCount != gotModCount) {
                throw new ConcurrentModificationException(
                        "Map was modified between creation of the Spliterator, and the advancement");
            }
        }

        private int advance() {
            checkConcurrent();
            while (bucket < lastBucket && pos >= bucketSize[bucket]) {
                bucket++;
                pos = 0;
            }
            return bucket < lastBucket ? bucketData[bucket][pos++] : -1;
        }

        @Override
        public boolean tryAdvance(final IntConsumer action) {
            final int index = advance();
            if (index >= 0) {
                action.accept(index);
            }
            return index >= 0;
        }

        @Override
        public boolean tryAdvance(final Consumer<? super Integer> action) {
            final int index = advance();
            if (index >= 0) {
                action.accept(index);
            }
            return index >= 0;
        }

        @Override
        public Spliterator.OfInt trySplit() {
            checkConcurrent();
            int half = Arrays.stream(bucketSize, bucket + 1, lastBucket).sum() / 2;
            if (half < 8) {
                return null;
            }
            int sum = 0;
            for (int i = lastBucket; i > bucket; i--) {
                sum += bucketSize[i];
                if (sum > half) {
                    IndexSpliterator remaining = new IndexSpliterator(gotModCount, sum, i, lastBucket);
                    lastBucket = i;
                    return remaining;
                }
            }
            return null;
        }

        @Override
        public void forEachRemaining(final IntConsumer action) {
            checkConcurrent();
            if (bucket >= lastBucket) {
                return;
            }
            while (bucket < lastBucket) {
                while (pos < bucketSize[bucket]) {
                    action.accept(bucketData[bucket][pos]);
                    pos++;
                }
                bucket++;
                pos = 0;
            }
        }

        @Override
        public void forEachRemaining(final Consumer<? super Integer> action) {
            checkConcurrent();
            if (bucket >= lastBucket) {
                return;
            }
            while (bucket < lastBucket) {
                while (pos < bucketSize[bucket]) {
                    action.accept(bucketData[bucket][pos]);
                    pos++;
                }
                bucket++;
                pos = 0;
            }
        }

    }

    /* *****************************************************************
     * Hooks to allow for testing in the same package.
     * *****************************************************************
     */

    int getBucketCount() {
        return bucketData.length;
    }

    int getDeletedCount() {
        return deletedCount;
    }

    int getSumSizes() {
        return IntStream.of(bucketSize).sum();
    }

}
