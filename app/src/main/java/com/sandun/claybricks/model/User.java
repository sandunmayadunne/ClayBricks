package com.sandun.claybricks.model;

public class User {

    private String user_id;
    private String user_name;
    private String user_last_name;
    private String user_email;
    private String user_mobile;
    private String status;
    private String province;
    private String district;
    private String city;
    private String line1;
    private String line2;

    public User() {
    }

    public User(String user_id, String user_name, String user_last_name, String user_email, String user_mobile, String status, String province, String district, String city, String line1, String line2) {
        this.user_id = user_id;
        this.user_name = user_name;
        this.user_last_name = user_last_name;
        this.user_email = user_email;
        this.user_mobile = user_mobile;
        this.status = status;
        this.province = province;
        this.district = district;
        this.city = city;
        this.line1 = line1;
        this.line2 = line2;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getUser_last_name() {
        return user_last_name;
    }

    public void setUser_last_name(String user_last_name) {
        this.user_last_name = user_last_name;
    }

    public String getUser_email() {
        return user_email;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }

    public String getUser_mobile() {
        return user_mobile;
    }

    public void setUser_mobile(String user_mobile) {
        this.user_mobile = user_mobile;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }
}
