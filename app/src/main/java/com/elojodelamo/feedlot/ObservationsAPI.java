package com.elojodelamo.feedlot;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ObservationsAPI {

    @GET("observation/{rfid_id}")
    Call<List<Observation>> getObservationByTagId(@Path("rfid_id") int rfid_id);

    @POST("observation")
    Call<Observation> createObservation(@Body Observation observation);

    @PUT("observation/{id}")
    Call<Void> updateObservation(@Path("id") int id, @Body Observation observation);

    @DELETE("observation/{id}")
    Call<Void> deleteObservation(@Path("id") int id);

}
