package shared.messages;

import com.google.gson.Gson;
import java.util.Arrays;


public class KVAdminMessage {
    public enum Command{
        INIT,
        SHUT_DOWN,
        START,
        STOP,
        TRANSFER
    }
    private Command command;

    public KVAdminMessage(Command command){
        this.command = command;
    }

    public Command getCommand() {
        return command;
    }


    public String encodeMessage(){
        return new Gson().toJson(this);
    }

    public void decode(String msg){
        KVAdminMessage KVAdmin = new Gson().fromJson(msg, this.getClass());
        this.command = KVAdmin.getCommand();
    }

}
