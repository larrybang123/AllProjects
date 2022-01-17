package app_kvServer.app_kvDatabase;

/**
 * This class represents a kv database object. It has a key and a value 
 * and methods to manipulate them.
 */
public class KVObject implements Comparable{
    private String key;
    private String value;

    private final String seperator = " ";

    /**
     * Constructs a new kv object from a key and a value
     * @param k the key
     * @param v the value
     */
    public KVObject(String k, String v) {
        this.key = k;
        this.value = v;
    }

    /**
     * Constructs a new kv object from a simple string representation
     * @param s
     */
    public KVObject(String s) {
        String[] kv = s.split("\\s+", 2);
        this.key = kv[0];
        this.value = kv[1];
    }

    /**
     * Get the key for the kv object
     * @return the key
     */
    public String getKey() {
        return this.key;
    }

    /**
     * Get the value for the kv object
     * @return the value
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Set the key for the kv object
     * @param k the key
     */
    public void setKey(String k) {
        this.key = k;
    }

    /**
     * Set the value for the kv object
     * @param v the value
     */
    public void setValue(String v) {
        this.value = v;
    }

    /**
     * Convert the kv object to a simple space delimited string representation
     * @return the string representation
     */
    @Override
    public String toString() {
        return new String(this.key + this.seperator + this.value);
    }

    /**
     * Convert the kv object to a string suitable for printing to console/logging
     * @return the string representation
     */
    public String printableString() {
        return new String("<" + this.key + ", " + this.value + ">");
    }

    /**
     * Static version of printableString() so that object need not be instantiated
     * @param key the key
     * @param value the value
     * @return the string representation
     */
    public static String printableString(String key, String value) {
        return new String("<" + key + ", " + value + ">");
    }

	@Override
	public int compareTo(Object o) {
		KVObject on = (KVObject)o;
		return this.getKey().compareTo(on.getKey());
	}
}