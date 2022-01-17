package app_kvECS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import app_kvServer.app_kvDatabase.DatabaseManager;
import app_kvServer.app_kvDatabase.KVObject;
import shared.CommonConstants;
import shared.hashing.HashRing;
import shared.hashing.Hashing;
import shared.metadata.MDNode;
import shared.metadata.MetaData;

public class ECS implements IECSClient {
	private static Logger logger = Logger.getRootLogger();

	private final int TIMEOUT = 10000;	// 10 seconds

	private boolean initRunning = false;
	private boolean firstInit = true;

	private Runtime run = Runtime.getRuntime();
	private ArrayList<Process> processes = new ArrayList<Process>();
	private HashMap<String, IECSNode> allServers = new HashMap<String, IECSNode>();
    private ArrayList<ECSNode> availableServers = new ArrayList<ECSNode>();
	private ArrayList<ECSNode> addedServers = new ArrayList<ECSNode>();
	private MetaData metadata = new MetaData();

	private HashRing<ECSNode> hashRing = new HashRing<>();

	//private ScheduledExecutorService executor;
	
	private ScheduledExecutorService executorBalancer;
	
	private boolean prcessingBalanceFlag = false;

	public ECS(String config_file) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(config_file));
			String line;
			//executor = Executors.newScheduledThreadPool(1);
			executorBalancer = Executors.newScheduledThreadPool(1);//set up a timer
			//executor.scheduleAtFixedRate(checkAlive, 0, 3, TimeUnit.SECONDS);
			executorBalancer.scheduleWithFixedDelay(balancer, 60, 60, TimeUnit.SECONDS);//60s
            while ((line = br.readLine()) != null) {
				String[] tokens = line.split("\\s+");
				String name = tokens[0];
				String host = tokens[1];
				int port = Integer.parseInt(tokens[2]);
				String lowHash = "0";
				String highHash = Hashing.getMd5(host + ":" + tokens[2]);
				ECSNode node = new ECSNode(name, host, port, lowHash, highHash);
				availableServers.add(node);
				allServers.put(node.getNodeName(), node);
            }
            br.close();
        } catch (Exception e) {
            System.out.println("An error occurred.");
			e.printStackTrace();
        }
	}

	@Override
	public boolean start() throws Exception {
		for (ECSNode node : ECS.this.addedServers) {
			node.send("ECS start");
		}
		return awaitNodes(addedServers.size(), TIMEOUT);
	}

	@Override
	public boolean stop() throws Exception {
		for (ECSNode node : addedServers) {
			node.send("ECS stop");
		}
		return awaitNodes(addedServers.size(), TIMEOUT);
	}

	@Override
	public boolean shutdown() throws Exception {
		for (int i = addedServers.size()-1; i >= 0; i--) {
			addedServers.get(i).send("ECS shutdown");
			availableServers.add(addedServers.get(i));
			addedServers.remove(i);
		}
		for (Process proc : processes) {
			proc.destroy();
		}
		return awaitNodes(addedServers.size(), TIMEOUT);
	}


	public String printAll() {
		int i = 0;
		String ret = "";
		for (ECSNode node : addedServers) {
			ret += (i + ": " + node.getNodeName() + " ");
			System.out.print(i + ": " + node.getNodeName() + " ");
			ret += (node.getNodeHost() + ":" + node.getNodePort() + "\n");
			System.out.println(node.getNodeHost() + ":" + node.getNodePort());

			i++;
		}
		return ret;
	}
	
	@Override
	public boolean awaitNodes(int count, int timeout) throws Exception {
		boolean serverFailed = false;
		initRunning = true;
		for (int i = ECS.this.addedServers.size()-1; i >= 0; i--) {
			ECSNode node = ECS.this.addedServers.get(i);
			try {
				node.send("ECS ping");
				//node.receive();
			} catch (IOException e) {
				serverFailed = true;
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
				serverFailed = true;
			}
		}
		initRunning = false;
		return !serverFailed;
	}

	public MetaData getMetaData() {
		return this.metadata;
	}

	@Override
    public Map<String, IECSNode> getNodes() {
		return allServers;
	}

    @Override
    public IECSNode getNodeByKey(String Key) {
		return allServers.get(Key);
	}

	@Override
	public Collection<IECSNode> setupNodes(int num_nodes, String strategy, int size) {
		initRunning = true;
		int repCount = 0;

		if (num_nodes < 0 || num_nodes > availableServers.size()) {
			System.out.println("num_nodes: " + num_nodes);
			System.out.println("availableServers: " + availableServers.size());
			return null;
		}
		try {
			while (num_nodes-- > 0) {
				ECSNode node = availableServers.get(0);
				
				Process proc;
				String path = ECS.class.getProtectionDomain().getCodeSource().getLocation().getPath();
				String serverPath = path.substring(0, path.lastIndexOf('/')) + "/m2-server.jar";
				String command = "ssh -n " + node.getNodeHost() + " nohup java -jar " + serverPath + " " + node.getNodePort() + " " + size + " " + strategy + " ERROR &";
				System.out.println(command);
				try {
					proc = run.exec(command);
					processes.add(proc);
					availableServers.remove(0);
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
				
				System.out.print("Attempting to connect to ");
				System.out.print(node.getNodeHost());
				System.out.print(':');
				System.out.print(node.getNodePort());
				while (true) {
					try {
						//node.connect();
						node.send("ECS ping");
						//node.disconnect();
						System.out.println('.');
						break;
					} catch (Exception e) {
						System.out.print('.');
					}
					Thread.sleep(100);
				}
				hashRing.addNode(node);
				addedServers.add(node);
				updateMetadata();
				
				this.sendMetaDataToServer(strategy, size);
                
				this.sleep(10);
				
				this.processBalance(true);
				/* do move by the balancer
				TimeUnit.MILLISECONDS.sleep(10);
				if (hashRing.getSize() > 1) {
					for (Map.Entry<BigInteger, ECSNode> entry : hashRing.getEntrySet()) {
						ECSNode node2 = entry.getValue();
						ECSNode smallerNode = hashRing.getSmallerNode(node2);
						smallerNode.connect();
						String sb = "ECS move " + node2.getNodeHashRange()[0] + " " + node2.getNodeHashRange()[1] + "|" +
								metadata.get(node2.getNodeName()).toString();
						smallerNode.send(sb);
						//String reply = smallerNode.receive();
					}
				}*/
				
			}
			ArrayList<IECSNode> ret = new ArrayList<IECSNode>();
			for (ECSNode node : addedServers)
				ret.add(node);

			initRunning = false;
			return ret;
		} catch (Exception e) {
			System.out.println("oof");
			e.printStackTrace();
			initRunning = false;
			return null;
		}
	}

	public void updateMetadata() throws NoSuchAlgorithmException {
		Collections.sort(addedServers);
		metadata.clear();
		if(addedServers.size() == 0){
			return;
		}

		String low = addedServers.get(addedServers.size()-1).getNodeHashRange()[1];
		for (ECSNode node : addedServers) {
			node.updateLowHash(low);
			metadata.addNode(new MDNode(node.getNodeName(), node.getNodeHost(), node.getNodePort(), node.getNodeHashRange()[0], node.getNodeHashRange()[1]));
			low = node.getNodeHashRange()[1];
		}
	}

	@Override
	public IECSNode addNode(String cacheStrategy, int cacheSize) {
		return setupNodes(1, cacheStrategy, cacheSize).iterator().next();
	}

	public IECSNode removeNode(int index) throws Exception {
		if (index < 0 || index >= addedServers.size()) {
			logger.error("No server at given index!");
			System.out.println("No server at given index");
			return null;
		}

		return removeNode(addedServers.get(index).getNodeName());
	}

	public IECSNode removeNode(String name) throws Exception {
		ECSNode removedNode = (ECSNode) allServers.get(name);

		availableServers.add(removedNode);
		addedServers.remove(removedNode);
		updateMetadata();
		
		sendMetaDataToServer(null,0);
		System.out.println("ecs remove the server:"+name);
		/*
		
		String mdUpdateMsg = "ECS update \\n" + metadata.toString();
		for (ECSNode node : addedServers) {
			node.connect();
			node.send(mdUpdateMsg);
		}*/
		hashRing.removeNode(removedNode);
		
		this.processBalance(true);
		/* do it by the balancer
		ECSNode biggerNode = hashRing.getBiggerNode(removedNode);
		biggerNode.connect();
		String sb = "ECS move " + biggerNode.getNodeHashRange()[0] + " " + biggerNode.getNodeHashRange()[1] + "|" +
				metadata.get(biggerNode.getNodeName()).toString();
		removedNode.send(sb);
        */
		TimeUnit.SECONDS.sleep(1);
		removedNode.send("ECS shutdown");
		return removedNode;
	}

	@Override
	public boolean removeNodes(Collection<String> nodeNames) throws Exception {
		for (String name : nodeNames) {
			if (removeNode(name) == null)
				return false;
		}
		return true;
	}

	@Override
	public Collection<IECSNode> addNodes(int count, String cacheStrategy, int cacheSize) {
		if (count < 0 || count > availableServers.size()) {
			logger.error("Attempting to add more servers than are available.");
			System.out.println("Attempting to add more servers than are available.");
			return null;
		}
		ArrayList<IECSNode> justAdded = new ArrayList<IECSNode>();
		for (int i=0; i<count; ++i) {
			justAdded.add(addNode(cacheStrategy, cacheSize));
		}

		return justAdded;
	}
	
	/**
	 *  to move the data , a kind of relative dynamic balance, not absolute balance
	 * will Rough move data by ecs , not the kvserver
	 * if have enough time later , do it by the kvserver 
	 */
	Runnable balancer = new Runnable() {
		public void run() {
		   processBalance(false);
		}
	};
	
	/**
	 * balance the data 
	 */
	public void processBalance(boolean forceFlag) {
		if(prcessingBalanceFlag) {
			return;
		}
		try {
			prcessingBalanceFlag = true;
			//geting all the data
			Map<String,KVObject[]> result = initData();
			//split the data
			splitData(result,forceFlag);
			initPrint();
			
		}catch(Exception e) {
			System.out.println("processBalance error : " + e.getMessage());
		}
		prcessingBalanceFlag = false;
		
	}
	
	/**
	 * get all the data from all db
	 * @return
	 */
	public Map<String,KVObject[]> initData() {
		Map<String,KVObject[]> result = new HashMap<String,KVObject[]>();

		 try {
			 Iterator<Map.Entry<String,IECSNode>>   itServerEntry = this.allServers.entrySet().iterator(); //find entries of all servers
			 while (itServerEntry.hasNext()) {
				 Entry<String,IECSNode> entry = itServerEntry.next();//entry: [server name, ECSnode]
				 IECSNode currentNode = entry.getValue();//find the ECSnode/server
				 DatabaseManager db = new DatabaseManager(currentNode.getNodeName());
				 File dbFile = new File(db.getDirectory()+db.getFilename());//open the db file of the server
				 if(dbFile.exists()) { //if the server has a db file (it there are kv paris in the db file)
					KVObject[] currentData =  db.getAllKVObjects();//get all kv pairs
					System.out.println(currentNode.getNodeName()+" size : "+currentData.length);
					result.put(entry.getKey(), currentData);// [servername, [k-v,k-v]]
					 
					 
				 }
			 }
		 }catch(Exception e) {
			 System.out.println("balancer getAllData error  :"+e.getMessage());
			 return new HashMap<String,KVObject[]>();// if error only return empty
		 }
		
		return result;
	}
	
	//split the data
	private void splitData(Map<String,KVObject[]> allNodeData,boolean forceFlag){
		
		List<KVObject> allServerData = new ArrayList<KVObject>();
		TreeSet<KVObject> allServerDataSet = new TreeSet<KVObject>();
		
		Iterator<Entry<String, KVObject[]>> iterator = allNodeData.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, KVObject[]> entry = iterator.next();
			allServerDataSet.addAll(Arrays.asList(entry.getValue()));// add all k-v pairs into the treeset
		}
		
		allServerData.addAll(allServerDataSet);//list
		
		if(!forceFlag) {//timer
			if(this.addedServers.size()<1) {//if there is no server
				//System.out.println("node size is zear , give up this time ");
				return ;
			}
			
			int avgSize = allServerData.size() / this.addedServers.size();//number of k-v pairs/ # of servers
			
			if(avgSize < 1) {
				//System.out.println("avgSize is too small , give up this time ");
				return ;
			}
			if(isNeedSplitData(allNodeData,avgSize)) { //check if there is a need to split the data
				
				System.out.println("found the server is not in blance, the balancer is begin ....");
				doBalance(allServerData,this.addedServers.size()); //start to do balancing
				System.out.println("the balancer is end ....");
			}
		}else {// addnode or removenode; line 290 and line 212
			//System.out.println("force balance begin ");
			doBalance(allServerData,this.addedServers.size());
			//System.out.println("force balance end ");
		}
		
		Iterator<Entry<String, KVObject[]>> iterator2 = allNodeData.entrySet().iterator(); //
		while (iterator2.hasNext() && this.addedServers.size() > 0) {
			Entry<String, KVObject[]> entry = iterator2.next();
			MDNode mdNode = this.metadata.get(entry.getKey());
			if(mdNode == null) {
				System.out.println("clear the server data -> "+entry.getKey());
				DatabaseManager db = new DatabaseManager(entry.getKey());
				db.clearDatabase();
			}
		}
		
		return ;
	}
	

	private boolean isNeedSplitData(Map<String,KVObject[]> allNodeData, int avgSize) {
		boolean isNeedSplit = false;
		
		Iterator<Entry<String, KVObject[]>> iterator = allNodeData.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, KVObject[]> entry = iterator.next();
			int diff = entry.getValue().length - avgSize;//# of kv-pair in a server - # avg
			
			if(Math.abs(diff) > CommonConstants.splitSize) {
				System.out.println("found the server:"+entry.getKey()+" need to processing");
				isNeedSplit = true;
				//break;
			}
		}
		
		return isNeedSplit;
	}

    private void doBalance (List<KVObject> allServerData,int nodeSize) { //all kv pairs and number of server
    	try {
    		
    		if(allServerData.size() < 1) {
        		return ;
        	}
    		
    		TreeSet <String> keys = new TreeSet <String>();
        	
        	TreeSet <String> newServerKeys = new TreeSet <String>();

        	for(KVObject kv : allServerData) {
        		keys.add(Hashing.getMd5(kv.getKey())); // find hash value of kv pair and add their hash value to the tree
        	}
        	
        	//Divide the set into {nodeSize} parts
        	int partsSize = keys.size() / nodeSize; // 9/3 =3 in order to find the hashstart of the server later
        	int position = 0;
        	
        	for(String key : keys) { //loop through the tree , key: hash value
        		position++;
        		System.out.print(key+" ");
        		if(position % partsSize == 0) { // 3 6 9
        			//System.out.println("################");
        			newServerKeys.add(key); // save new server start point hash value into a tree
        		}
        	}
        	System.out.println("newServerKeys : " + newServerKeys.toString());
        	
        	if(newServerKeys.size() < 1) {
        		return ;
        	}
        	// reset the servers (by using the new hash start)
        	for(ECSNode node : addedServers) { //loop all server
        		String newKey = newServerKeys.pollFirst(); //pop out the hash value
        		node.updateHighHash(newKey); // set the hash start of the server to be this hash value: each server correspond to each new hash start
        		System.out.println("update "+node.getNodeName()+" hign key :" + newKey);
        	}
        	
        	this.updateMetadata();
        	
        	this.sendMetaDataToServer(null, 0); // init the re organized server
        	
        	//prepare to move data
        	Map<String,List<KVObject>> allNewNodeData= new HashMap<String,List<KVObject>>();//new hashmap
        	for (KVObject kv : allServerData) { //loop all the kv pairs
        		
        		for (ECSNode node : addedServers) {
            		if(Hashing.isKeyInRange(kv.getKey(), node.getNodeHashRange()[0], node.getNodeHashRange()[1])) {
            			List<KVObject> kvs = allNewNodeData.get(node.getNodeName());
            			if(kvs == null) {
            				kvs = new ArrayList<KVObject>();
            			}
        				kvs.add(kv);
        				allNewNodeData.put(node.getNodeName(), kvs); // new hashmap: servername, kv pairs
        				break;
            		}
            	}
        	}
        	
        	//rewrite data to db
        	Iterator<Entry<String, List<KVObject>>> iterator = allNewNodeData.entrySet().iterator(); //loop through hashmap
        	while(iterator.hasNext()) {
        		Entry<String, List<KVObject>> entry = iterator.next();
        		DatabaseManager db = new DatabaseManager(entry.getKey());
        		File dbFile = new File(db.getDirectory()+db.getFilename()); //find db for each server
        		//if(dbFile.exists()) {
				db.clearDatabase(); //clear the existing datafile
				KVObject[] kvs = new KVObject[entry.getValue().size()];
				entry.getValue().toArray(kvs);
				db.putKVObject(kvs);//put new corresponding kv pairs in
				//}
        	}
        	
    	}catch(Exception e) {
    		System.out.println("do balance error :"+ e.getMessage());
    	}

    }
    
    private void sendMetaDataToServer(String strategy, int size) throws Exception {
    	
    	for (ECSNode node1 : addedServers) {
			String message = "ECS init " + size + " " + strategy + " " + node1.getNodeName() + "\\n" + metadata.toString();
			System.out.println(message);
			node1.send(message);   
		}
    }
    
    private void initPrint() {
    	System.out.println("");
    	System.out.println("");
    	this.sleep(1000);
    	System.out.print("ECSClient> ");
    }
    
    private void sleep (long time) {
    	try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			//e.printStackTrace();
		}
    }

	public HashMap<String, IECSNode> getAllServers() {
		return allServers;
	}
}
