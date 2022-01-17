package testing;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import java.io.IOException;
import java.util.Collection;

import app_kvClient.KVClient;
import app_kvECS.*;
import client.KVStore;
import shared.metadata.*;
import shared.messages.*;

import logger.LogSetup;
import org.apache.log4j.Level;

import org.junit.*;
import junit.framework.TestCase;
//import org.junit.jupiter.api.Test;
import app_kvECS.ECSNode;

public class AdditionalTest2 extends TestCase {

    public String putVal = "initialvalue";
//	public ECS ecs = null;
    private KVStore kvClient;
    private KVClient kvapplication;
    private ECS ecsnode;


    //@After
    public void kill() {
        System.out.println("Finished test");
        try {
            Thread.sleep(100);
        } catch (Exception e) {

        }
    }

    @Test
    public void testSetupNodes() {

        //startECS();

        Exception ex = null;

        KVStore Server1 = new KVStore("localhost", 50000);
        KVStore Server2 = new KVStore("localhost", 50001);

        try {
            Server1.connect();
            Server2.connect();
        } catch (Exception e) {
            ex = e;
        }
        kill();
        assertNull(ex);
    }

    @Test
    public void testPutindifferentserver() {
        //startECS();

        Exception ex = null;

        KVStore Server1 = new KVStore("localhost", 50000);
        KVStore Server2 = new KVStore("localhost", 50001);

        String [] expected = {"initialvalue", "initialvalue"};
        String [] actual = {null, null};
        try {
            Thread.sleep(1000);
            Server1.connect();
            Server2.connect();
            Server1.put("inputkey", putVal);
            actual[0] = Server1.get("inputkey").getValue();
            actual[1] = Server2.get("inputkey").getValue();

        } catch (Exception e) {
            kill();
            assertNotNull(null);
        }

        for (int i = 0; i < 2; i++) {
            if (!actual[i].equals(expected[i])) {
                kill();
                assertNotNull(null);
            }
        }
        kill();
        assertNull(null);
    }
    @Test
    public void testupdateindifferentserver() {

        //startECS();
        Exception ex = null;

        KVStore Server1 = new KVStore("localhost", 50000);
        KVStore Server2 = new KVStore("localhost", 50001);
        String [] expected = {"initialvalue", "initialvalue"};
        String [] expected2 = {"changevalue", "changevalue"};
        String [] actual = {null, null};
        try {
            //AllTests.ecs.start();
            Thread.sleep(1000);
            Server1.connect();
            Server2.connect();
            Server1.put("inputkey", putVal);
            actual[0] = Server1.get("inputkey").getValue();
            actual[1] = Server2.get("inputkey").getValue();
        } catch (Exception e) {
            kill();
            assertNotNull(null);
        }

        for (int i = 0; i < 2; i++) {
            if (!actual[i].equals(expected[i])) {
                kill();
                assertNotNull(null);
            }
        }
        //update
        try {
            String putVal ="changevalue";

            //AllTests.ecs.start();
            Thread.sleep(1000);
            //Server1.connect();
            //Server2.connect();

            Server1.put("inputkey", putVal);
            actual[0] = Server1.get("inputkey").getValue();
            actual[1] =Server2.get("inputkey").getValue();
        } catch (Exception e) {
            kill();
            assertNotNull(null);
        }
        for (int i = 0; i < 2; i++) {
            if (!actual[i].equals(expected2[i])) {
                kill();
                assertNotNull(null);
            }
        }
        kill();
        assertNull(null);
    }

    @Test
    public void testdeleteindifferentserver() {

        //startECS();

        Exception ex = null;

        KVStore Server1 = new KVStore("localhost", 50000);
        KVStore Server2 = new KVStore("localhost", 50001);

        String [] expected = {"initialvalue", "initialvalue"};

        String [] actual = {null, null};
        try {
            //AllTests.ecs.start();
            Thread.sleep(1000);
            Server1.connect();
            Server2.connect();

            Server1.put("inputkey", putVal);
            actual[0] = Server1.get("inputkey").getValue();
            actual[1] = Server2.get("inputkey").getValue();

        } catch (Exception e) {
            kill();
            assertNotNull(null);
        }

        for (int i = 0; i < 2; i++) {
            if (!actual[i].equals(expected[i])) {
                kill();
                assertNotNull(null);
            }
        }
        //delete
        KVMessage response1 = null;
        KVMessage response2 = null;

        try {
            Thread.sleep(1000);
            //Server1.connect();
            //Server2.connect();
            Server1.put("inputkey",null);

            response1 = Server1.get("inputkey");
            response2 = Server2.get("inputkey");
        } catch (Exception e) {
            kill();
            assertNotNull(null);
        }

        if (response1.getStatus()==KVMessage.StatusType.GET_ERROR && response2.getStatus()==KVMessage.StatusType.GET_ERROR) {
            kill();
            assertNull(null);
        }
        else {
            kill();
            assertNotNull(null);
        }

        kill();
        assertNull(null);
    }
    @Test
    public void getMetadata() {
        //startECS();
        try {
            KVStore Server1 = new KVStore("localhost", 50000);
            KVStore Server2 = new KVStore("localhost", 50001);
            Thread.sleep(1000);

            MetaData metadata = AllTests.ecs.getMetaData();
            MDNode server1 = metadata.get("server1");
            MDNode server2 = metadata.get("server2");


            int actualport1 = server1.getPort();
            int actualport2 = server2.getPort();


            if (actualport1 == 50000 && actualport2 == 50001  ) {
                kill();
                assertNull(null);
            } else {
                kill();
                assertNotNull(null);
            }
        } catch (Exception e) {
            kill();
            assertNotNull(null);
        }
        kill();
        assertNotNull(null);
    }


    @Test
    public void Testaddnodes_wrongnumber1(){
        kvClient = new KVStore("localhost", 50000);
        ecsnode =new ECS("ecs.config");
        try {
            kvClient.connect();
        } catch (Exception e) {
        }
        //if count number is <0
        Collection<IECSNode> status = ecsnode.addNodes(-1, "null", 0);


        assertTrue(status==null);
    }
    @Test
    public void Testaddnodes_wrongnumber2(){
        kvClient = new KVStore("localhost", 50000);
        ecsnode =new ECS("ecs.config");
        try {
            kvClient.connect();
        } catch (Exception e) {
        }
        //if count number is >avaliable number of servers
        Collection<IECSNode> status = ecsnode.addNodes(10000, "null", 0);

        assertTrue(status==null);
    }

    @Test
    public void Testremovenode_wrongnumber1() throws Exception {
        kvClient = new KVStore("localhost", 50000);
        ecsnode =new ECS("ecs.config");
        try {
            kvClient.connect();
        } catch (Exception e) {
        }
        Exception ex = null;
        //if count number is <0
        ecsnode.addNodes(2, "null", 0);
        //if count number is <0
        IECSNode status = ecsnode.removeNode(-1);


        assertTrue(status==null);
    }
    @Test
    public void Testremovenode_wrongnumber2() throws Exception {
        kvClient = new KVStore("localhost", 50000);
        ecsnode =new ECS("ecs.config");
        try {
            kvClient.connect();
        } catch (Exception e) {
        }
        Exception ex = null;
        ecsnode.addNodes(2, "null", 0);
        //if count number is >avaliable number of servers

        IECSNode status = ecsnode.removeNode(10000);
        assertTrue(status==null);
    }



}

