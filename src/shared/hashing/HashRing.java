package shared.hashing;

import app_kvECS.ECSNode;
import org.apache.log4j.Logger;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class HashRing<T extends ECSNode> {
    public SortedMap<BigInteger, ECSNode> ring;
    private Logger logger = Logger.getRootLogger();
    private int size;

    public HashRing() {
        this.ring = new TreeMap<>();
        this.size = 0;
    }

    public SortedMap<BigInteger, ECSNode> getRing(){return ring;}
    public Set<Map.Entry<BigInteger, ECSNode>> getEntrySet() {
        return ring.entrySet();
    }

    public int getSize() {
        return size;
    }

    public void addNode(ECSNode ecsNode) throws NoSuchAlgorithmException {
        BigInteger hashValue = getHashValue(ecsNode.getNodeHashName());
        if (ring.isEmpty()) {
            ecsNode.setNodeHashRange(hashValue, hashValue);
        } else {
            SortedMap<BigInteger, ECSNode> headMap = ring.headMap(hashValue);
            SortedMap<BigInteger, ECSNode> tailMap = ring.tailMap(hashValue);
            ECSNode biggerNode = (ECSNode) (tailMap.isEmpty() ? ring.get(ring.firstKey()) : ring.get(tailMap.firstKey()));
            ECSNode smallerNode = (ECSNode) (headMap.isEmpty() ? ring.get(ring.lastKey()) : ring.get(headMap.lastKey()));
            smallerNode.setNodeHashRange(getHashValue(smallerNode.getNodeHashName()), hashValue);
            ecsNode.setNodeHashRange(hashValue, getHashValue(biggerNode.getNodeHashName()));
        }
        ring.put(hashValue, ecsNode);
        size++;
    }

    public void removeNode(ECSNode ecsNode) throws NoSuchAlgorithmException {
        if (ring.containsKey(getHashValue(ecsNode.getNodeHashName()))) {
            BigInteger hashValue = getHashValue(ecsNode.getNodeHashName());
            SortedMap<BigInteger, ECSNode> headMap = ring.headMap(hashValue);
            SortedMap<BigInteger, ECSNode> tailMap = ring.tailMap(hashValue);
            ECSNode biggerNode = (ECSNode) (tailMap.isEmpty() ? ring.get(ring.firstKey()) : ring.get(tailMap.firstKey()));
            ECSNode smallerNode = (ECSNode) (headMap.isEmpty() ? ring.get(ring.lastKey()) : ring.get(headMap.lastKey()));
            smallerNode.setNodeHashRange(getHashValue(smallerNode.getNodeHashName()), getHashValue(biggerNode.getNodeHashName()));
            ring.remove(getHashValue(ecsNode.getNodeHashName()));
        }
        size--;
    }


    public BigInteger getHashValue(String key) throws NoSuchAlgorithmException {
        return new BigInteger(Hashing.getMd5(key), 16);
    }

    public Map<String, ECSNode> getNodes() {
        Map<String, ECSNode> outMap = new HashMap<>();
        for (Map.Entry<BigInteger, ECSNode> entry : ring.entrySet()) {
            outMap.put(entry.getValue().getNodeHashName(), entry.getValue());
        }
        return outMap;
    }

    public ECSNode getNode(BigInteger hashedName) {
        return ring.get(hashedName);
    }


    public ECSNode getSmallerNode(ECSNode ecsNode) throws NoSuchAlgorithmException {
        BigInteger hashValue = getHashValue(ecsNode.getNodeHashName());
        SortedMap<BigInteger, ECSNode> headMap = ring.headMap(hashValue);
        ECSNode smallerNode = (ECSNode) (headMap.isEmpty() ? ring.get(ring.lastKey()) : ring.get(headMap.lastKey()));
        return smallerNode;
    }

    public ECSNode getBiggerNode(ECSNode ecsNode) throws NoSuchAlgorithmException {
        BigInteger hashValue = getHashValue(ecsNode.getNodeHashName());
        SortedMap<BigInteger, ECSNode> tailMap = ring.tailMap(hashValue);
        ECSNode biggerNode = tailMap.isEmpty() ? ring.get(ring.firstKey()) : ring.get(tailMap.firstKey());
        return biggerNode;
    }
}


