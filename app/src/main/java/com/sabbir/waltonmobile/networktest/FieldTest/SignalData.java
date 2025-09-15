package com.sabbir.waltonmobile.networktest.FieldTest;

public class SignalData {
    private String model;
    private String timestamp;
    private int dbm;
    private String operatorName;
    private String location;
    private String signalQuality;

    public SignalData(String model, String timestamp, int dbm, String operatorName, String location, String signalQuality) {
        this.model = model;
        this.timestamp = timestamp;
        this.dbm = dbm;
        this.operatorName = operatorName;
        this.location = location;
        this.signalQuality = signalQuality;
    }

    // Getters
    public String getModel() { return model; }
    public String getTimestamp() { return timestamp; }
    public int getDbm() { return dbm; }
    public String getOperatorName() { return operatorName; }
    public String getLocation() { return location; }
    public String getSignalQuality() { return signalQuality; }

    @Override
    public String toString() {
        return "Model: " + model + " | Time: " + timestamp.replace("\n", " ") +
                " | DBM: " + dbm + " | Operator: " + operatorName +
                " | Location: " + location + " | Signal: " + signalQuality;
    }
}
