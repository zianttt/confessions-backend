package com.example.Confessions.dto;

import java.util.List;

public class DataHandler {

    private List<Long> submitIds;

    public DataHandler(){

    }

    public DataHandler(List<Long> submitIds) {
        this.submitIds = submitIds;
    }

    public List<Long> getSubmitIds() {
        return submitIds;
    }

    public void setSubmitIds(List<Long> submitIds) {
        this.submitIds = submitIds;
    }
}
