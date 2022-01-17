package app_kvServer.app_kvDatabase;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import app_kvServer.KVServer;
import shared.CommonConstants;
import shared.hashing.Hashing;

/**
 * This class is responsible for handling the persistent database of the server.
 * It also includes handling for mutual exclusion
 */
public class DatabaseManager {
    private final String filename;
    private final String directory;

    private KVServer server;
    private int size = 0;

    //private MessageDigest md;
    //private final String MIN_HASH = "00000000000000000000000000000000";
    //private final String MAX_HASH = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
    
    private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public DatabaseManager(KVServer server) {
        this.server = server;
        String path = KVServer.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String serverPath = path.substring(0, path.lastIndexOf('/')) + "/";
        this.directory = serverPath;
        this.filename = CommonConstants.databaseFileName + server.getServerName();
        /*
        try {
            this.md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            // Should not happen
        }*/
        this.updateSize();
    }
    
    /**
     * create an temporary databasemanager
     * @param serverName
     */
    public  DatabaseManager(String serverName) {
        String path = KVServer.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String serverPath = path.substring(0, path.lastIndexOf('/')) + "/";
        this.directory = serverPath;
        this.filename = CommonConstants.databaseFileName + serverName;
        this.updateSize();
    }

    /**
     * Return a KVObject for the requested key, if present
     * @param key the key to search for
     * @return the KVObject for the key if present, null otherwise
     * @throws FileNotFoundException If database file not yet created
     */
    public KVObject getKVObject(String key) throws FileNotFoundException {
        readWriteLock.readLock().lock();
        try {
            BufferedReader br = new BufferedReader(new FileReader(directory + filename));
            String line;
            while ((line = br.readLine()) != null) {
                KVObject kv = new KVObject(line);
                if (kv.getKey().equals(key)) {
                    br.close();
                    return kv;
                }
            }
            br.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            readWriteLock.readLock().unlock();
        }
        return null;
    }
    
    public KVObject[] getAllKVObjects() {
        readWriteLock.readLock().lock();
        ArrayList<KVObject> kvList = new ArrayList<>();
        File f = new File(directory + filename);
        if (f.exists() && f.isFile()){
            try {
                BufferedReader br = new BufferedReader(new FileReader(directory + filename));
                String line;
                while ((line = br.readLine()) != null) {
                    KVObject kv = new KVObject(line);
                    kvList.add(kv);
                }
                br.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                readWriteLock.readLock().unlock();
            }
            return kvList.toArray(new KVObject[kvList.size()]);
        }
        return new KVObject[0];
    }

    public KVObject[] getKVObjectsInRange(String start, String end) {
        readWriteLock.readLock().lock();
        ArrayList<KVObject> kvList = new ArrayList<>();
        File f = new File(directory + filename);
        if (f.exists() && f.isFile()){
            try {
                BufferedReader br = new BufferedReader(new FileReader(directory + filename));
                String line;
                while ((line = br.readLine()) != null) {
                    KVObject kv = new KVObject(line);
                    if (Hashing.isKeyInRange(kv.getKey(), start, end))
                        kvList.add(kv);
                }
                br.close();
            } catch (IOException | NoSuchAlgorithmException ioe) {
                ioe.printStackTrace();
            } finally {
                readWriteLock.readLock().unlock();
            }
            return kvList.toArray(new KVObject[kvList.size()]);
        }
        return new KVObject[0];
    }

    /**
     * Get the value for the corresponding key
     * @param key the key to look for
     * @return the value for the key if present, null otherwise
     * @throws FileNotFoundException If database file not yet created
     */
    public String getValue(String key) throws FileNotFoundException {
        KVObject kv = getKVObject(key);
        if (kv == null)
            return null;
        else
            return kv.getValue();
    }

    /**
     * Insert a new database entry for the given key and value
     * @param key the entry key
     * @param value the entry value
     * @return true if the operation was successful, false otherwise
     */
    public boolean putKVObject(String key, String value) {
        if (isDeleteRequest(value)) {
            return deleteKVObject(key);
		}
        try {
            File file = new File(directory + filename);
            file.createNewFile();
            File tempFile = new File( directory + "temp_" + filename);
            tempFile.createNewFile();

            BufferedReader br = new BufferedReader(new FileReader(file));
            BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));

            String line;
            boolean keyFound = false;

            while ((line = br.readLine()) != null) {
                if (keyFound) {
                    bw.write(line + "\n");
                } else {
                    KVObject kv = new KVObject(line);
                    if (kv.getKey().equals(key)) {
                        // Update value
                        keyFound = true;
                        kv.setValue(value);
                        bw.write(kv.toString() + "\n");
                    } else {
                        bw.write(line + "\n");
                    }
                }
            }

            if (!keyFound) {
                // Insert new kv
                KVObject kv = new KVObject(key, value);
                bw.write(kv.toString() + "\n");
                size++;
            }
            br.close();
            bw.close();
            file.delete();
            tempFile.renameTo(file);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            //readWriteLock.writeLock().unlock();
        }
    }

    public boolean putKVObject(KVObject kv) {
        return putKVObject(kv.getKey(), kv.getValue());
    }

    public boolean putKVObject(KVObject[] kvObjs) {
        // This method should not be updating any values
        readWriteLock.writeLock().lock();
        try {
            File file = new File(directory + filename);
            if (file.exists()){
                BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
                for (KVObject kv:kvObjs) {
                    bw.write(kv.toString() + "\n");
                }
                size += kvObjs.length;
                bw.close();
                return true;
            }else {
            	file.createNewFile();
            	BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
                for (KVObject kv:kvObjs) {
                    bw.write(kv.toString() + "\n");
                }
                size += kvObjs.length;
                bw.flush();
                bw.close();
                System.out.println("Create new file");
                return false;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * Delete the database entry for the given key
     * @param key the key to delete
     * @return true if the operation was successful, false otherwise
     */
    private boolean deleteKVObject(String key) {
        readWriteLock.writeLock().lock();
        try {
            File file = new File(directory + filename);
            file.createNewFile();
            File tempFile = new File(directory + "temp_" + filename);
            tempFile.createNewFile();

            BufferedReader br = new BufferedReader(new FileReader(file));
            BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));

            String line;
            boolean keyFound = false;

            while ((line = br.readLine()) != null) {
                if (keyFound) {
                    bw.write(line + "\n");
                } else {
                    KVObject kv = new KVObject(line);
                    if (kv.getKey().equals(key)) {
                        // Skip over this kv, 'deleting' it
                        keyFound = true;
                        size--;
                    } else {
                        bw.write(line + "\n");
                    }
                }
            }

            br.close();
            bw.close();
            file.delete();
            tempFile.renameTo(file);

            return keyFound;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public boolean deleteKVObjectsInRange(String start, String end) {
        readWriteLock.writeLock().lock();
        try {
            File file = new File(directory + filename);
            file.createNewFile();
            File tempFile = new File(directory + "temp_" + filename);
            tempFile.createNewFile();

            BufferedReader br = new BufferedReader(new FileReader(file));
            BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));

            String line;
            
            while ((line = br.readLine()) != null) {
                KVObject kv = new KVObject(line);
                if (!Hashing.isKeyInRange(kv.getKey(), start, end)) {
                    bw.write(line + "\n");
                } else {
                    size--;
                }
            }

            br.close();
            bw.close();
            file.delete();
            tempFile.renameTo(file);

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * Empty the database
     */
    public void clearDatabase() {
        readWriteLock.writeLock().lock();
        try {
            PrintWriter pw = new PrintWriter(new File(directory + filename));
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * Check whether the given value is actually requesting a delete operation
     * @param value the value to check
     * @return true if value represents a delete request
     */
    public static boolean isDeleteRequest(String value) {
        return value == null || value.isEmpty() || value.equals("null");
    }
    
    /**
     * Get current database size
     * @return current database size
     */
    public int getSize() {
        return size;
    }

    public void updateSize() {
        readWriteLock.readLock().lock();
        int sz = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(directory + filename));
            String line;
            while ((line = br.readLine()) != null) {
                sz++;
            }
            br.close();
        } catch (FileNotFoundException e) {
            size = 0;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            size = sz;
            readWriteLock.readLock().unlock();
        }
    }

	public String getFilename() {
		return filename;
	}

	public String getDirectory() {
		return directory;
	}
    
    
}
