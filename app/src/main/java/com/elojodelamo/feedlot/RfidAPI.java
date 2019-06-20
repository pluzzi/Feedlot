package com.elojodelamo.feedlot;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface RfidAPI {

    @GET("rfid")
    Call<List<Rfid>> getRfids();

}
