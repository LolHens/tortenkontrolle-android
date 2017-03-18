package lolhens.piectrl.piectrl;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.io.IOException;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    volatile private Socket socket = null;
    private ToggleButton[] toggleButtons;
    private EditText textfieldHost;

    private String PREFS_NAME = "Tortenkontrolle";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textfieldHost = (EditText) findViewById(R.id.textfieldHost);

        Button buttonConnect = (Button) findViewById(R.id.buttonConnect);

        toggleButtons = new ToggleButton[]{
                (ToggleButton) findViewById(R.id.toggleButton0),
                (ToggleButton) findViewById(R.id.toggleButton1),
                (ToggleButton) findViewById(R.id.toggleButton2),
                (ToggleButton) findViewById(R.id.toggleButton3),
                (ToggleButton) findViewById(R.id.toggleButton4),
                (ToggleButton) findViewById(R.id.toggleButton5),
                (ToggleButton) findViewById(R.id.toggleButton6),
                (ToggleButton) findViewById(R.id.toggleButton7)
        };

        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("host", textfieldHost.getText().toString());
                editor.apply();
            }
        });

        for (final ToggleButton toggleButton : toggleButtons) {
            toggleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Log.i("PieCtrl", "Write");
                    sendValue();
                    toggleButton.setChecked(!toggleButton.isChecked());
                }
            });
        }

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        textfieldHost.setText(settings.getString("host", ""));
        connect();

        startReceiverThread();
    }

    private void connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(textfieldHost.getText().toString(), 11641);
                } catch (Exception e) {
                    Log.e("PieCtrl", "Connect Error", e);
                }
            }
        }).start();
    }

    private void sendValue() {
        int value = 0;

        for (int i = 0; i < 8; i++)
            if (toggleButtons[i].isChecked()) value |= (0x1 << i);

        final int finalValue = value;

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (socket != null)
                    try {
                        socket.getOutputStream().write(finalValue);
                    } catch (Exception e) {
                        Log.e("PieCtrl", "Write Error", e);
                    }
            }
        }).start();
    }

    private void startReceiverThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    //Log.i("PieCtrl", "Read");
                    if (socket != null)
                        try {
                            final int byteValue = socket.getInputStream().read();

                            if (byteValue >= 0) {
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        for (int i = 0; i < 8; i++) {
                                            boolean value = (byteValue & (0x1 << i)) != 0;

                                            toggleButtons[i].setChecked(value);
                                        }
                                    }
                                });

                                continue;
                            } else
                                socket = null;
                        } catch (Exception e) {
                            Log.e("PieCtrl", "Read Error", e);
                        }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }).start();
    }
}
