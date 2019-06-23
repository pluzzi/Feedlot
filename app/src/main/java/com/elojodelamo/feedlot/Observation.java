package com.elojodelamo.feedlot;

public class Observation {
    private Integer id;
    private int rfid_id;
    private String observation;

    public Integer getId() {
        return id;
    }

    public int getRfid_id() {
        return rfid_id;
    }

    public String getObservation() {
        return observation;
    }

    public Observation(int rfid_id, String observation) {
        this.rfid_id = rfid_id;
        this.observation = observation;
    }
}
