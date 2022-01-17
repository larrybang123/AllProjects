package app_kvServer.app_kvCache;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class LFUCache extends Cache {

    private PriorityBlockingQueue<LFUEntry> lfuQueue;
    private ConcurrentHashMap<String, Integer> lfuFreqLookup;

    private class LFUEntry implements Comparable<LFUEntry> {
        private String key;
        private int numAccesses;
    
        public LFUEntry(String key, int freq) {
            this.key = key;
            this.numAccesses = freq;
        }
    
        public int getNumAccesses() {
            return this.numAccesses;
        }
    
        public String getKey() {
            return this.key;
        }
    
        public boolean equals(LFUEntry other) { 
            return this.getKey() == other.getKey();
        }
    
        @Override
        public int compareTo(LFUEntry other) {
            return this.getNumAccesses() - other.getNumAccesses();
        }
    }


    public LFUCache(int max_size) {
        super(max_size);
        lfuQueue = new PriorityBlockingQueue<LFUEntry>();
        lfuFreqLookup = new ConcurrentHashMap<String, Integer>();
    }

    @Override
    protected void update(String key) {
        // increment freq
        if (!lfuFreqLookup.contains(key)) {
            lfuFreqLookup.put(key, 1);
        }
        else {
            int freq = lfuFreqLookup.get(key) + 1;
            lfuFreqLookup.put(key, freq); 
            
            lfuQueue.remove(key);
            lfuQueue.add(new LFUEntry(key, freq));
        }
    }

    @Override
    public void evict() throws Exception {
        LFUEntry entryToEvict = lfuQueue.poll();
        if (entryToEvict == null)
            throw new Exception("Cache is empty!");
        String keyToEvict = entryToEvict.getKey();
        lfuFreqLookup.remove(keyToEvict);
        this.cache.remove(keyToEvict);
        this.size--;
    }

    @Override
    public void delete(String key) {
        super.delete(key);
        lfuQueue.remove(key);
        lfuFreqLookup.remove(key);
    }

    @Override
    public void clear() {
        super.clear();
        lfuQueue.clear();
        lfuFreqLookup.clear();
    }

}