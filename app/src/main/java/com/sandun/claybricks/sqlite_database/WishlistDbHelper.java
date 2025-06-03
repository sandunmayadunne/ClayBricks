package com.sandun.claybricks.sqlite_database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sandun.claybricks.model.Product;

public class WishlistDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "claybricks.db";
    private static final int DATABASE_VERSION = 1;

    // Wishlist table
    public static final String TABLE_WISHLIST = "wishlist";
    public static final String COLUMN_MOBILE = "user_mobile";
    public static final String COLUMN_PRODUCT_ID = "product_id";
    public static final String COLUMN_PRODUCT_NAME = "product_name";
    public static final String COLUMN_PRODUCT_PRICE = "product_price";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    public WishlistDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_WISHLIST_TABLE = "CREATE TABLE " + TABLE_WISHLIST + "("
                + COLUMN_MOBILE + " TEXT,"
                + COLUMN_PRODUCT_ID + " TEXT,"
                + COLUMN_PRODUCT_NAME + " TEXT,"
                + COLUMN_PRODUCT_PRICE + " TEXT,"
                + COLUMN_TIMESTAMP + " INTEGER,"
                + "PRIMARY KEY (" + COLUMN_MOBILE + ", " + COLUMN_PRODUCT_ID + ")"
                + ")";
        db.execSQL(CREATE_WISHLIST_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WISHLIST);
        onCreate(db);
    }

    public long addToWishlist(String mobile, Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MOBILE, mobile);
        values.put(COLUMN_PRODUCT_ID, product.getProduct_id());
        values.put(COLUMN_PRODUCT_NAME, product.getProduct_name());
        values.put(COLUMN_PRODUCT_PRICE, product.getProduct_price());
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());

        // Insert or replace if exists
        return db.insertWithOnConflict(TABLE_WISHLIST, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public boolean isInWishlist(String mobile, String productId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_WISHLIST
                + " WHERE " + COLUMN_MOBILE + " = ?"
                + " AND " + COLUMN_PRODUCT_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{mobile, productId});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public int removeFromWishlist(String mobile, String productId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_WISHLIST,
                COLUMN_MOBILE + " = ? AND " + COLUMN_PRODUCT_ID + " = ?",
                new String[]{mobile, productId});
    }

    public int getWishlistCount(String userMobile) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_WISHLIST
                + " WHERE " + COLUMN_MOBILE + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{userMobile});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    // Optional: Method to get all wishlist items for a user
    public Cursor getAllWishlistItems(String userMobile) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_WISHLIST,
                new String[]{COLUMN_PRODUCT_ID, COLUMN_PRODUCT_NAME, COLUMN_PRODUCT_PRICE},
                COLUMN_MOBILE + " = ?",
                new String[]{userMobile},
                null, null,
                COLUMN_TIMESTAMP + " DESC");
    }
}