package app_kvServer;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import app_kvServer.KVServer.ServerState;
import shared.communication.CommunicationChannel;
import shared.hashing.Hashing;
import shared.messages.KVMessage;
import shared.messages.KVMessage.StatusType;
import shared.messages.Message;
import shared.metadata.MDNode;
import shared.metadata.MetaData;


/**
 * Represents a connection end point for a particular client that is 
 * connected to the server. This class is responsible for message reception 
 * and sending. 
 */
public class ClientConnection implements Runnable {

	private static Logger logger = Logger.getRootLogger();
	private boolean isOpen;
	private KVServer server;
	private Socket clientSocket;
	private CommunicationChannel comm;	

	/**
	 * Constructs a new CientConnection object for a given TCP socket.
	 * @param clientSocket the Socket object for the client connection.
	 */
	public ClientConnection(Socket clientSocket, KVServer server) {
		this.server = server;
		this.clientSocket = clientSocket;
		this.comm = new CommunicationChannel(clientSocket);
		this.isOpen = true;
	}
	
	/**
	 * Initializes and starts the client connection. 
	 * Loops until the connection is closed or aborted by the client.
	 */
	private boolean isECSMessage(String msg) {
		return msg.split("\\s+", 2)[0].equals("ECS");
	}

	private String handleECSMessage(String msg) {
		String command = msg.split("\\s+", 3)[1];
		// Default reply
		String reply = command + " success";
		if (!msg.equals("ECS ping"))
			System.out.println("msg mcvd: " + msg);
		if (command.equals("init")) {
			int mdStart = msg.indexOf("\\n");
			//System.out.print("message: ");
			//System.out.println(msg);
			//System.out.print("mdStart: ");
			//System.out.println(mdStart);
			String[] args = msg.substring(0, mdStart).split("\\s+", 5);
			String metadata = msg.substring(mdStart+2);
			int cacheSize = Integer.parseInt(args[2]);
			String strategy = args[3];
			String name = args[4];
			System.out.println("Initing");
			server.initKVServer(name, cacheSize, strategy, new MetaData(metadata));
			System.out.println("finished initialization");
		}else if(command.equals("allServerInfo")) { 
			/*
			 * String allServerInfo = msg.replaceAll("ECS allServerInfo ", ""); JsonParser
			 * parser = new JsonParser(); JsonArray serverInfo =
			 * parser.parse(allServerInfo).getAsJsonArray();
			 * this.server.setAllServerInfo(serverInfo);
			 * System.out.println("update serverInfo : "+serverInfo);
			 */
		}else if (command.equals("start")) {
			server.startServer();
		} else if (command.equals("stop")) {
			server.stopServer();
		} else if (command.equals("shutdown")) {
			server.shutdown();
		} else if (command.equals("lock")) {
			server.lockWrite();
		} else if (command.equals("unlock")) {
			server.unlockWrite();
		} else if (command.equals("kill")) {
			server.kill();
		} else if (command.equals("move")) {
			// Format: ECS move <start> <end> <\n> MDNode string
			String[] lines = msg.split("\\|");
			String[] args = lines[0].split("\\s+");
			String start = args[2];
			String end = args[3];
			MDNode node = new MDNode(lines[1]);
			if (!server.moveData(start, end, node))
				reply = "move fail";
		} else if (command.equals("receive")) {
			String[] lines = msg.split(("\\\\n+"));
//			KVObject[] kvObjs = new KVObject[lines.length-1];
			System.out.println("rcvd len: " + lines.length);
			System.out.println("rcvd: ");
			for (int i = 1; i < lines.length; ++i) {
				System.out.println(" - " + lines[i]);
				//kvObjs[i-1] = new KVObject(lines[i]);
				try {
					if (lines[i].length() > 2)
						server.putKV(lines[i].split(" ")[0], lines[i].split(" ")[1]);
				} catch (Exception e) {
					System.out.println("Couldn't PUT");
					e.printStackTrace();
				}
			}
//			if (!server.receive(kvObjs))
//				reply = "receive fail";
		} else if (command.equals("update")) {
			int mdStart = msg.indexOf('\\');
			String metadata = msg.substring(mdStart+2);
			server.updateMetadata(new MetaData(metadata));
		} else if (command.equals("getStatus")) {
			reply = "status " + server.getServerState();
		} else if (command.equals("ping")) {
			reply = "pong";
		} else if (command.equals("clear")) {
			server.clearStorage();
		} else if (command.equals("put")) {
			String key = msg.split("\\s+", 4)[2];
			String value = msg.split("\\s+", 4)[3];
			System.out.println("PUT " + key + " " + value);
			boolean isDelete = isDeleteRequest(value);
			try {
				int prevSize = server.getDatabase().getSize();
				server.putKV(key, value);
				/*if (isDelete)
					statusType = StatusType.DELETE_SUCCESS;
				else if (server.getDatabase().getSize() == prevSize)
					statusType = StatusType.PUT_UPDATE;
				else
					statusType = StatusType.PUT_SUCCESS;*/
				reply = "PUT_SUCCESS";
			} catch (Exception e) {
				reply = "PUT_ERROR";
/*				if (isDelete)
					statusType = StatusType.DELETE_ERROR;
				else
					statusType = StatusType.PUT_ERROR;*/
			}


			//reply = "PUT_SUCCESS";
		} else {
			//System.out.println("message is: " + command);
			reply = command;
		}
		return reply;
	}

	private Message handleMessage(Message msg) throws NoSuchAlgorithmException {
		Message reply;
		//System.out.println("in hadnlemsg");
		if (server.getServerState() == ServerState.STOPPED) {
			reply = new Message(null, null, StatusType.SERVER_STOPPED);
			return reply;
		}
		
		if (!server.isKeyInRange(msg.getKey()) ) {
			System.out.println("error -> Don't put here");
			reply = new Message(null, null, StatusType.SERVER_NOT_RESPONSIBLE);
			return reply;
		}
		
		KVMessage.StatusType statusType = null;
		switch (msg.getStatus()) {
			case GET:
				String value = null;
				try {
					value = server.getKV(msg.getKey());
					statusType = StatusType.GET_SUCCESS;
				} catch (Exception e) {
					value = null;
					statusType = StatusType.GET_ERROR;
				} finally {
					reply = new Message(msg.getKey(), value, statusType);
					break;
				}
			case PUT:
				System.out.println("inP");
				if (server.isWriterLocked()) {
					reply = new Message(msg.getKey(), msg.getValue(), StatusType.SERVER_WRITE_LOCK);
					break;
				}
				boolean isDelete = isDeleteRequest(msg.getValue());
				try {
					int prevSize = server.getDatabase().getSize();
					server.putKV(msg.getKey(), msg.getValue());
					if (isDelete)
						statusType = StatusType.DELETE_SUCCESS;
					else if (server.getDatabase().getSize() == prevSize)
						statusType = StatusType.PUT_UPDATE;
					else
						statusType = StatusType.PUT_SUCCESS;
				} catch (Exception e) {
					if (isDelete)
						statusType = StatusType.DELETE_ERROR;
					else
						statusType = StatusType.PUT_ERROR;
				} finally {
					reply = new Message(msg.getKey(), msg.getValue(), statusType);
					break;
				}
			default:
		System.out.println("5");
				reply = null;
				break;
		}

		return reply;
	}


	/**
	 * Closes the client connection
	 */
	public void close() throws IOException {
		//if (clientSocket != null && clientSocket.isConnected())
		//	logger.info("Closing client connection to " + clientSocket.getInetAddress().getHostAddress() + " on port " + clientSocket.getPort());
		if (this.comm != null)
			this.comm.closeChannel();
		this.isOpen = false;
	}

	private boolean isDeleteRequest(String value) {
        return value == null || value.isEmpty() || value.equals("null");
    }

	public void run() {
		try {
			while(isOpen) {
				try {
					/*Message latestMsg = this.comm.receiveMessage();
					if (latestMsg == null) {
						throw new IOException("Unable to receive new message.");
					}

					Message reply = this.handleMessage(latestMsg);
					if (reply == null) {
						throw new IOException("Invalid message sent to server.");
					} else {
						this.comm.sendMessage(reply);
					}*/
					String latestMsg = this.comm.receive();
					//System.out.println("latestMsg:"+latestMsg);
					if (latestMsg == null) {
						throw new IOException("Unable to receive new message.");
					}

					if (latestMsg.equals("")) {
						//System.out.println("oof");
						continue;
					}

					if (isECSMessage(latestMsg)) {
						//System.out.println("get ECS msg");
						//System.out.println(latestMsg);
						String reply = this.handleECSMessage(latestMsg);
						if (reply == null)
							throw new IOException("Invalid message sent to server.");
						else
							this.comm.send(reply.getBytes());
					} else {
						//System.out.println("got KV message");
						//System.out.println(latestMsg);
						Message reply = this.handleMessage(new Message(latestMsg));
						System.out.println(reply.toString());
						if (reply == null)
							throw new IOException("Invalid message sent to server.");
						else {
							//logger.info("plz status");
							if (reply.getStatus() == StatusType.SERVER_NOT_RESPONSIBLE) {
								//System.out.println("pepis1");
								StringBuilder sb = new StringBuilder();
								sb.append(reply.toString(), 0, reply.toString().length() - 1);
								sb.append("\\n");
								sb.append(server.getMetadata().toString());
								//System.out.println("pepis2: " + sb.toString());
								this.comm.send(sb.toString().getBytes());
							} else {
								this.comm.send(reply.toByteArray());
							}
						}
					}

					/* connection either terminated by the client or lost due to
					 * network problems*/
				} catch (IOException ioe) {
					//logger.error(ioe.getMessage());
					logger.info("Connection end!");
					isOpen = false;
				} catch (IllegalArgumentException | NoSuchAlgorithmException iae) {
					logger.info("Client " + this.clientSocket.getInetAddress().getHostName() + " has disconnected.");
					System.out.println(iae.toString());
					isOpen = false;
				}
			}

		} finally {
			try {
				if (clientSocket != null) {
					this.comm.closeChannel();
					clientSocket.close();
					clientSocket = null;
				}
			} catch (IOException ioe) {
				logger.error("Error! Unable to tear down connection!", ioe);
			}
		}
	}
}
