package app_kvServer.app_kvCache;

import java.util.concurrent.ConcurrentLinkedQueue;

public class LRUCache extends Cache {

    private ConcurrentLinkedQueue<String> lruQueue;

    public LRUCache(int max_size) {
        super(max_size);
        lruQueue = new ConcurrentLinkedQueue<String>();
    }

    @Override
    protected void update(String key) {
        lruQueue.remove(key);
        lruQueue.add(key);
    }

    @Override
    public void evict() throws Exception {
        String keyToEvict = lruQueue.poll();
        if (keyToEvict == null)
            throw new Exception("Cache is empty!");
        this.cache.remove(keyToEvict);
        this.size--;
    }

    @Override
    public void delete(String key) {
        super.delete(key);
        lruQueue.remove(key);
    }

    @Override
    public void clear() {
        super.clear();
        lruQueue.clear();
    }
    
}
