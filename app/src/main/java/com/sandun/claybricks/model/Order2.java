// Updated Order2 model class
package com.sandun.claybricks.model;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class Order2 {
    private String orderId;
    private String status;
    private Date orderDate;
    private String userName;
    private String userMobile;
    private GeoPoint deliveryLocation;
    private String deliveryPrice;  // Changed to String
    private double distanceKm;
    private String totalPrice;     // Changed to String
    private List<Map<String, Object>> items;
    private double pricePerKm;
    private String userId;

    // Getters and setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserMobile() {
        return userMobile;
    }

    public void setUserMobile(String userMobile) {
        this.userMobile = userMobile;
    }

    public GeoPoint getDeliveryLocation() {
        return deliveryLocation;
    }

    public void setDeliveryLocation(GeoPoint deliveryLocation) {
        this.deliveryLocation = deliveryLocation;
    }

    public String getDeliveryPrice() {
        return deliveryPrice;
    }

    public void setDeliveryPrice(String deliveryPrice) {
        this.deliveryPrice = deliveryPrice;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(String totalPrice) {
        this.totalPrice = totalPrice;
    }

    public List<Map<String, Object>> getItems() {
        return items;
    }

    public void setItems(List<Map<String, Object>> items) {
        this.items = items;
    }

    public double getPricePerKm() {
        return pricePerKm;
    }

    public void setPricePerKm(double pricePerKm) {
        this.pricePerKm = pricePerKm;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}