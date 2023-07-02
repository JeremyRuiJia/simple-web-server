package liteweb;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadSafeLRUCache<K, V> {

    private final int capacity;
    private final LinkedHashMap<K, V> cache;
    private final Lock lock;
    private final Condition notFull;
    private final Condition notEmpty;

    public ThreadSafeLRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new LinkedHashMap<K, V>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
        this.lock = new ReentrantLock();
        this.notFull = lock.newCondition();
        this.notEmpty = lock.newCondition();
    }

    public void put(K key, V value) throws InterruptedException {
        lock.lock();
        try {
            while (cache.size() > capacity) {
                notFull.await();
            }
            cache.put(key, value);
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public V get(K key) throws InterruptedException {
        lock.lock();
        try {
            if (!cache.containsKey(key)) {
                return null; // or throw an exception, or return a default value
            }
            while (!cache.containsKey(key)) {
                notEmpty.await();
            }
            V value = cache.get(key);
            cache.remove(key);
            cache.put(key, value); // Move the accessed entry to the end of the map
            notFull.signalAll();
            return value;
        } finally {
            lock.unlock();
        }
    }
}
