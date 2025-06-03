package com.sandun.claybricks.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Product implements Parcelable {
    private String product_id;
    private String product_name;
    private String product_width;
    private String product_height;
    private String product_length;
    private String product_type;
    private String product_weight;
    private String product_quantity;
    private String ImageUrl;
    private String product_price;
    private String product_status;

    public Product() {}

    public Product(String product_id, String product_name, String product_width, String product_height,
                   String product_length, String product_type, String product_weight, String product_quantity,
                   String imageUrl, String product_price, String product_status) {
        this.product_id = product_id;
        this.product_name = product_name;
        this.product_width = product_width;
        this.product_height = product_height;
        this.product_length = product_length;
        this.product_type = product_type;
        this.product_weight = product_weight;
        this.product_quantity = product_quantity;
        this.ImageUrl = imageUrl;
        this.product_price = product_price;
        this.product_status = product_status;
    }

    protected Product(Parcel in) {
        product_id = in.readString();
        product_name = in.readString();
        product_width = in.readString();
        product_height = in.readString();
        product_length = in.readString();
        product_type = in.readString();
        product_weight = in.readString();
        product_quantity = in.readString();
        ImageUrl = in.readString();
        product_price = in.readString();
        product_status = in.readString();
    }

    public static final Creator<Product> CREATOR = new Creator<Product>() {
        @Override
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        @Override
        public Product[] newArray(int size) {
            return new Product[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(product_id);
        dest.writeString(product_name);
        dest.writeString(product_width);
        dest.writeString(product_height);
        dest.writeString(product_length);
        dest.writeString(product_type);
        dest.writeString(product_weight);
        dest.writeString(product_quantity);
        dest.writeString(ImageUrl);
        dest.writeString(product_price);
        dest.writeString(product_status);
    }

    // Getters and Setters
    public String getProduct_id() {
        return product_id;
    }

    public void setProduct_id(String product_id) {
        this.product_id = product_id;
    }

    public String getProduct_name() {
        return product_name;
    }

    public void setProduct_name(String product_name) {
        this.product_name = product_name;
    }

    public String getProduct_width() {
        return product_width;
    }

    public void setProduct_width(String product_width) {
        this.product_width = product_width;
    }

    public String getProduct_height() {
        return product_height;
    }

    public void setProduct_height(String product_height) {
        this.product_height = product_height;
    }

    public String getProduct_length() {
        return product_length;
    }

    public void setProduct_length(String product_length) {
        this.product_length = product_length;
    }

    public String getProduct_type() {
        return product_type;
    }

    public void setProduct_type(String product_type) {
        this.product_type = product_type;
    }

    public String getProduct_weight() {
        return product_weight;
    }

    public void setProduct_weight(String product_weight) {
        this.product_weight = product_weight;
    }

    public String getProduct_quantity() {
        return product_quantity;
    }

    public void setProduct_quantity(String product_quantity) {
        this.product_quantity = product_quantity;
    }

    public String getImageUrl() {
        return ImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        ImageUrl = imageUrl;
    }

    public String getProduct_price() {
        return product_price;
    }

    public void setProduct_price(String product_price) {
        this.product_price = product_price;
    }

    public String getProduct_status() {
        return product_status;
    }

    public void setProduct_status(String product_status) {
        this.product_status = product_status;
    }
}