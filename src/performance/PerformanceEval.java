package performance;

import java.lang.Math;
import java.util.Date; 
import java.io.IOException;

import app_kvServer.*;
import client.*;
import app_kvClient.*;

import shared.messages.KVMessage;
import shared.messages.KVMessage.StatusType;

final class KVTestObject {
    public String key;
    public String value;
    public double weight;

    public KVTestObject() {}
    
    public void update(String key, String value, double weight) {
        this.key = key;
        this.value = value;
        this.weight = weight;
    }
}

public class PerformanceEval {

    final static int TOTAL_REQUESTS = 10000;
    static double PERCENT_PUT_OVER_TOTAL_REQ = 50;

    public static void main(String[] args) {
        System.out.println("Running performance test, press enter once you've set up the servers as specified below");
        System.out.println("1: port = 50000, cacheSize = 50, cacheStrat = FIFO");
        System.out.println("1: port = 50010, cacheSize = 50, cacheStrat = LRU");
        System.out.println("1: port = 50020, cacheSize = 50, cacheStrat = LFU");
        try {
            System.in.read();
        }
        catch (IOException e){
            System.out.println("Error reading from user");
        }
        for (PERCENT_PUT_OVER_TOTAL_REQ = 20; PERCENT_PUT_OVER_TOTAL_REQ < 81; PERCENT_PUT_OVER_TOTAL_REQ += 30) {
            boolean loggingEnabled = false;
            long[] cacheTimes = testCachePolicies(loggingEnabled);
            long[] cacheTimes1 = testCachePolicies(loggingEnabled);
            long[] cacheTimes2 = testCachePolicies(loggingEnabled);
            System.out.println("Total time taken to service " + TOTAL_REQUESTS + " requests by");
            System.out.println("FIFO: " + cacheTimes[0] + ", " + cacheTimes1[0] + ", " + cacheTimes2[0] + " ms");
            System.out.println("LRU: " + cacheTimes[1] + ", " + cacheTimes1[1] + ", " + cacheTimes2[1] + " ms");
            System.out.println("LFU: " + cacheTimes[2] + ", " + cacheTimes1[2] + ", " + cacheTimes2[2] + " ms");
            System.out.println("With total percent of requests equal to PUT = " + PERCENT_PUT_OVER_TOTAL_REQ + "%");
        }
        
    }

    // test policies and return FIFOTime, LRUTime, LFUTime
    public static long[] testCachePolicies(boolean loggingEnabled) {
        
        if (loggingEnabled) System.out.println("Running performance test...");

        /*try {
            new LogSetup("logs/testing/perf.log", Level.ERROR);
            System.out.println("Log Setup");
            System.out.print("Creating servers...");
            new KVServer(50000, 10, "FIFO");
            System.out.print(" 1");
            new KVServer(50010, 10, "LRU");
            System.out.print(" 2");
            new KVServer(50020, 10, "LFU");
            System.out.println(" 3");
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        
        KVStore kvClient1 = new KVStore("localhost", 50000);
        KVStore kvClient2 = new KVStore("localhost", 50010);
        KVStore kvClient3 = new KVStore("localhost", 50020);
        if (loggingEnabled) System.out.println("Clients created");
        if (loggingEnabled) System.out.println("Generating test objects...");
        KVTestObject[] testObjects = generateTestObjects();
        if (loggingEnabled) System.out.println("Generating request order...");
        int[] requests = generateTestOrder(TOTAL_REQUESTS, testObjects);

        long FIFOTime = runTest(kvClient1, testObjects, requests);
        long LRUTime = runTest(kvClient2, testObjects, requests);
        long LFUTime = runTest(kvClient3, testObjects, requests);

        long[] results = {FIFOTime, LRUTime, LFUTime};

        return results;
    }

    // returns int array corresponding to which requests should be sent
    // (ex if 1 1 2 send a request using testObjects 1 and 2)
    private static int[] generateTestOrder(int requestCount, KVTestObject[] testObjects) {
        int[] requests = new int[requestCount];

        for(int i = 0; i < requestCount; i++) {
            double random = 100 * Math.random();
            double probability = 0;
            for(int j = 0; probability <= random && j < 15625; j++) {
                probability += testObjects[j].weight;
                if (probability > random || j >= 15625) {
                    requests[i] = j;
                    break; // this break might be unnecessary
                }
            }
        }

        return requests;
    }

    // returns KVTestObjects with a weight sum of 100
    private static KVTestObject[] generateTestObjects() {
        KVTestObject[] testObjects = new KVTestObject[15625];
        String[] keys = new String[15625];
        double[] weights = new double[15625];
        double totalWeight = 0;
        int i = 0;
        for (char l1 = 'a'; l1 != 'z'; l1 = (char)(l1 + 1)) {
            for (char l2 = 'a'; l2 != 'z'; l2 = (char)(l2 + 1)) {
                for (char l3 = 'a'; l3 != 'z'; l3 = (char)(l3 + 1)) {
                    char[] _key = {l1, l2, l3};
                    keys[i] = new String(_key);
                    weights[i] = Math.random();
                    totalWeight += weights[i];
                    //System.out.println("l1: " + l1 + ", l2:" + l2 + ", l3:" + l3);
                    //System.out.println(keys[i] + ": " + i);
                    i++;
                }
            }    
        }

        double weightMultiplier = 100 / totalWeight;

        for(i = 0; i < 15625; i++) {
            weights[i] *= weightMultiplier;
            testObjects[i] = new KVTestObject();
            testObjects[i].update(keys[i], "this is the value we're going with", weights[i]);
        }

        return testObjects;

    }
	
    private static long runTest(KVStore kvClient, KVTestObject[] testObjects, int[] requestOrder) {
        float averageLatency;

        long startTime = (new Date()).getTime();
        for (int i = 0; i < requestOrder.length; i++) {
            int reqIndex = requestOrder[i];

            Exception ex = null;
            try {
                if ((i % 100) < PERCENT_PUT_OVER_TOTAL_REQ) {
                    kvClient.put(testObjects[reqIndex].key, testObjects[reqIndex].value);
                } else {
                    kvClient.get(testObjects[reqIndex].key);
                }
            } catch (Exception e) {
                ex = e;
            }
        }
        long endTime = (new Date()).getTime();
        return endTime - startTime;
	}
}
