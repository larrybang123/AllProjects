package testing;


import java.io.File;
import java.net.UnknownHostException;
import java.util.*;

import java.io.IOException;

import app_kvClient.KVClient;
import app_kvECS.*;
import app_kvServer.app_kvDatabase.DatabaseManager;
import app_kvServer.app_kvDatabase.KVObject;
import client.KVStore;
import junit.framework.TestCase;
import shared.metadata.*;
import shared.messages.*;

import logger.LogSetup;
import org.apache.log4j.Level;

import org.junit.*;

import java.util.HashMap;
import java.util.Iterator;

import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AdditionalTest3 extends TestCase {
    private KVStore kvClient;
    private KVClient kvapplication;
    private ECS ECS;


    @Test
    public void test1() {
        KVStore Server1 = new KVStore("localhost", 50000);
        KVStore Server2 = new KVStore("localhost", 50001);

        int length1=0;
        int length2=0;
        int length3=0;


        try {
            Server1.connect();
            Server2.connect();
            Server1.put("inputkey1", "value1");
            Server2.put("inputkey2", "value2");
            Server1.put("inputkey3", "value3");
            Server2.put("inputkey4", "value4");
            Server1.put("inputkey5", "value5");
            Server2.put("inputkey6", "value6");

            Map<String,KVObject[]> result1 = ECS.initData();
            List<Integer> list1 = new ArrayList<Integer>();
            Iterator<Map.Entry<String, KVObject[]>> itServerEntry1 = result1.entrySet().iterator(); //find entries of all servers
            while (itServerEntry1.hasNext()) {
                Map.Entry<String, KVObject[]> entry =itServerEntry1.next();
                list1.add( entry.getValue().length);
            length1=list1.get(0);
            length2=list1.get(1);
            if ( length1==3 && length2==3) {
                assertNull(null);;
            }
            else
            {assertNotNull(null);}
            }

            KVStore Server3 = new KVStore("localhost", 50002);
            Server3.connect();

            Map<String,KVObject[]> result2 = ECS.initData();
            List<Integer> list2 = new ArrayList<Integer>();
            Iterator<Map.Entry<String, KVObject[]>> itServerEntry2 = result2.entrySet().iterator(); //find entries of all servers
            while (itServerEntry2.hasNext()) {
                Map.Entry<String, KVObject[]> entry =itServerEntry2.next();
                list2.add( entry.getValue().length);
                length1=list2.get(0);
                length2=list2.get(1);
                length3=list2.get(2);
                if ( length1==2 && length2==2 &&length3==2) {
                    assertNull(null);;
                }
                else
                {assertNotNull(null);}
            }

        } catch (Exception e) {
            assertNotNull(null);
        }
    }
}


