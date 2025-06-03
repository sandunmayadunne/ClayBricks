package com.sandun.claybricks.model;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import java.util.List;
import java.util.Map;

public class CompletedOrder {
    private Timestamp deliveryDate;
    private GeoPoint deliveryLocation;
    private String deliveryPrice;
    private double distanceKm;
    private List<Map<String, Object>> items;
    private String orderId;
    private Timestamp orderDate;
    private String totalPrice;
    private String userName;

    // Constructor
    public CompletedOrder() {}

    // Getters and setters
    public Timestamp getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(Timestamp deliveryDate) { this.deliveryDate = deliveryDate; }
    public GeoPoint getDeliveryLocation() { return deliveryLocation; }
    public void setDeliveryLocation(GeoPoint deliveryLocation) { this.deliveryLocation = deliveryLocation; }
    public String getDeliveryPrice() { return deliveryPrice; }
    public void setDeliveryPrice(String deliveryPrice) { this.deliveryPrice = deliveryPrice; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Timestamp getOrderDate() { return orderDate; }
    public void setOrderDate(Timestamp orderDate) { this.orderDate = orderDate; }
    public String getTotalPrice() { return totalPrice; }
    public void setTotalPrice(String totalPrice) { this.totalPrice = totalPrice; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}