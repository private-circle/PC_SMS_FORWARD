    package com.example.myapplication;

    public class MessageBody {
        private final String message;
        private final String sender;
        private final String time;

        public MessageBody(String message, String sender, String time) {
            this.message = message;
            this.sender = sender;
            this.time = time;
        }
    }

