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
        private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (SMS_RECEIVED.equals(intent.getAction())) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    if (pdus != null) {
                        SmsMessage[] smsMessages = new SmsMessage[pdus.length];
                        for (int i = 0; i < pdus.length; i++) {
                            smsMessages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                            String message = smsMessages[i].getMessageBody();
                            String sender = smsMessages[i].getOriginatingAddress();
                            String time = String.valueOf(smsMessages[i].getTimestampMillis());

                            if (validateMessage(message)) {
//                                sendBroadcastToMainActivity(context, message, sender, time);
                                sendMessageBroadcastToService(context, message, sender, time);
                            }
                        }
                    }
                }
            }
        }

        private void sendBroadcastToMainActivity(Context context, String message, String sender, String time) {
//            Intent broadcastIntent = new Intent(SmsService.ACTION_SEND_MESSAGE);
            Intent broadcastIntent = new Intent(MainActivity.ACTION_SEND_MESSAGE);
            broadcastIntent.putExtra("message", message);
            broadcastIntent.putExtra("sender", sender);
            broadcastIntent.putExtra("time", time);
            Log.d("SmsBroadcast", "Sending broadcast to MainActivity\n" + message + "\n" + sender + "\n" + time);
            context.sendBroadcast(broadcastIntent);
        }

        private void sendMessageBroadcastToService(Context context, String message, String sender, String time) {
            Intent serviceIntent = new Intent(context, SmsService.class);
            serviceIntent.setAction(SmsService.ACTION_SEND_MESSAGE);
            serviceIntent.putExtra("message", message);
            serviceIntent.putExtra("sender", sender);
            serviceIntent.putExtra("time", time);
            context.startService(serviceIntent);
        }

        public static boolean validateMessage(String message) {
            // Create a case-insensitive regex pattern to check both "OTP" and at least one of "INR 100.00" or "INR 50.00"
            // Pattern pattern = Pattern.compile("(?i)(?=.*\\bOTP\\b)(?=.*\\b(?:INR 100\\.00|INR 50\\.00)\\b)");
            // Create a case-insensitive regex pattern to check both "OTP" and "ICICI" along with at-least at least one of "INR 100.00" or "INR 50.00"
            Pattern pattern = Pattern.compile("(?i)(?=.*\\bOTP\\b)(?=.*\\bICICI\\b)(?=.*\\b(?:INR 100\\.00|INR 50\\.00)\\b)");

//            This pattern ensures that:
//
//            (?i)                                      :makes the regex case-insensitive.
//            (?=.*\\bOTP\\b)                           :ensures "OTP" is present.
//            (?=.*\\bICICI\\b)                         :ensures "ICICI" is present.
//            (?=.*\\b(?:INR 100\\.00|INR 50\\.00)\\b)  :ensures that either "INR 100.00" or "INR 50.00" is present.

            // Match the pattern against the message
            Matcher matcher = pattern.matcher(message);

            // Return true if both "OTP" and at least one of "INR 100.00" or "INR 50.00" are found
            return matcher.find();
        }
    }
