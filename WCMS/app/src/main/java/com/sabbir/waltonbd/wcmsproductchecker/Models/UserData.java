package com.sabbir.waltonbd.wcmsproductchecker.Models;

import com.google.gson.annotations.SerializedName;

public class UserData {

    @SerializedName("id")
    private String id;

    @SerializedName("employee_id")
    private String employeeId;

    @SerializedName("username")
    private String username;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("department")
    private String department;

    @SerializedName("role")
    private String role;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
