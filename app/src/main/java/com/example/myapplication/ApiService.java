    package com.example.myapplication;

    import retrofit2.Call;
    import retrofit2.http.Body;
    import retrofit2.http.Header;
    import retrofit2.http.Headers;
    import retrofit2.http.POST;

    public interface ApiService {
        @Headers("Content-Type: application/json")
        @POST("mca_purchase_process/bank_otp/") //API endpoint
        Call<Void> sendMessage(@Header("Authorization") String authorization, @Body MessageBody messageBody);
    }
