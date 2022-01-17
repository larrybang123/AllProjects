package shared.metadata;

public class MDNode {


    private String name;
    private String host;
    private int port;
    private String startHash;
    private String endHash;

    public MDNode(String name, String host, int port, String start, String end) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.startHash = start;
        this.endHash = end;
	}

	
	public MDNode(String data) {
		String[] tokens = data.split("\\s+", 11);
		this.host = tokens[0].trim();
		this.port = Integer.parseInt(tokens[1]);
		this.startHash = tokens[2].trim();
		this.endHash = tokens[3].trim();
		this.name = tokens[4].trim();
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getStartHash() {
		return startHash;
	}
	
	public void setStartHash(String startHash) {
		this.startHash = startHash;
	}

	public String getEndHash() {
		return endHash;
	}

	public void setEndHash(String endHash) {
		this.endHash = endHash;
	}

	public String[] getRange() {
		return new String[] {this.startHash, this.endHash};
	}

	@Override
	public String toString() {
		String returnVal = this.host + " " + this.port + " " + this.startHash + " " + this.endHash + " " + this.name;
		return returnVal;
	}

    
}
