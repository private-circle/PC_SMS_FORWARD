package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SmsService extends Service {
    private ApiService apiService;
    private static final String BASE_URL = "https://privatecircle.co/";
    private static final String AUTH_TOKEN = "Bearer 8X3U4IwmyUoxBBUPksn8388NwGPo0F"; // Actual OAuth2 token prod, qa
    private static final int MAX_RETRIES = 4;
    private static final long INITIAL_RETRY_INTERVAL = 30 * 1000; // 30 seconds
    private long retryInterval = INITIAL_RETRY_INTERVAL;
    private int retryCount = 0;
    public static final String ACTION_SEND_MESSAGE = "com.example.myapplication.SEND_MESSAGE";

    public static final String SMS_SERVICE_CHANNEL = "SmsServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        createNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new Notification.Builder(this, SMS_SERVICE_CHANNEL)
                .setContentTitle("SMS Forwarding Service")
                .setContentText("Running in background")
                .setSmallIcon(R.drawable.pc_notification)
                .build();
        startForeground(1, notification);

        if (intent != null && intent.hasExtra("message") && intent.hasExtra("sender") && intent.hasExtra("time")) {
            String message = intent.getStringExtra("message");
            String sender = intent.getStringExtra("sender");
            String time = intent.getStringExtra("time");
            // Send the SMS data to the server
            sendMessageToServer(message, sender, time);

            // Send a broadcast with the SMS data to main activity to view the SMS text and sender in textviews
            Intent broadcastIntent = new Intent(ACTION_SEND_MESSAGE);
            broadcastIntent.putExtra("message", message);
            broadcastIntent.putExtra("sender", sender);
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
        } else {
            Log.d("SmsForwardingService", "Service started without SMS data");
        }

        return START_STICKY;
    }

    private void createNotification() {
        NotificationChannel channel = new NotificationChannel(SMS_SERVICE_CHANNEL, "Sms Service Channel", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, SMS_SERVICE_CHANNEL)
                .setContentTitle("SMS Service")
                .setContentText("Running...")
                .setSmallIcon(R.drawable.pc_notification)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        builder.build();
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
        Toast.makeText(SmsService.this, "Message sending failed after multiple retries.", Toast.LENGTH_LONG).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}


