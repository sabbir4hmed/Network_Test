package com.sabbir.waltonbd.wcmsproductchecker.Models;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    @SerializedName("id")
    private int id;

    @SerializedName("userName")
    private String userName;

    @SerializedName("name")
    private String name;

    @SerializedName("role")
    private String role;

    // Constructor
    public LoginResponse() {
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // Helper method to check if login was successful
    public boolean isSuccess() {
        return id > 0 && userName != null && !userName.isEmpty();
    }

    // Helper method for error message
    public String getMessage() {
        if (isSuccess()) {
            return "Login successful";
        }
        return "Login failed";
    }
}
