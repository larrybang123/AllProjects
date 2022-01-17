package client;

import app_kvServer.app_kvDatabase.KVObject;
import org.apache.log4j.Logger;
import shared.communication.CommunicationChannel;
import shared.hashing.Hashing;
import shared.messages.KVMessage;
import shared.messages.KVMessage.StatusType;
import shared.messages.Message;
import shared.metadata.MDNode;
import shared.metadata.MetaData;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class KVStore implements KVCommInterface {
	private static Logger logger = Logger.getRootLogger();
	private Socket serverSocket;
	private CommunicationChannel comm;
	private String serverAddress;
	private int serverPort;
	public MetaData metadata = null;
	private Hashing hasher;

	/**
	 * Initialize KVStore with address and port of KVServer
	 * @param address the address of the KVServer
	 * @param port the port of the KVServer
	 */
	public KVStore(String address, int port) {
		this.serverAddress = address;
		this.serverPort = port;
		this.serverSocket = null;
		this.comm = null;
		this.hasher = new Hashing();
	}

	@Override
	public void printDcHelp() {
		if (metadata != null)
			System.out.println("Other servers you can try connecting to:");
		for (MDNode v : metadata.getData().values()) {
			System.out.println(v.getName() + " " + v.getHost() + ":" + v.getPort());
		}
	}

	@Override
	public ArrayList<String []> getReconnectList() {
//		if (metadata != null)
//			System.out.println("Other servers you can try connecting to:");
		ArrayList<String []> ret = new ArrayList<String []>();
		for (MDNode v : metadata.getData().values()) {
			String [] add = {v.getHost(), Integer.toString(v.getPort())};
			ret.add(add);
		}
		return ret;
	}

	@Override
	public void connect() throws Exception {
		serverSocket = new Socket(this.serverAddress, this.serverPort);
		serverSocket.setSoTimeout(5000);
		this.comm = new CommunicationChannel(this.serverSocket);
	}

	@Override
	public void disconnect() {
		try {
			this.serverSocket.close();
			this.comm.closeChannel();
		} catch (IOException ioe) {
			logger.error("Could not disconnect session: " + ioe.getCause().getMessage());
		}
		
	}

	@Override
	public KVMessage put(String key, String value) throws Exception {
	
		MDNode serverMD = null;
		Socket tempSocket = this.serverSocket;
		CommunicationChannel tempComm = this.comm;

		if (this.metadata != null) {
			String correctServerName = Hashing.findServerInRange(key, this.metadata, true);
			serverMD = this.metadata.get(correctServerName);
			if (!(serverMD.getHost() == this.serverAddress && this.serverPort == serverMD.getPort())) {
				tempSocket = new Socket(serverMD.getHost(), serverMD.getPort());
				tempSocket.setSoTimeout(1000000);
				tempComm = new CommunicationChannel(tempSocket);
				System.out.println(" server : "+correctServerName);
			}
		}
		// generate the put request
        logger.info("Sending PUT request for " + KVObject.printableString(key, value));
		//System.out.println("sending put");
		Message request = new Message(key, value, StatusType.PUT);
		// send the put request
		tempComm.sendMessage(request);
		// receive message response
		String response;
		KVMessage responseMsg;
		try {
			response = tempComm.receive();
			logger.info("Received reply for PUT request.");
			String[] split = response.split("\\n+", 2);
			responseMsg = new Message(split[0]);
			if (responseMsg.getStatus() == StatusType.SERVER_NOT_RESPONSIBLE) {
				//System.out.println("server not responsible");
				System.out.println(response.substring(response.indexOf('\\') + 2));
				this.metadata = new MetaData(response.substring(response.indexOf('\\') + 2));
				//System.out.println("made new metadata");
				responseMsg = this.put(key, value);
			}
		} catch (SocketTimeoutException ste) {
			logger.info("Server took too long to respond. Assume disconnection occurred.");
			throw new Exception("Socket Timeout.");
		}
		if (serverMD != null) {
			if (!(serverMD.getHost() == this.serverAddress && this.serverPort == serverMD.getPort())) {
				tempSocket.close();
				tempComm.closeChannel();
			}
		}
		// return response
		return responseMsg;
	}

	@Override
	public KVMessage get(String key) throws Exception {
		MDNode serverMD = null;
		Socket tempSocket = this.serverSocket;
		CommunicationChannel tempComm = this.comm;

		if (this.metadata != null) {
			String correctServerName = Hashing.findServerInRange(key, this.metadata, false);
			serverMD = this.metadata.get(correctServerName);
			if (!(serverMD.getHost() == this.serverAddress && this.serverPort == serverMD.getPort())) {
				tempSocket = new Socket(serverMD.getHost(), serverMD.getPort());
				tempSocket.setSoTimeout(5000);
				tempComm = new CommunicationChannel(tempSocket);
			}
		}
		
		// generate the get request
        logger.info("Sending GET request for " + key);
		Message request = new Message(key, null, StatusType.GET);
		
		// send the get request
		tempComm.sendMessage(request);

		String response;
		KVMessage responseMsg;
		try {
			response = tempComm.receive();
			logger.info("Received reply for PUT request.");
			String[] split = response.split("\\n+", 2);
			responseMsg = new Message(split[0]);
			if (responseMsg.getStatus() == StatusType.SERVER_NOT_RESPONSIBLE) {
				this.metadata = new MetaData(response.substring(response.indexOf('\\') + 2));
				responseMsg = this.get(key);
			}
		} catch (SocketTimeoutException ste) {
			logger.info("Server took too long to respond. Assume disconnection occurred.");
			throw new Exception("Socket Timeout.");
		}

		//System.out.println("got info");
		System.out.println(responseMsg.toString());
		if (serverMD != null) {
			if (!(serverMD.getHost() == this.serverAddress && this.serverPort == serverMD.getPort())) {
				tempSocket.close();
				tempComm.closeChannel();
			}
		}
		// return response
		return responseMsg;
	}

	public Socket getSocket() {
		return this.serverSocket;
	}
}
