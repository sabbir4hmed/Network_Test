package com.sabbir.waltonbd.wcmsproductchecker.Api;

import com.sabbir.waltonbd.wcmsproductchecker.Models.LoginRequest;
import com.sabbir.waltonbd.wcmsproductchecker.Models.LoginResponse;
//import com.sabbir.waltonbd.wcmsproductchecker.Models.ProductResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    // Login API - WCMS
    @POST("api/account/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

   /* // Product GET API (for future use)
    @GET("api/product")
    Call<ProductResponse> getProduct(
            @Header("Authorization") String token,
            @Query("imei") String imei
    );*/
}
