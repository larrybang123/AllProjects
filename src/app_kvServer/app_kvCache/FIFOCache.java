package app_kvServer.app_kvCache;

import java.util.concurrent.ConcurrentLinkedQueue;

public class FIFOCache extends Cache {

    private ConcurrentLinkedQueue<String> fifoQueue;

    public FIFOCache(int max_size) {
        super(max_size);
        fifoQueue = new ConcurrentLinkedQueue<String>();
    }

    @Override
    protected void update(String key) {
        if (!fifoQueue.contains(key))
            fifoQueue.add(key);
    }

    @Override
    public void evict() throws Exception {
        String keyToEvict = fifoQueue.poll();
        if (keyToEvict == null)
            throw new Exception("Cache is empty!");
        this.cache.remove(keyToEvict);
        this.size--;
    }

    @Override
    public void delete(String key) {
        super.delete(key);
        fifoQueue.remove(key);
    }

    @Override
    public void clear() {
        super.clear();
        fifoQueue.clear();
    }

}