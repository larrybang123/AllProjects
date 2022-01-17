package app_kvServer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import app_kvServer.app_kvCache.Cache;
import app_kvServer.app_kvCache.FIFOCache;
import app_kvServer.app_kvCache.LFUCache;
import app_kvServer.app_kvCache.LRUCache;
import app_kvServer.app_kvDatabase.DatabaseManager;
import app_kvServer.app_kvDatabase.KVObject;
import logger.LogSetup;
import shared.communication.CommunicationChannel;
import shared.hashing.Hashing;
import shared.metadata.MDNode;
import shared.metadata.MetaData;


public class KVServer extends Thread implements IKVServer {

	public enum ServerState {
		IDLE,
		STARTED,
		SHUTDOWN,
		STOPPED
	}

	private class ClientThread {
		public ClientConnection client;
		public Thread thread;

		public ClientThread(ClientConnection c, Thread t) {
			client = c;
			thread = t;
		}
	}

	private static Logger logger = Logger.getRootLogger();

	private ServerSocket serverSocket;
	private ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	private boolean running;

	private String name;
	private MetaData metadata;
	private String startHash;
	private String endHash;
	private ServerState state;
	private boolean writeLocked;

	private Cache cache;
	private CacheStrategy cachingMethod;
	private DatabaseManager database;

	private Hashing hasher;

	private final int MAX_KEY_SIZE = 20;
	private final int MAX_VAL_SIZE = 120000;
	
	//private JsonArray allServerInfo;

	/**
	 * Start KV Server at given port
	 * @param port given port for storage server to operate
	 * @param cacheSize specifies how many key-value pairs the server is allowed
	 *           to keep in-memory
	 * @param strategy specifies the cache replacement strategy in case the cache
	 *           is full and there is a GET- or PUT-request on a key that is
	 *           currently not contained in the cache. Options are "FIFO", "LRU",
	 *           and "LFU".
	 */
	public KVServer(int port, int cacheSize, String strategy) {
		//System.out.println("Run0");
		this.state = ServerState.STOPPED;
		this.running = initializeServer(port);

		// Values assigned on init:
		this.name = null;
		this.metadata = null;
		this.startHash = null;
		this.endHash = null;
		this.cache = null;
		this.database = null;

		this.hasher = new Hashing();
		//System.out.println("ok0");
		//run();
	}
	
	@Override
	public int getPort(){
		return serverSocket.getLocalPort();
	}

	public String getServerName() {
		return this.name;
	}

	public String[] getRange() {
		return new String[] {this.startHash, this.endHash};
	}

	@Override
    public String getHostname(){
		if (this.serverSocket != null)
			return this.serverSocket.getInetAddress().getHostName();
		else
			return null;
	}

	@Override
    public CacheStrategy getCacheStrategy(){
		return this.cachingMethod;
	}

	@Override
    public int getCacheSize(){
		if (this.cache == null)
			return 0;
		return this.cache.getMaxSize();
	}

	public DatabaseManager getDatabase() {
		return this.database;
	}

	@Override
    public boolean inStorage(String key) {
		try {
			if (database != null)
				return database.getKVObject(key) != null;
			else return false;
		} catch (FileNotFoundException fnfe) {
			return false;
		}
	}

	@Override
    public boolean inCache(String key){
		if (this.cache == null)
			return false;
		return cache.contains(key);
	}

	@Override
    public String getKV(String key) throws Exception {
		if (!isValidKey(key)) {
			logger.info("Key '" + key + "' is not valid.");
			throw new Exception("Invalid key.");
		}
		String value = null;
		if (this.cache != null)
			value = this.cache.get(key);
		if (value != null) {
			logger.info("Found cache entry for " + KVObject.printableString(key, value));
			return value;
		}
		try {
			if (database != null)
				value = database.getValue(key);
			if (value == null) {
				logger.info("Could not find database entry for '" + key + "'");
				throw new Exception("Could not find database entry for '" + key + "'");
			} else {
				logger.info("Found database entry for " + KVObject.printableString(key, value));
				return value;
			}
		} catch (FileNotFoundException fnfe) {
			logger.error("Trying to access an empty database.");
			throw new Exception("Could not find database entry for '" + key + "'");
		}
	}

	@Override
    public void putKV(String key, String value) throws Exception{
		//System.out.print("Ee1");
		if (!isValidKey(key)) {
			logger.info("Key '" + key + "' is not valid.");
			throw new Exception("Invalid key.");
		}
		//System.out.print("Ee2");
		if (!isValidValue(value)) {
			logger.info("Value '" + value + "' is not valid.");
			throw new Exception("Invalid value.");
		}
		//System.out.print("Ee3");
		boolean success = database != null && database.putKVObject(key, value);
		//System.out.print("Ee4");
		if (!success) {
		//System.out.print("Ee5");
			logger.error("Could not serve put request for " + KVObject.printableString(key, value));
			throw new Exception("Could not serve put request for " + KVObject.printableString(key, value));	
		}
		if (this.cache == null) {
		//	System.out.print("Ee6");
			return;
		}
		//System.out.print("Ee7");
		if (DatabaseManager.isDeleteRequest(value)) {
		//System.out.print("Ee8");
			this.cache.delete(key);
			logger.info("Removed cache entry for " + KVObject.printableString(key, value));
		} else {
		//System.out.print("Ee9");
			this.cache.insert(key, value);
			logger.info("Inserted cache entry for " + KVObject.printableString(key, value));
		}
	}

	@Override
    public void clearCache(){
		if (this.cache != null)
			this.cache.clear();
		logger.info("Cleared server cache.");
	}

	@Override
    public void clearStorage(){
		clearCache();
		if (database != null)
			database.clearDatabase();
		logger.info("Cleared database.");
	}

	public boolean isRunning() {
		return this.running;
	}

	@Override
    public void run(){
		if(this.serverSocket != null) {
			//System.out.println("Sicko");
			//logger.info("beep");
			logger.info(this.getPort());
			//logger.info(this.getCacheStrategy());
			//logger.info(this.getCacheStrategy().toString());
			logger.info("Server started on port " + this.getPort() + " with " + "no"/*this.getCacheStrategy().toString()*/ + " caching strategy.");
			//System.out.println("Mode");
	        while(this.running){
	            try {
	                Socket client = serverSocket.accept();  
	                ClientConnection connection =  new ClientConnection(client, this);
					Thread clientThread = new Thread(connection);
					this.clients.add(new ClientThread(connection, clientThread));
	                clientThread.start();
	                
	                logger.info("Connected to " 
	                		+ client.getInetAddress().getHostName() 
	                		+  " on port " + client.getPort());
	            } catch (IOException e) {
	            	logger.error("Error! " +
	            			"Unable to establish connection. \n", e);
	            }
	        }
		}
		
		close();
	}

	/**
	 * Initializes the server socket.
	 */
	public boolean initializeServer(int port) {	
		logger.info("Initializing server ...");
    	try {
            this.serverSocket = new ServerSocket(port);
            logger.info("Server listening on port: " 
					+ this.serverSocket.getLocalPort());
            return true;
        
        } catch (IOException e) {
        	logger.error("Error! Cannot open server socket:");
            if(e instanceof BindException){
            	logger.error("Port " + port + " is already bound!");
            }
            return false;
		}
	}

	@Override
    public void kill(){
		logger.info("Force killing server ...");
		running = false;
		//close();
		//this.stop();
		logger.info("Server killed.");
		System.exit(1);
	}

	@Override
    public void close() {
		//System.out.println("Closing Server ...");
		running = false;
		logger.info("Closing server ...");
		try {
			logger.info("Closing client connections ...");
			//System.out.println("Closing client connections ...");
			for (ClientThread clientThread:this.clients) {
				if (clientThread != null && clientThread.client != null)
					clientThread.client.close();
			}
			//logger.info("Waiting for ongoing requests to complete ...");
			//System.out.println("Waiting for ongoing requests to complete ...");
			/*for (ClientThread clientThread:this.clients) {
				if (clientThread != null && clientThread.thread != null)
					clientThread.thread.join();
			}*/

			//System.out.println("All threads joined.");

			if (serverSocket != null)
				serverSocket.close();

			logger.info("Server stopped.");
		} catch (Exception e) {
			logger.info("Error closing server.");
			System.out.println("Error closing server.");
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		
	}

	/**
	 * Checks whether a given key is valid or not
	 * @param key The key to check
	 * @return true if key is valid
	 */
	private boolean isValidKey(String key) {
		return !key.isEmpty() && key.length() <= MAX_KEY_SIZE && !key.contains(" ");
	}

	/**
	 * Checks whether a given value is valid or not
	 * @param value The value to check
	 * @return true if value is valid
	 */
	private boolean isValidValue(String value) {
		return value.length() <= MAX_VAL_SIZE;
	}

	public boolean isKeyInRange(String key) throws NoSuchAlgorithmException {
		if (database == null)
			return false;
		try {
			return hasher.isKeyInRange(key, this.startHash, this.endHash);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return false;
		}
		//return database.isKeyInRange(key);
	}

	/*
	 * ===========================================
	 * ECS FUNCTIONALITY
	 * ===========================================
	 */

	/**
	 * Initialize the KVServer with the metadata, it’s local cache size, 
	 * and the cache replacement strategy, and block it for client requests, 
	 * i.e., all client requests are rejected with an SERVER_STOPPED error message; 
	 * ECS requests have to be processed.
	 */
	public void initKVServer(String name, int cacheSize, String strategy, MetaData metadata) {
		this.name = name;
		this.metadata = metadata;
		String[] range = null;
		MDNode thisNode = this.metadata.get(this.name);
		range = thisNode.getRange();
		this.startHash = range[0];
		this.endHash = range[1];
		this.writeLocked = false;
		this.state = ServerState.STOPPED;
		this.database = new DatabaseManager(this);

		switch (strategy) {
			case "FIFO":
				this.cachingMethod = CacheStrategy.FIFO;
				cache = cacheSize > 0 ? new FIFOCache(cacheSize) : null;
				break;
			case "LRU":
				this.cachingMethod = CacheStrategy.LRU;
				cache = cacheSize > 0 ? new LRUCache(cacheSize) : null;
				break;
			case "LFU":
				this.cachingMethod = CacheStrategy.LFU;
				cache = cacheSize > 0 ? new LFUCache(cacheSize) : null;
				break;
			default:
				this.cachingMethod = CacheStrategy.None;
				cache = null;
				break;
		}
	}

	/**
	 * Starts the KVServer, all client requests and all ECS requests are processed.
	 */
	public void startServer() {
		this.state = ServerState.STARTED;
	}

	/**
	 * Stops the KVServer, all client requests are rejected and only ECS requests are processed.
	 */
	public void stopServer() {
		this.state = ServerState.STOPPED;
	}

	/**
	 * Exits the KVServer application.
	 */
	public void shutdown() {
		this.state = ServerState.SHUTDOWN;
		this.close();
		System.exit(0);
	}

	/**
	 * Lock the KVServer for write operations.
	 */
	public void lockWrite() {
		this.writeLocked = true;
	}

	/**
	 * Unlock the KVServer for write operations.
	 */
	public void unlockWrite() {
		this.writeLocked = false;
	}

	/**
	 * Transfer a subset (range) of the KVServer’s data to another KVServer 
	 * (reallocation before removing this server or adding a new KVServer to the ring); 
	 * send a notification to the ECS, if data transfer is completed.
	 */
	public boolean moveData(String start, String end, MDNode server) {
		// Get Data to Move
		System.out.println("starting to move data");
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("ECS receive ");	// Command Line
			if(database == null) {
				return false;
			}
			KVObject[] objs = database.getKVObjectsInRange(start, end);
			if (objs.length == 0 || objs == null)
				return true;
			for (KVObject kv:objs) {
				sb.append("\\n");
				sb.append(kv.toString());
			}
			// sb.append("\0");
			String msg = sb.toString();

			// Get Server to send data to
			String address = server.getHost();
			int port = server.getPort();
			Socket socket = new Socket(address, port);
			//System.out.println("Establishing Connection");
			// Establish communication and send message
			CommunicationChannel comm = new CommunicationChannel(socket);
			comm.send(msg.getBytes());

			// Wait for reply
			// String reply = comm.receive();
			// System.out.println(reply);

			//if (!reply.equals("receive success")) {
				//reply = comm.receive();
				//return false;
			//}
			// Delete the data moved
			database.deleteKVObjectsInRange(start, end);

			//System.out.println("finished moving data");
			return true;
		} catch (UnknownHostException e) {
			logger.error("Unknown Host!");
			return false;
		} catch (IOException e) {
			logger.error("IOException!");
			e.printStackTrace();
			return false;
		}
	}

	public boolean receive(KVObject[] kvObjs) {
		if (kvObjs.length == 0)
			return true;
		database.putKVObject(kvObjs);
		return true;
	}

	/**
	 * Update the metadata repository of this server
	 */
	public void updateMetadata(MetaData metadata) {
		this.metadata = metadata;
		System.out.println("name: " + this.name);
		String[] range = this.metadata.get(this.name).getRange();
		this.startHash = range[0];
		this.endHash = range[1];
	}


	public ServerState getServerState() {
		return this.state;
	}

	public boolean isWriterLocked() {
		return this.writeLocked;
	}

	public MetaData getMetadata() {
		return this.metadata;
	}

	// ==============================================================================================
	// ==============================================================================================
	// ==============================================================================================
	// ==============================================================================================
	// ==============================================================================================

	/*
	 * public JsonArray getAllServerInfo() { return allServerInfo; }
	 * 
	 * public void setAllServerInfo(JsonArray allServerInfo) { this.allServerInfo =
	 * allServerInfo; }
	 */

	/**
     * Main entry point for the kv server application. 
     * @param args contains the port number at args[0], cache size at args[1] and caching strategy at args[2].
     */
	public static void main(String[] args) {
		System.out.println("Run4");
        try {
            new LogSetup("logs/server.log", Level.ALL);
            if(args.length < 3) {
                System.out.println("Error! Invalid number of arguments!");
                System.out.println("Usage: KVServer port cacheSize cacheStrat");
            } else {
                int port = Integer.parseInt(args[0]);
                int cacheSize = Integer.parseInt(args[1]);
                String strategy = args[2];
				KVServer newServer = new KVServer(port, cacheSize, strategy);
				newServer.start();
           }
        } catch (IOException e) {
            System.out.println("Error! Unable to initialize logger!");
            e.printStackTrace();
            System.exit(1);
        } catch (NumberFormatException nfe) {
            System.out.println("Error! Invalid argument <port> or <cacheSize>! Not a number!");
            nfe.printStackTrace();
            System.exit(1);
        }
    }
	
	
}
