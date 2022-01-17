package app_kvServer.app_kvCache;

import java.util.concurrent.ConcurrentHashMap;

public abstract class Cache {

    protected ConcurrentHashMap<String, String> cache;
    protected int max_size;
    protected int size;

    public Cache(int max_size) {
        this.max_size = max_size;
        this.size = 0;
        cache = new ConcurrentHashMap<String, String>();
    }

    protected abstract void update(String key);
    public abstract void evict() throws Exception;

    public String get(String key) {
        if (!contains(key))
            return null;
        String val = cache.get(key);
        update(key);
        return val;
    }

    public void insert(String key, String value) throws Exception {
        if (this.get(key) != null) {
            this.cache.put(key, value);
            return;
        }
        if (this.size == this.max_size)
            evict();
        cache.put(key, value);
        update(key);
        this.size++;
    }

    public void delete(String key) {
        if (contains(key)) {
            this.cache.remove(key);
            this.size--;
        }
    }

    public boolean contains(String key) {
        return cache.containsKey(key);
    }

    public String getNoUpdate(String key) {
        return cache.get(key);
    }

    public int getMaxSize() {
        return this.max_size;
    }

    public int getCurrentSize() {
        return this.size;
    }

    public void clear() {
        this.cache.clear();
        this.size = 0;
    }

}