package com.sandun.claybricks.model;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.Date;
import java.util.List;
import java.util.Map;

@IgnoreExtraProperties
public class Order {

    private String orderId;
    private String deliveryId;
    private String deliveryStatus;
    private String status;
    private String userId;
    private String userName;
    private String userMobile;
    private String totalPrice;
    private List<Map<String, Object>> items;
    private String originalOrderId;

    private String paymentStatus;
    private Date paymentDate;
    private String deliveryPrice;

    public Order() {
    }

    public Order(String orderId, String status, String userId, String userName, String userMobile, String totalPrice) {
        this.orderId = orderId;
        this.status = status;
        this.userId = userId;
        this.userName = userName;
        this.userMobile = userMobile;
        this.totalPrice = totalPrice;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getDeliveryId() { return deliveryId; }
    public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }

    public String getStatus() {
        return status;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }
    public String getOriginalOrderId() { return originalOrderId; }
    public void setOriginalOrderId(String originalOrderId) { this.originalOrderId = originalOrderId; }

    public String getDeliveryPrice() { return deliveryPrice; }
    public void setDeliveryPrice(String deliveryPrice) { this.deliveryPrice = deliveryPrice; }
}
