package testing;

import app_kvClient.KVClient;
import client.KVStore;
import org.junit.Test;

import junit.framework.TestCase;
import shared.messages.KVMessage;

import java.lang.reflect.WildcardType;

public class AdditionalTest extends TestCase {

    // TODO add your test cases, at least 3
    private KVStore kvClient;
    private KVClient kvapplication;

    public void setUp() {
        kvClient = new KVStore("localhost", 50000);
        try {
            kvClient.connect();
        } catch (Exception e) {
        }
    }

    @Test
    public void testGetKeyOverMaxLength(){
        String key = "foo2foo2foo2foo2foo2foo2foo2foo2foo2";
        KVMessage response = null;
        Exception ex = null;
        try{
            response = kvClient.get(key);
        }catch (Exception e){
            ex=e;
        }
        assertTrue(ex == null && response.getStatus() == KVMessage.StatusType.GET_ERROR);
    }

    @Test
    public void testPutKeyOverMaxLength(){
        String key = "foo2foo2foo2foo2foo2foo2foo2foo2foo2";
        String value="bar2";
        KVMessage response = null;
        Exception ex = null;
        try{
            response = kvClient.put(key,value);
        }catch (Exception e){
            ex=e;
        }
        assertTrue(ex == null && response.getStatus() == KVMessage.StatusType.PUT_ERROR);
    }

    @Test
    public void testDeleteKeyOverMaxLength(){
        String key = "foo2foo2foo2foo2foo2foo2foo2foo2foo2";
        String value=null;
        KVMessage response = null;
        Exception ex = null;
        try{
            response = kvClient.put(key,value);
        }catch (Exception e){
            ex=e;
        }
        assertTrue(ex == null && response.getStatus() == KVMessage.StatusType.DELETE_ERROR);
    }

    @Test
    public void testPutValueOverLength(){
        String key="foo2";
        StringBuilder buildvalue=new StringBuilder("a");
        for (int i=0; i<1000000;i++){
            buildvalue.append("a");
        }
        String value= buildvalue.toString();
        KVMessage response = null;
        Exception ex = null;
        try{
            response = kvClient.put(key,value);
        }catch (Exception e){
            ex=e;
        }
        assertTrue(ex == null && response.getStatus() == KVMessage.StatusType.PUT_ERROR);
    }

    @Test
    public void testGetDisconnected() {
        kvClient.disconnect();
        String key = "foo";

        Exception ex = null;

        try {
            kvClient.get(key);
        } catch (Exception e) {
            ex = e;
        }

        assertNotNull(ex);
    }

    @Test
    public void testPutValueWithSpace() {
        String key = "foo";

        String value_withspace = "bar2 bar2";
        KVMessage response = null;
        Exception ex = null;

        try {
            response = kvClient.put(key, value_withspace);

        } catch (Exception e) {
            ex = e;
        }

        assertTrue(ex == null && response.getStatus() == KVMessage.StatusType.PUT_UPDATE&& response.getValue().equals(value_withspace));
    }
}
