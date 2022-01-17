package shared.communication;

import shared.messages.*;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * This class is responsible for communication with a socket.
 * It is used to read from or write to a socket.
 */
public class CommunicationChannel {

	private Logger logger = Logger.getRootLogger();
	
	private Socket socket;
	private OutputStream output;
 	private InputStream input;
	
	private static final int BUFFER_SIZE = 1024;
	private static final int DROP_SIZE = 1024 * BUFFER_SIZE;
	
	/**
	 * Creates new Communication Channel with a given socket
	 * @param socket socket to connect with
	 */
	public CommunicationChannel(Socket socket) {
		this.socket = socket;
		try {
			output = this.socket.getOutputStream();
			input = this.socket.getInputStream();
		} catch (IOException ioe) {
			logger.info("Could not establish input/output stream");
		}
		logger.info("Connection established");
	}

	/**
	 * Creates new Communication Channel with socket address and port number
	 * @param address socket address
	 * @param port socket port number
	 */
	public CommunicationChannel(String address, int port) 
			throws UnknownHostException, IOException {
		
		this.socket = new Socket(address, port);
		output = this.socket.getOutputStream();
		input = this.socket.getInputStream();
		logger.info("Connection established");
	}

	// Use this method to send a generic byte[] message
	public void send(byte[] msgBytes) throws IOException {
		int msgLength = msgBytes.length;
		DataOutputStream dos = new DataOutputStream(output);
		dos.writeInt(msgLength);
		dos.flush();
		// send actual message
		output.write(msgBytes, 0, msgBytes.length);
		output.flush();
		
	}

	// Use this method to receive a generic string message
	public String receive() throws IOException {
		byte[] messageBuffer = new byte[BUFFER_SIZE];
		DataInputStream dataInputStream = new DataInputStream(input);
		int messageSize = dataInputStream.readInt();
		byte[] byteMessage = new byte[messageSize];
		int numBufferNeeded = messageSize / BUFFER_SIZE;
		int numBytesLeft = messageSize % BUFFER_SIZE;

		for (int i = 0; i < numBufferNeeded; i++) {
			try {
				int numBytesRead = input.read(messageBuffer, 0, BUFFER_SIZE);
				assert(numBytesRead == BUFFER_SIZE);
			} catch (IOException ioe) {
				logger.error("Error! Failed to read message." + ioe);
			}
			System.arraycopy(messageBuffer, 0, byteMessage, i * BUFFER_SIZE, BUFFER_SIZE);

			messageBuffer = new byte[BUFFER_SIZE];
		}

		try {
			int numBytesRead = input.read(messageBuffer, 0, numBytesLeft);
			assert(numBytesRead == numBytesLeft);
		} catch (IOException ioe) {
			logger.error("Error! Failed to read message." + ioe);
		}
		System.arraycopy(messageBuffer, 0, byteMessage, numBufferNeeded * BUFFER_SIZE, numBytesLeft);
		return new String(byteMessage);
	}


	/**
	 * Method sends a Message to the socket connected to.
	 * @param msg the Message that is to be sent.
	 * @throws IOException some I/O error regarding the output stream 
	 */


	public void sendMessage(Message msg) throws IOException {
		byte[] msgBytes = msg.toByteArray();
		int msgLength = msgBytes.length;
		DataOutputStream dos = new DataOutputStream(output);
		dos.writeInt(msgLength);
		dos.flush();

		output.write(msgBytes, 0, msgBytes.length);
		output.flush();
		String msgString = new String(msgBytes);
		logger.info("SEND \t<" 
				+ socket.getInetAddress().getHostAddress() + ":" 
				+ socket.getPort() + ">: '" 
				+ msgString +"'");
    }
	
	/**
	 * Receives a Message from the socket connected to
	 * @return the Message object received
	 * @throws IOException
	 */
	public Message receiveMessage() throws IOException {
		int index = 0;
		byte[] msgBytes = null, tmp = null;
		byte[] bufferBytes = new byte[BUFFER_SIZE];
		
		/* read first char from stream */
		byte read = (byte) input.read();	
		boolean reading = true;
		
		int i = 0;
		while(read != -1 && read != 0 && reading) {
			/* carriage return */
			/* if buffer filled, copy to msg array */
			if(index == BUFFER_SIZE) {
				if(msgBytes == null){
					tmp = new byte[BUFFER_SIZE];
					System.arraycopy(bufferBytes, 0, tmp, 0, BUFFER_SIZE);
				} else {
					tmp = new byte[msgBytes.length + BUFFER_SIZE];
					System.arraycopy(msgBytes, 0, tmp, 0, msgBytes.length);
					System.arraycopy(bufferBytes, 0, tmp, msgBytes.length,
							BUFFER_SIZE);
				}

				msgBytes = tmp;
				bufferBytes = new byte[BUFFER_SIZE];
				index = 0;
			} 

			/* only read valid characters, i.e. letters and numbers */
			if((read > 31 && read < 127)) {
				bufferBytes[index] = read;
				index++;
			}
			
			/* stop reading is DROP_SIZE is reached */
			if(msgBytes != null && msgBytes.length + index >= DROP_SIZE) {
				reading = false;
			}

			/* read next char from stream */
			read = (byte) input.read();
		}

		if(msgBytes == null){
			tmp = new byte[index];
			System.arraycopy(bufferBytes, 0, tmp, 0, index);
		} else {
			tmp = new byte[msgBytes.length + index];
			System.arraycopy(msgBytes, 0, tmp, 0, msgBytes.length);
			System.arraycopy(bufferBytes, 0, tmp, msgBytes.length, index);
		}

		msgBytes = tmp;
		
		/* build final String */
		Message msg = new Message(msgBytes);
		//logger.info("Receive Message:\t '" + msg.getMsg() + "'");
		return msg;
	}
	
	/**
	 * Closes the communication channel
	 */
	public void closeChannel() throws IOException {
		input.close();
		output.close();
	}
 	
}
