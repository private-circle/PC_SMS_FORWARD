    package com.example.myapplication;

    import retrofit2.Call;
    import retrofit2.http.Body;
    import retrofit2.http.Header;
    import retrofit2.http.Headers;
    import retrofit2.http.POST;

    /**
     * ApiService interface for defining API endpoints related to SMS messaging.
     * This interface uses Retrofit to handle HTTP requests.
     */
    public interface ApiService {
        
        /**
         * Sends a message to the server.
         *
         * @param authorization The authorization token to access the API, typically in the format "Bearer {token}".
         * @param messageBody The body of the message to be sent, encapsulated in a MessageBody object.
         * @return A Call<Void> object which can be used to send the request asynchronously.
         */
        @Headers("Content-Type: application/json")
        @POST("mca_purchase_process/bank_otp/") // API endpoint for sending messages
        Call<Void> sendMessage(@Header("Authorization") String authorization, @Body MessageBody messageBody);
    }
