package testing;

import java.io.IOException;

import org.apache.log4j.Level;

import org.junit.*;
import app_kvECS.*;
import app_kvServer.KVServer;
import app_kvServer.app_kvDatabase.DatabaseManager;
import junit.framework.Test;
import junit.framework.TestSuite;
import logger.LogSetup;

public class AllTests {
	public static ECS ecs;

	static {
		try {
			new LogSetup("logs/testing/test.log", Level.ERROR);
			ecs = new ECS("ecs.config");
			ecs.setupNodes(1, "lru", 100);
			ecs.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
/*
	public static void main(String [] args) {
		suite();
	}
*/
	public static Test suite() {
		TestSuite clientSuite = new TestSuite("Basic Storage ServerTest-Suite");
		clientSuite.addTestSuite(ConnectionTest.class);
		clientSuite.addTestSuite(InteractionTest.class);
		//clientSuite.addTestSuite(AdditionalTest.class);
		//clientSuite.addTestSuite(AdditionalTest2.class);
		clientSuite.addTestSuite(AdditionalTest3.class);
		return clientSuite;
	}

}