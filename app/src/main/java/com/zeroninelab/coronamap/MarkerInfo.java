package com.zeroninelab.coronamap;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class MarkerInfo {

    private String title;
    private ArrayList<LatLng> latLngs;
    private String contactCount;
    private String description;

    public ArrayList<LatLng> getLatLngs() {
        return latLngs;
    }

    public void setLatLngs(ArrayList<LatLng> latLngs) {
        this.latLngs = latLngs;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContactCount() {
        return contactCount;
    }

    public void setContactCount(String contactCount) {
        this.contactCount = contactCount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
