package shared.messages;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

// import java.json.Json;
// import javax.json.Json;
// import javax.json.JsonObject;
// import javax.json.JsonReader;
// import javax.json.JsonWriter;

// import org.json.JSONObject;

public class Message implements KVMessage {

	String key;
	String value;
	StatusType status;

	/**
	 * Constructs a new Message object from a byte array
	 * @param msg the byte array representing the message
	 */
	public Message(byte[] msg) {
		// JSONObject obj = new JSONObject(msg.toString());
		// this.key = ogj.getString("key");
		// this.value = ogj.getString("value");
		// this.status = ogj.getInt("status");

		String[] tokens = new String(msg).split("\\s+", 3);
		this.status = StatusType.valueOf(tokens[0]);
		this.key 	= tokens[1];
		this.value 	= tokens[2];
	}

	/**
	 * Constructs a new Message object from a string
	 * @param msg the string representing the message
	 */
	public Message(String msg) {
		// JSONObject obj = new JSONObject(msg);
		// this.key = ogj.getString("key");
		// this.value = ogj.getString("value");
		// this.status = ogj.getInt("status");

		String[] tokens = msg.split("\\s+", 3);
		this.status = StatusType.valueOf(tokens[0]);
		this.key 	= tokens[1];
		this.value 	= tokens[2];
	}

	/**
	 * Constructs a new Message object from a key, value and status
	 * @param k = key, v = value, st = status
	 */
	public Message(String k, String v, StatusType st) {
		this.key 	= k;
		this.value 	= v;
		this.status = st;
	}

	/**
	 * Converts the message object to a byte array
	 * @return a byte array representing the message
	 */
	public byte[] toByteArray() {
		// return gson.toJson(this).getBytes();
		byte[] msg = this.toString().getBytes();
		return msg;
	}

	/**
	 * Converts the message object to a string
	 * @return a string representing the message
	 */
	@Override
	public String toString() {
		// return gson.toJson(this);
		String msg = new String(this.status.name() + " "  + this.key + " " + this.value);
		return msg;
	}

	@Override
	public String getKey() {
		return this.key;
	}
	
	@Override
	public String getValue() {
		return this.value;
	}
	
	@Override
	public StatusType getStatus() {
		return this.status;
	}
	
}


