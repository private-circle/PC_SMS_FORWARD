    package com.example.myapplication;

    import android.Manifest;

    import android.annotation.SuppressLint;
    import android.content.BroadcastReceiver;
    import android.content.Context;
    import android.content.Intent;
    import android.content.IntentFilter;
    import android.content.pm.PackageManager;
    import android.os.Build;
    import android.os.Bundle;
    import android.os.Handler;
    import android.util.Log;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.activity.EdgeToEdge;
    import androidx.annotation.NonNull;
    import androidx.annotation.RequiresApi;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.app.ActivityCompat;
    import androidx.core.content.ContextCompat;
    import androidx.core.graphics.Insets;
    import androidx.core.view.ViewCompat;
    import androidx.core.view.WindowInsetsCompat;

    import retrofit2.Call;
    import retrofit2.Callback;
    import retrofit2.Response;
    import retrofit2.Retrofit;
    import retrofit2.converter.gson.GsonConverterFactory;

    public class MainActivity extends AppCompatActivity {
        private static final int SMS_PERMISSION_REQUEST_CODE = 47;
        private TextView senderTextView, messageTextView;

        private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onReceive(Context context, Intent intent) {
                if (SmsService.ACTION_SEND_MESSAGE.equals(intent.getAction())) {
                    String message = intent.getStringExtra("message");
                    String sender = intent.getStringExtra("sender");

                    // Update the TextViews with the received data
                    messageTextView.setText(message);
                    senderTextView.setText(sender);
                }
            }
        };

        public static final String ACTION_SEND_MESSAGE = "com.example.myapplication.SEND_MESSAGE";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_main);

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });


            initializeUI();

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                requestSMSPermission();
            }

            // Start the service
            Intent serviceIntent = new Intent(this, SmsService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }

        @Override
        protected void onResume() {
            super.onResume();
            IntentFilter filter = new IntentFilter(ACTION_SEND_MESSAGE);
            registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }

        @Override
        protected void onPause() {
            super.onPause();
            unregisterReceiver(broadcastReceiver);
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    initializeApp();
                } else {
                    Toast.makeText(this, "SMS permission is required to run this app", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }

        private void requestSMSPermission() {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, SMS_PERMISSION_REQUEST_CODE);
        }

        private void initializeUI() {
            senderTextView = findViewById(R.id.sender_text);
            messageTextView = findViewById(R.id.body_text);
        }

    }
