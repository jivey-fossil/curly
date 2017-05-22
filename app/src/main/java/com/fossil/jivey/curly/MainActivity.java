package com.fossil.jivey.curly;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.net.NetworkInfo;

import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static android.R.attr.lines;

public class MainActivity extends AppCompatActivity implements CommandCompleteListener {
    private static final String TAG = "Curly";
    InputStream commandsIS;
    BufferedReader reader;
    String command;
    TextView textView;
    TimingLogger timings;
    int mLineCount=0, mCompleteCount=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.testView1);
        textView.setMovementMethod(new ScrollingMovementMethod());
        final EditText linecount = (EditText) findViewById(R.id.textView_lineCount);

        // Make sure internet is available before enabling "process file" button
        // Read file button is enabled by default in the layout xml file
        if (isWifiConnected()) {
            if (isNetworkConnected()) {
                Button procFileBtn = (Button) findViewById(R.id.button2);
                procFileBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        textView.setText("");
                        String count = linecount.getText().toString();
                        if (!count.isEmpty()
                                && android.text.TextUtils.isDigitsOnly(count)) {
                            mLineCount = Integer.parseInt(count);
                            processFile(v, mLineCount);
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    "Please enter valid number of lines.", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });
            } else {
                new AlertDialog.Builder(this)
                    .setTitle("No Internet Connection")
                    .setMessage("It looks like your internet connection is off. Please turn it " +
                            "on and try again")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).setIcon(android.R.drawable.ic_dialog_alert).show();
            }
        }

    }

    @Override
    public void onPause() {
        if (mCompleteCount != mLineCount) {
            Long totalTime = timings.dumpToLog();
            textView.append("\n Total time: " + totalTime + "ms");
        }
        super.onPause();

    }

    // Callback from network command execution
    @Override
    public void commandComplete(String url, String response) {
        Long splitTime = timings.addSplit("commandComplete");
        String displayStr = "\n\n[" + ++mCompleteCount + "] time: " + splitTime + "ms, " + url;
        textView.append(displayStr);
        textView.append("\n" + response);
        if (mCompleteCount == mLineCount) {
            Long totalTime = timings.dumpToLog();
            textView.append("\n Total time: " + totalTime + "ms");
        }
    }

    // Read text from file one line per button press
    public void ReadBtn(View v) {
        //reading text from file
        if (commandsIS == null) resetIS();
        try {
            command = reader.readLine();
            timings.addSplit(command);
            if (command != null) textView.setText(command);
            else {
                reader.close(); reader = null;
                commandsIS.close(); commandsIS = null;
                timings.dumpToLog();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Read text from file one line per button press
    public void processFile(View v, int lines) {
        //reading text from file
        int totalLines = lines;
        if (commandsIS == null) resetIS();
        try {
            command = reader.readLine();
            while (command != null && lines > 0) {
                timings.addSplit(command);
                //new ExecuteCommand(this).execute(command);
                String countStr = "Command " + String.valueOf(totalLines - lines);
                new ExecuteCommand(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,command, countStr);
                command = reader.readLine();
                lines--;
                }
            // out of lines in commands file
            reader.close(); reader = null;
            commandsIS.close(); commandsIS = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // open command input file, and set up buffered reader
    private void resetIS() {
        commandsIS = getResources().openRawResource(R.raw.command_lines);
        reader = new BufferedReader(new InputStreamReader(commandsIS));
        if (timings == null) timings = new TimingLogger(TAG, "Process Commands", true);
        else {
            timings.dumpToLog();
            timings.reset();
        }
        mCompleteCount = 0;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE); // 1
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo(); // 2
        return networkInfo != null && networkInfo.isConnected(); // 3
    }

    private boolean isWifiConnected() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && (ConnectivityManager.TYPE_WIFI == networkInfo.getType()) && networkInfo.isConnected();
    }

}
