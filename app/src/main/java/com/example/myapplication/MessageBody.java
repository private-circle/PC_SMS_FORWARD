    /**
     * MessageBody is a class that represents the structure of a message
     * to be sent to the server. It encapsulates the message content,
     * the sender's information, and the time the message was received.
     */
    package com.example.myapplication;

    public class MessageBody {
        // The content of the message
        private final String message;
        // The sender of the message
        private final String sender;
        // The time the message was received
        private final String time;

        /**
         * Constructs a new MessageBody instance with the specified message,
         * sender, and time.
         *
         * @param message The content of the message.
         * @param sender The sender of the message.
         * @param time The time the message was received.
         */
        public MessageBody(String message, String sender, String time) {
            this.message = message;
            this.sender = sender;
            this.time = time;
        }
    }
