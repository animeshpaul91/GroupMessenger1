package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.widget.Button;
import android.widget.TextView;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.TextView;
import android.net.Uri;

import java.io.IOException;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] REMOTE_PORT = {"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;

    Uri providerUri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger1.provider");
    int c=0; //counter for generating keys

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        Log.v("URI", providerUri.toString());

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        } catch (IOException e) {
            Log.e(TAG, "Can't Create a ServerSocket");
            Log.e(TAG, e.getMessage());
            Log.e(TAG, Log.getStackTraceString(e));
            return;
        }

        final Button b = (Button) findViewById(R.id.button4); //Refers to Send Button
        final EditText editText = (EditText) findViewById(R.id.editText1); //Text that is typed

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString();
                editText.setText(""); //Reset
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
            }
        });

    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void>{

        @Override
        protected Void doInBackground(ServerSocket... sockets)
        {  ServerSocket serverSocket = sockets[0];

            try {
                String msgFromClient;
                while (true) {
                    Socket server = serverSocket.accept();
                    InputStreamReader input = new InputStreamReader(server.getInputStream());
                    BufferedReader in = new BufferedReader(input);
                    msgFromClient = in.readLine();
                    if (msgFromClient != null) {
                        PrintWriter pw = new PrintWriter(server.getOutputStream(), true);
                        pw.println(msgFromClient);
                        publishProgress(msgFromClient);
                        server.close();
                    }

                }
            }
            catch (Exception e)
            {
                Log.e(TAG, "Exception Occured");
                Log.e(TAG, e.getMessage());
                Log.e(TAG, Log.getStackTraceString(e));
            }
            return null;
        }


        protected void onProgressUpdate(String... strings)
        {
            String msgReceived = strings[0].trim();
            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(msgReceived+"\t\n");

            //Inserting Key and Value
            ContentValues keyValueToInsert = new ContentValues();
            keyValueToInsert.put("key", Integer.toString(c));
            keyValueToInsert.put("value", msgReceived);
            Uri newUri = getContentResolver().insert(providerUri, keyValueToInsert);
            c++; //Increase Sequence Number
            Log.v("Insertion Successful", newUri.toString());
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void>
    {
        @Override
        protected Void doInBackground(String... msgs)
        {
            try
            {
                for (int i=0; i< REMOTE_PORT.length; i++)
                {
                    String msgToSend = msgs[0];
                    Socket client = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(REMOTE_PORT[i]));
                    try
                    {
                        PrintWriter output = new PrintWriter(client.getOutputStream(), true);
                        output.println(msgToSend);
                        output.flush();
                    }
                    catch (IOException e)
                    {
                        Log.e(TAG, "IO Exception Occured");
                        Log.e(TAG, e.getMessage());
                        Log.e(TAG, Log.getStackTraceString(e));
                    }

                    InputStreamReader ip = new InputStreamReader(client.getInputStream());
                    BufferedReader bf = new BufferedReader(ip);
                    while(!bf.readLine().equals(msgToSend)) {
                        continue;
                    }
                    ip.close();
                    client.close();
                }
            }
            catch (UnknownHostException e)
            {
                Log.e(TAG,"ClientTask UnknownHostException");
                Log.e(TAG, e.getMessage());
                Log.e(TAG, Log.getStackTraceString(e));
            }
            catch (IOException e)
            {
                Log.e(TAG,"ClientTask socket IO Exception");
                Log.e(TAG, e.getMessage());
                Log.e(TAG, Log.getStackTraceString(e));
            }

            return null;
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
