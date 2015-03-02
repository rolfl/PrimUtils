package net.tuis.primutils;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.tuis.primutils.PrimOps.*;

/**
 * Relate unique key values to an int index.
 * <p>
 * The first added key will be index 0, and so on. The order and value of the
 * keys is arbitrary, and can be any value from Integer.MIN_VALUE to
 * Integer.MAX_VALUE inclusive. There is a hard limit of at most
 * Integer.MAX_VALUE key mappings though. Further, there is no guarantee on the
 * order of keys returned in any streams or other multi-value return structures.
 * While the value and order of the keys is arbitrary, the sequence of any index
 * values returned by the {@link #add(int)} method is not. The system has the
 * following guarantees:
 * <ol>
 * <li>index values will always start from 0
 * <li>adding new values will always return a value 1 larger than the previous
 * add, unless there are deleted keys.
 * <li>deleting a key will create a 'hole' in the index sequence
 * <li>adding a new key when there are currently 'holes' in the index sequence
 * (after a delete), will reuse one of the previously deleted indexes.
 * <li>as a consequence, there is no guarantee that index values will be
 * strictly sequential, but that no two keys will ever return the same index
 * value
 * </ol>
 * <p>
 * Memory footprint overhead is relatively small for instances of the
 * IntKeyIndex class. There is a requirement for an indexing system and a key
 * storage system. These storage systems have an initial space allocated for
 * each instance. An empty, minimal instance will consume in the order of 4KB,
 * but, that same instance, with millions of entries will have less than 1% of
 * overhead wasted. What this means is that the system, like many other
 * collections, is not useful for many (thousands of) small instances. On the
 * other hand, a few small instances are fine, and a few huge instances are
 * great.
 * <p>
 * In addition to an int[] array for each of the keys, there is an int[]-based
 * array structure used to hash-index the location of the keys too. In other
 * words, there are two int values stored for each key indexed, and a very small
 * overhead after that. there's another array cell in an indexing system.
 * <p>
 * The memory used by this class, then, is about 2 ints per entry * 4 bytes per
 * int, a 1000 member map will use 8000 bytes. Compare that with a
 * Map&lt;Integer,Integer&gt; which would consume about....100 bytes per entry.
 * <p>
 * Due to odd Java implementations, you cannot create arrays with as many as
 * Integer.MAX_VALUE entries, but this class, can support up to that amount.
 * 
 * @author rolf
 *
 */
public final class IntKeyIndex {

    private static final int IDEAL_BUCKET_SIZE = 64;
    private static final int INITIAL_BUCKET_SIZE = 8;
    private static final int MIN_BUCKET_COUNT = 16;

    private int[][] bucketData;
    private int[] bucketSize;
    private int size;
    private int mask;
    private int[] deletedIndices = null;
    private int deletedCount = 0;
    private int modCount = 0;

    private final IntArray keys = new IntArray();

    /**
     * Create an IntIntMap with the specified "Not There" value, and the
     * specified initial capacity.
     * 
     * @param capacity
     *            the initial capacity to budget for.
     */
    public IntKeyIndex(final int capacity) {

        int nxtp2 = nextPowerOf2(capacity / IDEAL_BUCKET_SIZE);
        int bCount = Math.max(MIN_BUCKET_COUNT, nxtp2);
        bucketData = new int[bCount][];
        bucketSize = new int[bCount];
        mask = bCount - 1;
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
     * Determine whether there are any mappings in the Map
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
        return getIndex(key) >= 0;
    }

    /**
     * Identify whether an index is mapped to a key.
     * 
     * @param index
     *            the index to check the mapping for.
     * @return true if the key was previously mapped.
     */
    public boolean containsIndex(final int index) {
        if (index < 0 || index >= size) {
            return false;
        }
        if (deletedCount > 0 && Arrays.stream(deletedIndices, 0, deletedCount).anyMatch(i -> i == index)) {
            return false;
        }
        return true;
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
        final int bucket = bucketId(key);
        final int bucketPos = locate(bucketData[bucket], bucketSize[bucket], key);
        if (bucketPos >= 0) {
            // existing index
            return bucketData[bucket][bucketPos];
        }
        // only changes to the actual key values make a difference on the
        // iteration.
        // addKeyValue is the only place where max size is actually checked.
        int keyIndex = addKeyValue(key);
        modCount++;
        insertBucketIndex(bucket, -bucketPos - 1, keyIndex);
        return -keyIndex - 1;
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
        final int bucket = bucketId(key);
        final int pos = locate(bucketData[bucket], bucketSize[bucket], key);
        return pos < 0 ? -1 : bucketData[bucket][pos];
    }

    /**
     * Return the key value that maps to the specified index, if any.
     * 
     * @param index
     *            The index to lookup
     * @param notThere
     *            the value to return if the index is not associated to a key.
     * @return the key mapping to this index, or notThere if the index is not
     *         associated. Use {@link #containsIndex(int)} to check.
     */
    public int getKey(final int index, final int notThere) {
        return containsIndex(index) ? keys.get(index) : notThere;
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
        final int bucket = bucketId(key);
        final int pos = locate(bucketData[bucket], bucketSize[bucket], key);
        if (pos < 0) {
            return -1;
        }
        // only changes to the actual key values make a difference on the
        // iteration.
        modCount++;
        final int index = bucketData[bucket][pos];
        deleteIndex(index);
        bucketSize[bucket]--;
        System.arraycopy(bucketData[bucket], pos + 1, bucketData[bucket], pos, bucketSize[bucket] - pos);
        return index;
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
     * Get all indices that are mapped in this Map (the order of the indices is
     * not sequential).
     * <p>
     * There is a guarantee that the values represented in this array have a
     * 1-to-1 positional mapping to their respective keys returned from
     * {@link #getKeys()} if no modifications to the map have been made
     * between the calls
     * 
     * @return all values in the map in the matching order as
     *         {@link #getKeys()}
     */
    public int[] getIndices() {
        return streamIndices().toArray();
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
        return liveIndices().map(i -> keys.get(i));
    }

    /**
     * Stream all indices that are mapped in this Map.
     * <p>
     * There is a guarantee that the values represented in this array have a
     * 1-to-1 positional mapping to their respective keys returned from
     * {@link #streamKeys()} if no modifications to the map have been made
     * between the calls
     * 
     * @return all values in the map in the matching order as
     *         {@link #getKeys()}
     */
    public IntStream streamIndices() {
        return liveIndices();
    }
    
    /**
     * Stream all entries in an Entry container.
     * @return a stream of all Key to Index mappings.
     */
    public Stream<IntKIntVEntry> streamEntries() {
        return liveIndices().mapToObj(i -> new IntKIntVEntry(keys.get(i), i));
    }

    /**
     * Create a string representation of the state of the KeyIndex instance.
     * 
     * @return a string useful for toString() methods.
     */
    public String report() {
        long allocated = Stream.of(bucketData).filter(b -> b != null).mapToLong(b -> b.length).sum();
        long max = IntStream.of(bucketSize).max().getAsInt();
        long vals = Stream.of(keys).filter(vs -> vs != null).count() * KVEXTENT;
        return String.format("IntIntMap size %s (used %d, deleted %d) buckets %d hashspace %d longest %d valspace %d",
                size(), size, deletedCount, bucketSize.length, allocated, max, vals);
    }

    /**
     * Compute a hashCode using just the key values in this map. The resulting
     * hash is the same regardless of the insertion order of the keys.
     * 
     * @return a useful hash of just the keys in this map.
     */
    public int getKeyHashCode() {
        if (size() == 0) {
            return 0;
        }
        return liveIndices().map(i -> keys.get(i)).map(k -> Integer.rotateLeft(k, k)).reduce((x, p) -> x ^ p)
                .getAsInt();
    }

    /**
     * Compute a hashCode using just the indexes mapped in this map. The
     * resulting hash is the same regardless of the insertion order of the keys.
     * Two maps which have the same indexes provisioned will have the same
     * resulting hashCode.
     * 
     * @return a useful hash of just the keys in this map.
     */
    public int getIndexHashCode() {
        if (size() == 0) {
            return 0;
        }
        return liveIndices().map(k -> Integer.rotateLeft(k, k)).reduce((x, p) -> x ^ p).getAsInt();
    }

    /**
     * Return true if this instance has the exact same key/index mappings.
     * 
     * @param obj
     *            the other IntKeyIndex to check.
     * @return true if this instance has the exact same key/index mappings.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IntKeyIndex)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        IntKeyIndex other = (IntKeyIndex)obj;
        if (other.size() != size()) {
            return false;
        }

        return liveIndices().allMatch(i -> same(other, i));
    }

    @Override
    public int hashCode() {
        return Integer.rotateLeft(getKeyHashCode(), 13) ^ getIndexHashCode();
    }

    @Override
    public String toString() {
        return report();
    }

    /* *****************************************************************
     * Support methods for implementing the public interface.
     * *****************************************************************
     */

    private boolean same(final IntKeyIndex them, final int index) {
        final int k = keys.get(index);
        int t = them.getIndex(k);
        if (t != index) {
            return false;
        }
        return true;
    }

    private static int nextPowerOf2(final int value) {
        return Integer.highestOneBit((value - 1) * 2);
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
        if (deletedCount == 0 && deletedIndices == null) {
            deletedIndices = new int[INITIAL_BUCKET_SIZE];
        }
        if (deletedCount == deletedIndices.length) {
            deletedIndices = Arrays.copyOf(deletedIndices, extendSize(deletedIndices.length));
        }
        deletedIndices[deletedCount++] = index;
        keys.set(index, -1); // make the delete visible in the keys.
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
            int k = keys.get(bucket[mid]);
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

    private int addKeyValue(final int key) {
        if (deletedCount > 0) {
            // There's a previously deleted spot, reuse it.
            deletedCount--;
            final int pos = deletedIndices[deletedCount];
            keys.set(pos, key);
            return pos;
        }
        keys.set(size, key);
        return size++;
    }

    private void insertBucketIndex(final int bucket, final int bucketPos, final int keyIndex) {
        if (bucketSize[bucket] == 0 && bucketData[bucket] == null) {
            bucketData[bucket] = new int[INITIAL_BUCKET_SIZE];
        } else if (bucketSize[bucket] == bucketData[bucket].length) {
            bucketData[bucket] = Arrays.copyOf(bucketData[bucket], extendSize(bucketData[bucket].length));
        }
        if (bucketPos < bucketSize[bucket]) {
            System.arraycopy(bucketData[bucket], bucketPos, bucketData[bucket], bucketPos + 1, bucketSize[bucket]
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
        int b = msk & hashShift(keys.get(index));
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

        public IndexSpliterator(int gotModCount, int expect, int from, int limit) {
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
            for (int i = lastBucket - 1; i > bucket; i--) {
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

    final int getBucketCount() {
        return bucketData.length;
    }

    final int getDeletedCount() {
        return deletedCount;
    }

    final int getSumSizes() {
        return IntStream.of(bucketSize).sum();
    }

}
