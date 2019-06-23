package com.elojodelamo.feedlot;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RfidAPI {

    @GET("rfid")
    Call<List<Rfid>> getRfids();

    @GET("rfid/{code}")
    Call<Rfid> getRfidByCode(@Path("code") String code);

    @POST("rfid")
    Call<Rfid> createRfid(@Body Rfid rfid);

    @DELETE("rfid/{id}")
    Call<Void> deleteRfid(@Path("id") int id);

}
