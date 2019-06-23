package com.elojodelamo.feedlot;

import com.google.gson.annotations.SerializedName;

public class Rfid {
    private Integer id;
    private String rfid;

    public Rfid(String rfid) {
        this.rfid = rfid;
    }

    public Integer getId() {
        return id;
    }

    public String getRfid() {
        return rfid;
    }
}
