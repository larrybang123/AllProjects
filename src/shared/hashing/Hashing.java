package shared.hashing;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import app_kvServer.KVServer;
import shared.CommonConstants;
import shared.metadata.MDNode;
import shared.metadata.MetaData;

public class Hashing {
    public static String getMd5(String input)
    {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] inputInBytes = input.getBytes();
            byte[] outputInBytes = md.digest(inputInBytes);
            BigInteger bigInt = new BigInteger(1, outputInBytes);
            return bigInt.mod(new BigInteger(CommonConstants.publicMod)).toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Deprecated
    public static MDNode getServerBykey(String key,KVServer server) throws NoSuchAlgorithmException {
    	MDNode mdNode = null;
    	HashMap<String,MDNode> metadata = server.getMetadata().getData();
    	Set<String> keys = metadata.keySet();
    	
    	boolean currentResult = isKeyInRange(key,server.getRange()[0],server.getRange()[1]);
    	
    	if(currentResult) {
    		
    		for(String key1 : keys) {
    			if(key1.equals(server.getServerName())) {
    				mdNode = metadata.get(key1);
    				break;
    			}
    		}
    	}else {
    		/*
    		for(JsonElement ser : allServerInfo) {
    			JsonObject jsResultTemp = ser.getAsJsonObject();
    			boolean flag = isKeyInRange(key,jsResultTemp.get("lowHash").getAsString(),jsResultTemp.get("highHash").getAsString());
    			if(flag) {
    				jsResult = jsResultTemp;
    				break;
    			}
        	}*/
    		
    		for(String key1 : keys) {
    			MDNode nodeTemp = metadata.get(key1);
    			boolean flag = isKeyInRange(key,nodeTemp.getStartHash(),nodeTemp.getEndHash());
    			if(key1.equals(server.getName())) {
    				mdNode = metadata.get(key1);
    				break;
    			}
    		}
    	}
    	System.out.println("key"+key+" -> server:"+mdNode);
    	
    	if(mdNode == null) {
    		System.out.println("program error ,find server error ");
    	}
    	return mdNode;
    }
    
    public static boolean isKeyInRange(String key, String start, String end) throws NoSuchAlgorithmException {
        String hash = getMd5(key);
        if (isHashGreaterThan(start, end) == 1) {
            return (isHashGreaterThan(hash, start) == 1 || (isHashGreaterThan(hash, end) <= 0));
        } else if (isHashGreaterThan(start, end) == 0) {
            return true;
        } else {
            // Normal case
            return (isHashGreaterThan(hash, start) == 1 && (isHashGreaterThan(hash, end) <= 0));
        }
    }

    public static int isHashGreaterThan(String h1, String h2) {
        BigInteger bigIntger1 = new BigInteger(h1, 10);
        BigInteger bigIntger2 = new BigInteger(h2, 10);
        int i1 = bigIntger1.compareTo(bigIntger2);
        return i1;
    }

    public static String findServerInRange(String key, MetaData meta, boolean isPut) throws NoSuchAlgorithmException {
        HashMap<String, MDNode> nodeData = meta.getData();
        for (Map.Entry<String, MDNode> entry : nodeData.entrySet()) {
	    	MDNode v = entry.getValue();
			String startHash = v.getStartHash();
			String endHash = v.getEndHash();
            if (isKeyInRange(key, startHash, endHash)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        
        System.out.println(getMd5("mgf101"));
        System.out.println( isKeyInRange("mgf100","10","88"));
        System.out.println( isKeyInRange("mgf101","10","88"));
        System.out.println( isKeyInRange("mgf101","88","10"));
        
    }
}
