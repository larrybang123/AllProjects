package app_kvClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import app_kvServer.app_kvDatabase.KVObject;
import logger.LogSetup;
import shared.messages.KVMessage;
import client.KVCommInterface;
import client.KVStore;

public class KVClient implements IKVClient {
    
    private static Logger logger = Logger.getRootLogger();
    
    private static final String PROMPT = "KVClient> ";
    private BufferedReader stdin;

    private KVCommInterface kvStore = null;
    private String serverAddress;
    private int serverPort;

//	private HashMap<String, int> serverList;

    private boolean connected = false;
    private boolean exiting = false;

    @Override
    public void newConnection(String hostname, int port) throws Exception {
        if (this.isConnected()) {
            // Tear down old connection
            this.disconnect();
        }

        // Establish new connection
        this.serverAddress = hostname;
        this.serverPort = port;
        this.connect();
    }

    @Override
    public KVCommInterface getStore(){
        //if (kvStore == null) {
        //    return new KVStore(this.serverAddress, this.serverPort);
        //}
        return kvStore;
    }

	/**
	 * Connect to the server
	 * @throws Exception if connection attempt fails
	 */
    public void connect() throws Exception {
        if (this.isConnected())
			return;
		try {
			if (this.kvStore == null)
            this.kvStore = new KVStore(this.serverAddress, this.serverPort);
			kvStore.connect();
			this.connected = true;
			Socket socket = ((KVStore) kvStore).getSocket();
			System.out.println("Successfully connected to " + socket.getInetAddress().getHostName() + " at port " + socket.getPort());
		} catch (Exception e) {
			kvStore = null;
			this.connected = false;
			throw e;
		}
        
    }

	/**
	 * Disconnect from the server
	 */
    public void disconnect() {
        if (!this.isConnected())
            return;
		kvStore.disconnect();
		Socket socket = ((KVStore) kvStore).getSocket();
		System.out.println("Disconnected from " + socket.getInetAddress().getHostName());
        kvStore = null;
        this.connected = false;
    }

    public boolean isConnected() {
        return this.connected;
    }

	/**
	 * Called if connection unexpectedly terminates
	 */
    public void handleConnectionFailure() {
        printError("Connection to server lost! Please reconnect before attempting to communicate with the server again.");
		//this.kvStore.printDcHelp();
		ArrayList<String[]> connectionOptions = this.kvStore.getReconnectList();
        this.disconnect();
		for (String [] option : connectionOptions) {
			boolean connectSuccess = false;
			try {
				this.serverAddress = option[0];
				this.serverPort = Integer.parseInt(option[1]);
				connect();
				connectSuccess = true;
			} catch(Exception e) {
				connectSuccess = false;
			}
			if (connectSuccess) {
				System.out.println("Reconnected to " + option[0] + ":" + Integer.parseInt(option[1]));
				break;
			}
		}
    }

	/**
	 * Called after client receives a message from the server
	 * @param msg
	 */
    public void handleServerReply(KVMessage msg) {
        switch(msg.getStatus()) {
            case GET_SUCCESS:
                print(msg.getValue());
                break;
            case GET_ERROR:
                printError("No database entry for " + msg.getKey());
                break;
            case PUT_SUCCESS:
            case PUT_UPDATE:
                print("Successfully added " + KVObject.printableString(msg.getKey(), msg.getValue()) + " to server database.");
                break;
            case PUT_ERROR:
                printError("Unable to add " + KVObject.printableString(msg.getKey(), msg.getValue()) + " to server database.");
                break;
            case DELETE_SUCCESS:
                print("Successfully deleted " + msg.getKey() + " from server database.");
                break;
            case DELETE_ERROR:
                printError("Unable to delete " + msg.getKey() + " from server database.");
				break;
            default:
                printError("Invalid reply status.");
				System.out.println(msg.toString());
                break;
        }
    }

	/**
	 * Print message to console
	 * @param msg the message to print
	 */
    private void print(String msg) {
        System.out.println(PROMPT + msg);
    }

	/**
	 * Print error message to console
	 * @param error the error message to print
	 */
    private void printError(String error){
		System.out.println(PROMPT + "Error! " +  error);
	}

    public void run() {
//		this.serverList = new HashMap<>();
		while(!exiting) {
			stdin = new BufferedReader(new InputStreamReader(System.in));
			System.out.print(PROMPT);
			
			try {
				String cmdLine = stdin.readLine();
				this.handleCommand(cmdLine);
			} catch (Exception e) {
				//exiting = true;
				printError("CLI does not respond - Application terminated ");
			}
		}
	}
	
	/**
	 * Parse client input and perform appropriate action
	 * @param cmdLine the client input line
	 */
	public void handleCommand(String cmdLine) {
		String[] tokens = cmdLine.split("\\s+");

		if(tokens[0].equals("quit")) {	
            this.exiting = true;
			disconnect();
			System.out.println(PROMPT + "Application exit!");
		
		} else if (tokens[0].equals("connect")){
			if(tokens.length == 3) {
				try{
					//this.serverAddress = tokens[1];
					//this.serverPort = Integer.parseInt(tokens[2]);
					//this.newConnection(this.serverAddress, this.serverPort);
					this.newConnection(tokens[1], Integer.parseInt(tokens[2]));
				} catch(NumberFormatException nfe) {
					printError("No valid address. Port must be a number!");
					logger.info("Unable to parse argument <port>", nfe);
				} catch (UnknownHostException e) {
					printError("Unknown Host!");
					logger.info("Unknown Host!", e);
				} catch (Exception e) {
					kvStore = null;
					printError(e.getMessage());
					printError("Could not establish connection!");
					logger.warn("Could not establish connection!", e);
				}
			} else {
				printError("Invalid number of parameters!");
			}
			
		} else if(tokens[0].equals("disconnect")) {
            if (!this.isConnected())
                System.out.println("You are not connected to a server.");
			disconnect();
			
		} else if(tokens[0].equals("logLevel")) {
			if(tokens.length == 2) {
				String level = setLevel(tokens[1]);
				if(level.equals(LogSetup.UNKNOWN_LEVEL)) {
					printError("No valid log level!");
					printPossibleLogLevels();
				} else {
					System.out.println(PROMPT + 
							"Log level changed to level " + level);
				}
			} else {
				printError("Invalid number of parameters!");
			}
			
		} else if (tokens[0].equals("help")) {
			printHelp();
		} else if (tokens[0].equals("put")) {
			if(tokens.length > 1) {
				String valueString = "";
				for (int i = 2; i < tokens.length; i++) {
					if (i != 2) {
						valueString += " ";
					}
					valueString += tokens[i];
				}
				if(tokens[1].getBytes().length > 20) {
					printError("Key cannot be over 20 bytes!");
				} else if (valueString.getBytes().length > 120 * 1024) {
					printError("Value cannot be over 120 kilobytes!");
                } else if (!this.isConnected()) {
                    printError("Not connected to server!");
                } else {
					try {
                        this.handleServerReply(kvStore.put(tokens[1], valueString));
                    } catch (Exception e) {
						e.printStackTrace();
                        this.handleConnectionFailure();
					}
				}
			} else {
				printError("Invalid number of parameters!");
			}
		} else if (tokens[0].equals("get")) {
			if(tokens.length == 2) {
				if(tokens[1].getBytes().length > 20) {
					printError("Key cannot be over 20 bytes!");
				} else if (!this.isConnected()) {
                    printError("Not connected to server!");
                } else {
                    try {
						System.out.println("trying to get");
                        this.handleServerReply(kvStore.get(tokens[1]));
                    } catch (Exception e) {
                        this.handleConnectionFailure();
					}

				}
			} else {
				printError("Invalid number of parameters!");
			}
		} else {
			printError("Unknown command");
			printHelp();
		}
    }

	/**
	 * Print the help list of available commands
	 */
    private void printHelp() {
		StringBuilder sb = new StringBuilder();
		sb.append(PROMPT).append("KV CLIENT HELP (Usage):\n");
		sb.append(PROMPT);
		sb.append("::::::::::::::::::::::::::::::::");
		sb.append("::::::::::::::::::::::::::::::::\n");
		sb.append(PROMPT).append("connect <host> <port>");
		sb.append("\t establishes a connection to a server\n");
		sb.append(PROMPT).append("disconnect");
		sb.append("\t\t\t disconnects from the server \n");
		sb.append(PROMPT).append("put <key> <value>");
		sb.append("\t\t\t sends a key-value pair to be stored or updated on the server. Deletes existing KV if value is null.\n");
		sb.append(PROMPT).append("get <key>");
		sb.append("\t\t\t get the value of the KV pair with key <key>\n");

		sb.append(PROMPT).append("logLevel <level>");
		sb.append("\t\t\t changes the logLevel \n");
		sb.append(PROMPT).append("\t\t\t\t ");
		sb.append("ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \n");
		
		sb.append(PROMPT).append("quit");
		sb.append("\t\t\t exits the program. Automatically disconnects from server if connected.");
		System.out.println(sb.toString());
	}
	
	private void printPossibleLogLevels() {
		System.out.println(PROMPT 
				+ "Possible log levels are:");
		System.out.println(PROMPT 
				+ "ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF");
	}
	
	/**
	 * Set Log Level
	 * @param levelString the level to set to
	 */
    private String setLevel(String levelString) {
		if(levelString.equals(Level.ALL.toString())) {
			logger.setLevel(Level.ALL);
			return Level.ALL.toString();
		} else if(levelString.equals(Level.DEBUG.toString())) {
			logger.setLevel(Level.DEBUG);
			return Level.DEBUG.toString();
		} else if(levelString.equals(Level.INFO.toString())) {
			logger.setLevel(Level.INFO);
			return Level.INFO.toString();
		} else if(levelString.equals(Level.WARN.toString())) {
			logger.setLevel(Level.WARN);
			return Level.WARN.toString();
		} else if(levelString.equals(Level.ERROR.toString())) {
			logger.setLevel(Level.ERROR);
			return Level.ERROR.toString();
		} else if(levelString.equals(Level.FATAL.toString())) {
			logger.setLevel(Level.FATAL);
			return Level.FATAL.toString();
		} else if(levelString.equals(Level.OFF.toString())) {
			logger.setLevel(Level.OFF);
			return Level.OFF.toString();
		} else {
			return LogSetup.UNKNOWN_LEVEL;
		}
    }
    
    /**
     * Main entry point for the kv client application. 
     * @param args contains the port number at args[0].
     */
    public static void main(String[] args) {
        try {
            new LogSetup("logs/client.log", Level.OFF);
            KVClient app = new KVClient();
            app.run();
        } catch (IOException e) {
            System.out.println("Error! Unable to initialize logger!");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
