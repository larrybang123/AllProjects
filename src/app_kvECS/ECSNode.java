package app_kvECS;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;

import com.google.gson.JsonObject;

import shared.communication.CommunicationChannel;
import shared.hashing.Hashing;

public class ECSNode implements IECSNode, Comparable<ECSNode> {

    private String name;
    private String host;
    private int port;
    private String lowHash;
    private String highHash;
    private boolean isConnect;

    private String mainLowHash;
    private String mainHighHash;

    private Socket serverSocket = null;
    private CommunicationChannel comm = null;

    public ECSNode(String name, String host, int port, String lowHash, String highHash) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.lowHash = lowHash;
        this.highHash = highHash;
    }
    
    public JsonObject getECSNodeInfo() {
    	JsonObject info = new JsonObject();
    	info.addProperty("name", this.name);
    	info.addProperty("host", this.host);
    	info.addProperty("port", this.port);
    	info.addProperty("lowHash", this.lowHash);
    	info.addProperty("highHash", this.highHash);
    	return info;
    }
   
	public void updateHighHash(String highHash) {
		this.highHash = highHash;
	}

    public void updateLowHash(String lowHash) {
        this.lowHash = lowHash;
    }


    /**
     * @return  the name of the node (ie "Server 8.8.8.8")
     */
    @Override
    public String getNodeName() {
        return this.name;
    }

    /**
     * @return  the hostname of the node (ie "8.8.8.8")
     */
    @Override
    public String getNodeHost() {
        return this.host;
    }

    /**
     * @return  the port number of the node (ie 8080)
     */
    @Override
    public int getNodePort() {
        return this.port;
    }

    /**
     * @return  array of two strings representing the low and high range of the hashes that the given node is responsible for
     */
    @Override
    public String[] getNodeHashRange() {
        String[] ret = {this.lowHash, this.highHash};
		return ret;
    }

    public boolean isConnect() {
        return isConnect;
    }

    public void connect() throws Exception {
        if (!isConnect) {
            serverSocket = new Socket(this.host, this.port);
            serverSocket.setSoTimeout(10000);
            this.comm = new CommunicationChannel(this.serverSocket);
            isConnect = true;
        }
	}

	public void disconnect() {
		try {
			this.serverSocket.close();
			this.comm.closeChannel();
			isConnect = false;
		} catch (IOException ioe) {
            ioe.printStackTrace();
		}
    }

    public void send(String message) throws Exception {
    	this.connect();
        this.comm.send(message.getBytes());
        this.disconnect();
    }
    
    public String sendAndReceive(String message) throws Exception {
    	this.connect();
        this.comm.send(message.getBytes());
        String result = this.comm.receive();
        this.disconnect();
        
        return result;
    }
    /*
    public String receive() throws Exception {
    	this.connect();
        String result = this.comm.receive();
        this.disconnect();
        return result;
    }*/

    // For sorting based on highHash
    @Override
    public int compareTo(ECSNode other) {
        return Hashing.isHashGreaterThan(this.highHash, other.highHash) == 1 ? 1 : -1;
    }

    public void setNodeHashRange(BigInteger start, BigInteger end) {
        this.lowHash = start.toString(16);
        this.highHash = end.toString(16);
    }

    public String getNodeHashName() {
        return this.host + ":" + this.port;
    }

	@Override
	public String toString() {
		return "ECSNode [name=" + name + ", host=" + host + ", port=" + port + ", lowHash=" + lowHash + ", highHash="
				+ highHash + "]";
	}
    
    
    
    
}
