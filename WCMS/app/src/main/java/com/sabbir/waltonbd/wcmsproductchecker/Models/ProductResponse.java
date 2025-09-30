package com.sabbir.waltonbd.wcmsproductchecker.Models;

import com.google.gson.annotations.SerializedName;

public class ProductResponse {

    @SerializedName("model")
    private String model;

    @SerializedName("mobileCode")
    private String mobileCode;

    @SerializedName("imei1")
    private String imei1;

    @SerializedName("imei2")
    private String imei2;

    @SerializedName("color")
    private String color;

    @SerializedName("grade")
    private String grade;

    @SerializedName("productionGrade")
    private String productionGrade;

    @SerializedName("gradeReson")
    private String gradeReason;

    @SerializedName("deliveryDate")
    private String deliveryDate;

    // Constructor
    public ProductResponse() {
    }

    // Getters and Setters
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getMobileCode() {
        return mobileCode;
    }

    public void setMobileCode(String mobileCode) {
        this.mobileCode = mobileCode;
    }

    public String getImei1() {
        return imei1;
    }

    public void setImei1(String imei1) {
        this.imei1 = imei1;
    }

    public String getImei2() {
        return imei2;
    }

    public void setImei2(String imei2) {
        this.imei2 = imei2;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getProductionGrade() {
        return productionGrade;
    }

    public void setProductionGrade(String productionGrade) {
        this.productionGrade = productionGrade;
    }

    public String getGradeReason() {
        return gradeReason;
    }

    public void setGradeReason(String gradeReason) {
        this.gradeReason = gradeReason;
    }

    public String getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(String deliveryDate) {
        this.deliveryDate = deliveryDate;
    }
}
