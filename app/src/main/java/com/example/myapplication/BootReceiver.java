/**
 * BootReceiver is a BroadcastReceiver that listens for the ACTION_BOOT_COMPLETED event.
 * When the device finishes booting, it starts the SmsService to ensure that SMS forwarding
 * functionality is available immediately after boot.
 */
package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    /**
     * Called when the BroadcastReceiver receives a broadcast.
     *
     * @param context The context in which the receiver is running.
     * @param intent The Intent being received, which contains information about the broadcast.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if the received intent action is ACTION_BOOT_COMPLETED
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Create an intent to start the SmsService
            Intent serviceIntent = new Intent(context, SmsService.class);
            // Start the SmsService as a foreground service for Android O and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                // For older versions, start the service normally
                context.startService(serviceIntent);
            }
        }
    }
}