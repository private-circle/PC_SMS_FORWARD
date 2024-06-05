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
        private ApiService apiService;
        private static final int SMS_PERMISSION_REQUEST_CODE = 47;
        private static final String BASE_URL = "https://privatecircle.co/";
        private static final String AUTH_TOKEN = "Bearer 8X3U4IwmyUoxBBUPksn8388NwGPo0F"; // Actual OAuth2 token prod, qa
        private static final int MAX_RETRIES = 4;
        private static final long INITIAL_RETRY_INTERVAL = 30 * 1000; // 30 seconds

        private TextView senderTextView, messageTextView;
        private long retryInterval = INITIAL_RETRY_INTERVAL;
        private int retryCount = 0;

        private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_SEND_MESSAGE.equals(intent.getAction())) {
                    String message = intent.getStringExtra("message");
                    String sender = intent.getStringExtra("sender");
                    String time = intent.getStringExtra("time");

                    senderTextView.setText("From: " + sender);
                    messageTextView.setText("Body is : " + message);
                    sendMessageToServer(message, sender, time);
                }
            }
        };

        public static final String ACTION_SEND_MESSAGE = "com.example.myapplication.SEND_MESSAGE";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_main);

            initializeUI();

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                requestSMSPermission();
            } else {
                initializeApp();
            }

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

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
                    initializeApp();
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

        private void initializeApp() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(ApiService.class);
        }

        private void sendMessageToServer(String message, String sender, String time) {
            MessageBody messageBody = new MessageBody(message, sender, time);
            Call<Void> call = apiService.sendMessage(AUTH_TOKEN, messageBody);
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        resetRetryParams();
                    } else {
                        handleFailure(message, sender, time);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    handleFailure(message, sender, time);
                }
            });
        }

        private void handleFailure(String message, String sender, String time) {
            if (retryCount < MAX_RETRIES) {
                retryCount++;
                retrySendMessageDelayed(message, sender, time);
            } else {
                showAlert();
                resetRetryParams();
            }
        }

        private void retrySendMessageDelayed(String message, String sender, String time) {
            new Handler().postDelayed(() -> sendMessageToServer(message, sender, time), retryInterval);
            retryInterval *= 2;
        }

        private void resetRetryParams() {
            retryCount = 0;
            retryInterval = INITIAL_RETRY_INTERVAL;
        }

        private void showAlert() {
            Toast.makeText(MainActivity.this, "Message sending failed after multiple retries.", Toast.LENGTH_LONG).show();
        }
    }
