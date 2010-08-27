package jmeta;

import java.util.Iterator;

/**
 * This collection-map-like object can map the full range of integers to values and allows traversal.
 *
 * (It is trivial to also support in-order traversal; but this has a high cost for highly non-random inserts.)
 *
 * Insertion is near O(log n) operation, and the order of keys inserted hardly matters.
 * Deletion is O(log n) operation.
 * Every next() is near O(1) operation. Total traversal is near O(n) operation.
 *
 * Iterating and mutating is allowed, but there is no guarantee if the mutation will be seen by the iterator.
 *
 * Unfortunately, due to java primitives, it is impossible to implement the Map<K, V> interface (without a significant
 *  loss of performance).
 *
 * It is (loosely) based on Library Sort. http://en.wikipedia.org/wiki/Library_sort
 *
 * Author: Onne Gorter <onne@onnlucky.com>
 */
public class IntMap<V> implements Iterable<IntMap.Entry<V>> {
    private int size = 0;
    // we want to keep unused maps minimally invasive; but don't want to do null checks all the time
    private    int[] keys = new    int[] { Integer.MIN_VALUE };
    private Object[] data = new Object[] { null };

    // a simple reversible integer hashing scheme
    // -1640531535 == 2654435761 (unsigned; is 2^32 golden ration; from knuth)
    // Remove these and we have in-order traversal (a SortedIntMap). We could wrap a SortedIntMap with these hashes to
    // produce a IntMap with better insert performance.
    private static int  hash(int key) { return key * -1640531535; }
    private static int ihash(int key) { return key * 244002641; }

    // simple binary search in the keys array
    int indexOf(final int key) {
        int lo = 0;
        int hi = keys.length - 1;
        while (lo < hi) {
            int mid = lo + ((hi - lo) / 2);
            if (keys[mid] > key) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    /// get the value mapped by the key
    public V get(final int ikey) {
        final int key = hash(ikey);
        int at = indexOf(key);
        if (keys[at] == key) {
            // look forward until last entry that is still maps the key; this is the real entry
            for (at++; at < keys.length && keys[at] == key; at++);
            return (V) data[at - 1];
        }
        return null;
    }

    // grow the keys and data array
    void grow() {
        int nsize = keys.length * 2;
        int[]    okeys = keys;
        Object[] odata = data;

        keys = new    int[nsize];
        data = new Object[nsize];

        for (int io = 0, in = 0; in < keys.length; io++, in += 2) {
            keys[in] = keys[in + 1] = okeys[io];
            data[in + 1] = odata[io];
        }
        // resulting keys: [ key1, key1, key2, key2, ... ]
        // resulting data: [ null, data, null, data, ... ]
        // last key entry holds real data
    }

    /// insert a mapping from key to a value
    public void put(final int ikey, final V v) {
        final int key = hash(ikey);
        int at = indexOf(key);

        // key already exists in map; update it
        if (keys[at] == key) {
            // forward to real entry (see #get())
            for (at++; at < keys.length && keys[at] == key; at++);
            Object old = data[at - 1]; data[at - 1] = v;

            // account for deletes or updating old deletes
            if (old != null && v == null) size--;
            if (old == null && v != null) size++;
            return;
        }

        // key does not exist in map, and we are deleting it; a no-op
        if (v == null) return;

        // grow if needed; we do need a new position because there is a new list
        if (size >= keys.length) {
            grow();
            at = indexOf(key);
        }

        // look around for a spot; in big lists this might not be soo good for cache locality
        // maybe we want to grow sooner and not at exactly at size, otherwise we might still be moving lots of entries
        int spot;
        for (int n = 0;; n++) {
            if (at - n >= 0) if (data[at - n] == null) { spot = at - n; break; }
            if (at + n < keys.length) if (data[at + n] == null) { spot = at + n; break; }
        }

        // move everything over in the direction of the found empty spot; notice the edge condition
        if (spot < at) {        // [ .. , spot < < <, at, .. ]
            if (keys[at] < key) at--;
            for (int n = spot; n < at; n++) {
                keys[n] = keys[n + 1];
                data[n] = data[n + 1];
            }
        } else if (spot > at) { // [ .. , at, > > > spot, .. ]
            if (keys[at] > key) at++;
            for (int n = spot; n > at; n--) {
                keys[n] = keys[n - 1];
                data[n] = data[n - 1];
            }
        }

        // actually insert and add to size
        keys[at] = key;
        data[at] = v;
        size++;
    }

    /// delete the mapping form key to a value; notice this is the same as inserting a key to null mapping
    public void del(int key) {
        // deleting is just updating key position with null
        put(key, null);
    }

    /// return current size of items mapped
    public int size() { return size; }
    /// returns true if zero items are mapped
    public boolean isEmpty() { return size == 0; }
    /// remove all mappings
    public void clear() {
        size = 0;
        keys = new int[] { Integer.MIN_VALUE };
        data = new Object[] { null };
    }

    /// get an iterator over the entries using #Entry objects
    public Iterator<Entry<V>> iterator() {
        return new Iter(keys, data);
    }

    /// a single mapping from a key to a value
    public static class Entry<V> {
        public final int key;
        public final V value;
        private Entry(int k, V v) { key = k; value = v; }
        public int key() { return key; }
        public V value() { return value; }
    }

    // iterator implementation using the Entry class as result
    private class Iter<V> implements Iterator<IntMap.Entry<V>> {
        final int[]    indx;
        final Object[] data;
        int cur = 0, next = 0;

        private Iter(int[] is, Object[] ds) { indx = is; data = ds; }

        public boolean hasNext() {
            while (next < indx.length) {
                if (data[next] != null) return true;
                next++;
            }
            return false;
        }

        public Entry<V> next() {
            while (next < indx.length) {
                if (data[next] != null) {
                    cur = next; next++;
                    return new Entry<V>(ihash(indx[cur]), (V)data[cur]);
                }
                next++;
            }
            throw new IllegalStateException();
        }

        public void remove() {
            if (data[cur] == null) throw new IllegalStateException();
            del(indx[cur]);   // slighly lazy here
            data[cur] = null; // since our data array might be a copy due to grow()
        }
    }

    // check for consistency
    void debug(boolean output) {
        if (output) {
            for (int i = 0; i < keys.length; i++) {
                System.out.println(""+ i + ": "+ keys[i] +" = "+ data[i]);
            }
            System.out.println();
        }

        int last = Integer.MAX_VALUE;
        for (int i = 0; i < keys.length; i++) {
            assert (last >= keys[i]);
            last = keys[i];
        }
    }

    // test
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        IntMap<String> map = new IntMap<String>();

        map.put(9, "NEW9");
        map.put(8, "NEW8");
        map.put(7, "NEW7");
        map.put(6, "NEW6");
        map.put(5, "NEW5");
        map.put(4, "NEW4");
        map.put(3, "NEW3");
        map.put(2, "NEW2");
        map.put(1, "NEW1");
        map.put(0, "NEW0");
        map.put(100, "NEW100");

        //System.exit(0);

        int testi = 876;
        int testp = 0;
        for (int i = 0; i < 1000000; i++) {
            int p = (int) Math.round(Math.random() * 2000000 - 1000000);
            boolean d = Math.random() > 0.9;
            p = i;
            if (p == 100) p++;
            if (p == testp) p++;
            if (d && i != testi) map.del(p);
            else map.put(p, "FOO");
            if (i == testi) testp = p;
        }

        assert map.get(100) == "NEW100";
        assert map.get(testp).equals("FOO");

        System.out.println(map.size());
        System.out.println(map.get(100));
        System.out.println(map.get(testp));

        System.out.println(map.size());
        System.out.println(map.keys.length);

        //for (IntMap.Entry<String> entry : map) {
        //    if (entry.value == "FOO") continue;
        //    System.out.println(""+ entry.key +" -> "+ entry.value);
        //}

        System.out.println(String.format("takes: %.2fs", (System.currentTimeMillis() - start) / 1000f));

        map.debug(false);
    }
}
