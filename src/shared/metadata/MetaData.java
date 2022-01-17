package shared.metadata;

import java.util.HashMap;
import java.util.Map;

public class MetaData {

	private HashMap<String, MDNode> data;

	public MetaData() {
		data = new HashMap<String, MDNode>();
	}

	public MetaData(String str) {
		data = new HashMap<String, MDNode>();
		String[] lines = str.split("\\\\n"); //("\\r?\\n");
		for (String line:lines) {
		//	System.out.print(" - ");
		//	System.out.println(line);
			if (line.equals("\\"))
				break;
			MDNode node = new MDNode(line);
			data.put(node.getName(), node);
		}
	}

	public HashMap<String, MDNode> getData() {
		return this.data;
	}

	public MDNode get(String name) {
		return this.data.get(name);
	}

	public void addNode(MDNode node) {
		this.data.put(node.getName(), node);
	}

	public void clear() {
		this.data.clear();
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		for (String key:data.keySet()) {
			MDNode node = data.get(key);
			str.append(node.toString());
			str.append("\\n");
		}
		str.append('\\');
		return str.toString();
	}
}
