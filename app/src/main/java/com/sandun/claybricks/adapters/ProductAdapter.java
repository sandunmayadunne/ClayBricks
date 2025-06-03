//package com.sandun.claybricks.adapters;
//
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.util.Base64;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.sandun.claybricks.R;
//import com.sandun.claybricks.model.Product;
//import com.sandun.claybricks.navigation.ProductUpdateActivity;
//
//import java.util.List;
//
//public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
//    private Context context;
//    private List<Product> productList;
//
//    public ProductAdapter(Context context, List<Product> productList) {
//        this.context = context;
//        this.productList = productList;
//    }
//
//    @NonNull
//    @Override
//    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(context).inflate(R.layout.view_product_box, parent, false);
//        return new ProductViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
//        Product product = productList.get(position);
//        holder.productName.setText(product.getProduct_name());
//
//        // Decode Base64 image
//        byte[] decodedString = Base64.decode(product.getProduct_imageBase64(), Base64.DEFAULT);
//        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
//        holder.productImage.setImageBitmap(decodedByte);
//
//        // Set click listener to open ProductUpdateActivity
//        holder.itemView.setOnClickListener(view -> {
//            Intent intent = new Intent(context, ProductUpdateActivity.class);
//            intent.putExtra("productName", product.getProduct_name());
//            intent.putExtra("productWidth", product.getProduct_width());
//            intent.putExtra("productHeight", product.getProduct_height());
//            intent.putExtra("productLength", product.getProduct_length());
//            intent.putExtra("productType", product.getProduct_type());
//            intent.putExtra("productWeight", product.getProduct_weight());
//            intent.putExtra("productQuantity", product.getProduct_quantity());
//            intent.putExtra("productImage", product.getProduct_imageBase64());
//            context.startActivity(intent);
//        });
//    }
//
//    @Override
//    public int getItemCount() {
//        return productList.size();
//    }
//
//    public static class ProductViewHolder extends RecyclerView.ViewHolder {
//        ImageView productImage;
//        TextView productName;
//
//        public ProductViewHolder(@NonNull View itemView) {
//            super(itemView);
//            productImage = itemView.findViewById(com.sandun.claybricks.R.id.productImage);
//            productName = itemView.findViewById(R.id.productName);
//        }
//    }
//}


package com.sandun.claybricks.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sandun.claybricks.R;
import com.sandun.claybricks.model.Product;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
    private Context context;
    private List<Product> productList;

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_product_box, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);

        // Set Product Name
        holder.productName.setText(product.getProduct_name());

        // Decode Base64 image and set to ImageView
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            byte[] decodedBytes = Base64.decode(product.getImageUrl(), Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            holder.productImage.setImageBitmap(decodedBitmap);
        } else {
            holder.productImage.setImageResource(R.drawable.add_icon); // Default image
        }
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView productName;
        ImageView productImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productTypeName);
            productImage = itemView.findViewById(R.id.productImage111);
        }
    }
}
