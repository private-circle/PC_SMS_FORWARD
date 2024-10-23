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

/**
 * SmsService is a Service that handles the SMS forwarding functionality.
 * It listens for incoming SMS messages, processes them, and sends the data
 * to a remote server while providing notifications to the user.
 */
public class SmsService extends Service {
    private ApiService apiService; // API service for network calls
    private static final String BASE_URL = "https://privatecircle.co/"; // Base URL for the API
    private static final String AUTH_TOKEN = "Bearer 8X3U4IwmyUoxBBUPksn8388NwGPo0F"; // Actual OAuth2 token for authentication
    private static final int MAX_RETRIES = 4; // Maximum number of retry attempts for sending messages
    private static final long INITIAL_RETRY_INTERVAL = 30 * 1000; // Initial retry interval set to 30 seconds
    private long retryInterval = INITIAL_RETRY_INTERVAL; // Current retry interval
    private int retryCount = 0; // Counter for the number of retries
    public static final String ACTION_SEND_MESSAGE = "com.example.myapplication.SEND_MESSAGE"; // Action string for sending messages

    public static final String SMS_SERVICE_CHANNEL = "SmsServiceChannel"; // Notification channel ID

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Retrofit for network calls
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class); // Create an instance of the API service

        createNotification(); // Create the notification for the service
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create a foreground notification for the service
        Notification notification = new Notification.Builder(this, SMS_SERVICE_CHANNEL)
                .setContentTitle("SMS Forwarding Service")
                .setContentText("Running in background")
                .setSmallIcon(R.drawable.pc_notification)
                .build();
        startForeground(1, notification); // Start the service in the foreground

        // Check if the intent contains SMS data
        if (intent != null && intent.hasExtra("message") && intent.hasExtra("sender") && intent.hasExtra("time")) {
            String message = intent.getStringExtra("message"); // Retrieve the message
            String sender = intent.getStringExtra("sender"); // Retrieve the sender
            String time = intent.getStringExtra("time"); // Retrieve the time
            // Send the SMS data to the server
            sendMessageToServer(message, sender, time);

            // Send a broadcast with the SMS data to main activity to view the SMS text and sender in textviews
            Intent broadcastIntent = new Intent(ACTION_SEND_MESSAGE);
            broadcastIntent.putExtra("message", message);
            broadcastIntent.putExtra("sender", sender);
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent); // Send the broadcast
        } else {
            Log.d("SmsForwardingService", "Service started without SMS data"); // Log if no SMS data is found
        }

        return START_STICKY; // Indicate that the service should be restarted if terminated
    }

    /**
     * Creates a notification channel and builds the notification for the service.
     */
    private void createNotification() {
        NotificationChannel channel = new NotificationChannel(SMS_SERVICE_CHANNEL, "Sms Service Channel", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel); // Create the notification channel
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, SMS_SERVICE_CHANNEL)
                .setContentTitle("SMS Service")
                .setContentText("Running...")
                .setSmallIcon(R.drawable.pc_notification)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        builder.build(); // Build the notification
    }

    /**
     * Sends the SMS message to the server.
     *
     * @param message The content of the SMS message.
     * @param sender The sender of the SMS message.
     * @param time The time the SMS message was received.
     */
    private void sendMessageToServer(String message, String sender, String time) {
        MessageBody messageBody = new MessageBody(message, sender, time); // Create a MessageBody object
        Call<Void> call = apiService.sendMessage(AUTH_TOKEN, messageBody); // Create a call to send the message
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    resetRetryParams(); // Reset retry parameters on success
                } else {
                    handleFailure(message, sender, time); // Handle failure if the response is not successful
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                handleFailure(message, sender, time); // Handle failure on network error
            }
        });
    }

    /**
     * Handles the failure of sending a message.
     *
     * @param message The content of the SMS message.
     * @param sender The sender of the SMS message.
     * @param time The time the SMS message was received.
     */
    private void handleFailure(String message, String sender, String time) {
        if (retryCount < MAX_RETRIES) {
            retryCount++; // Increment the retry count
            retrySendMessageDelayed(message, sender, time); // Retry sending the message after a delay
        } else {
            showAlert(); // Show an alert if maximum retries are reached
            resetRetryParams(); // Reset retry parameters
        }
    }

    /**
     * Retries sending the message after a delay.
     *
     * @param message The content of the SMS message.
     * @param sender The sender of the SMS message.
     * @param time The time the SMS message was received.
     */
    private void retrySendMessageDelayed(String message, String sender, String time) {
        new Handler().postDelayed(() -> sendMessageToServer(message, sender, time), retryInterval); // Retry after the specified interval
        retryInterval *= 2; // Double the retry interval for the next attempt
    }

    /**
     * Resets the retry parameters to their initial values.
     */
    private void resetRetryParams() {
        retryCount = 0; // Reset the retry count
        retryInterval = INITIAL_RETRY_INTERVAL; // Reset the retry interval
    }

    /**
     * Shows an alert to the user indicating that message sending has failed.
     */
    private void showAlert() {
        Toast.makeText(SmsService.this, "Message sending failed after multiple retries.", Toast.LENGTH_LONG).show(); // Show a toast message
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Return null as binding is not used
    }
}
