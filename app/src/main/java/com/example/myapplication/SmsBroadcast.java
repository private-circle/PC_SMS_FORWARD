    /**
     * SmsBroadcast is a BroadcastReceiver that listens for incoming SMS messages.
     * It processes the received messages and validates their content before sending
     * the relevant information to the SmsService or MainActivity.
     */
    package com.example.myapplication;

    import android.content.BroadcastReceiver;
    import android.content.Context;
    import android.content.Intent;
    import android.os.Bundle;
    import android.telephony.SmsMessage;
    import android.util.Log;

    import java.util.Objects;
    import java.util.regex.Pattern;
    import java.util.regex.Matcher;

    public class SmsBroadcast extends BroadcastReceiver {
        // Constant for the SMS received action
        private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

        /**
         * Called when an SMS message is received.
         *
         * @param context The context in which the receiver is running.
         * @param intent The intent containing the received SMS message.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            // Check if the received intent action matches SMS_RECEIVED
            if (SMS_RECEIVED.equals(intent.getAction())) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    // Retrieve the PDUs (Protocol Data Units) from the bundle
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    if (pdus != null) {
                        SmsMessage[] smsMessages = new SmsMessage[pdus.length];
                        // Process each PDU
                        for (int i = 0; i < pdus.length; i++) {
                            smsMessages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                            String message = smsMessages[i].getMessageBody();
                            String sender = smsMessages[i].getOriginatingAddress();
                            String time = String.valueOf(smsMessages[i].getTimestampMillis());

                            // Validate the message content
                            if (validateMessage(message)) {
                                // Send the message details to the SmsService
                                sendMessageBroadcastToService(context, message, sender, time);
                            }
                        }
                    }
                }
            }
        }

        /**
         * Sends a broadcast to the MainActivity with the SMS details.
         *
         * @param context The context in which the receiver is running.
         * @param message The content of the SMS message.
         * @param sender The sender's phone number.
         * @param time The time the message was received.
         */
        private void sendBroadcastToMainActivity(Context context, String message, String sender, String time) {
            // Create an intent for the MainActivity action
            Intent broadcastIntent = new Intent(MainActivity.ACTION_SEND_MESSAGE);
            broadcastIntent.putExtra("message", message);
            broadcastIntent.putExtra("sender", sender);
            broadcastIntent.putExtra("time", time);
            Log.d("SmsBroadcast", "Sending broadcast to MainActivity\n" + message + "\n" + sender + "\n" + time);
            context.sendBroadcast(broadcastIntent);
        }

        /**
         * Sends the SMS details to the SmsService for further processing.
         *
         * @param context The context in which the receiver is running.
         * @param message The content of the SMS message.
         * @param sender The sender's phone number.
         * @param time The time the message was received.
         */
        private void sendMessageBroadcastToService(Context context, String message, String sender, String time) {
            // Create an intent for the SmsService
            Intent serviceIntent = new Intent(context, SmsService.class);
            serviceIntent.setAction(SmsService.ACTION_SEND_MESSAGE);
            serviceIntent.putExtra("message", message);
            serviceIntent.putExtra("sender", sender);
            serviceIntent.putExtra("time", time);
            context.startService(serviceIntent);
        }

        /**
         * Validates the content of the SMS message based on specific criteria.
         *
         * @param message The content of the SMS message to validate.
         * @return true if the message meets the validation criteria, false otherwise.
         */
        public static boolean validateMessage(String message) {
            // Create a case-insensitive regex pattern to check for "OTP" , at least "ICICI" or "Volopay"
            // along with at least one of "INR 101.18" or "INR 50.59"
            Pattern pattern = Pattern.compile("(?i)(?=.*\\bOTP\\b)(?=.*\\b(?:ICICI|Volopay)\\b)(?=.*\\b(?:INR 101\\.18|INR 50\\.59|INR 100\\.00|INR 50\\.00 )\\b)");

            // Match the pattern against the message
            Matcher matcher = pattern.matcher(message);

            // Return true if both "OTP", "ICICI" or "Volopay" and at least one of "INR 101.18" or "INR 50.59" are found
            return matcher.find();
        }
    }
