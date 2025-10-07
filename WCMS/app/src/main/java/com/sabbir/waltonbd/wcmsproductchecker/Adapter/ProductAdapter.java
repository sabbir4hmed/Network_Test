package com.sabbir.waltonbd.wcmsproductchecker.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sabbir.waltonbd.wcmsproductchecker.Models.ProductResponse;
import com.sabbir.waltonbd.wcmsproductchecker.R;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<ProductResponse> productList;

    public ProductAdapter() {
        this.productList = new ArrayList<>();
    }

    public void setProduct(ProductResponse product) {
        this.productList.clear();
        this.productList.add(product);
        notifyDataSetChanged();
    }

    public void clearProducts() {
        this.productList.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_detail, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductResponse product = productList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {

        private TextView tvModel, tvMobileCode, tvImei1, tvImei2, tvColor;
        private TextView tvGrade, tvProductionGrade, tvGradeReason, tvDeliveryDate, tvBoxCode;
        private LinearLayout layoutGradeReason;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvModel = itemView.findViewById(R.id.tvModel);
            tvMobileCode = itemView.findViewById(R.id.tvMobileCode);
            tvImei1 = itemView.findViewById(R.id.tvImei1);
            tvImei2 = itemView.findViewById(R.id.tvImei2);
            tvColor = itemView.findViewById(R.id.tvColor);
            tvGrade = itemView.findViewById(R.id.tvGrade);
            tvProductionGrade = itemView.findViewById(R.id.tvProductionGrade);
            tvGradeReason = itemView.findViewById(R.id.tvGradeReason);
            tvDeliveryDate = itemView.findViewById(R.id.tvDeliveryDate);
            tvBoxCode = itemView.findViewById(R.id.tvBoxCode);
            layoutGradeReason = itemView.findViewById(R.id.layoutGradeReason);
        }

        public void bind(ProductResponse product) {
            tvModel.setText(product.getModel() != null ? product.getModel() : "N/A");
            tvMobileCode.setText(product.getMobileCode() != null ? product.getMobileCode() : "N/A");
            tvImei1.setText(product.getImei1() != null ? product.getImei1() : "N/A");
            tvImei2.setText(product.getImei2() != null ? product.getImei2() : "N/A");
            tvColor.setText(product.getColor() != null ? product.getColor() : "N/A");
            tvGrade.setText(product.getGrade() != null ? product.getGrade() : "N/A");
            tvProductionGrade.setText(product.getProductionGrade() != null ? product.getProductionGrade() : "N/A");
            tvDeliveryDate.setText(product.getDeliveryDate() != null ? product.getDeliveryDate() : "N/A");
            tvBoxCode.setText(product.getBoxCode() != null ? product.getBoxCode() : "N/A");

            // Show grade reason if exists
            if (product.getGradeReason() != null && !product.getGradeReason().isEmpty()) {
                layoutGradeReason.setVisibility(View.VISIBLE);
                tvGradeReason.setText(product.getGradeReason());
            } else {
                layoutGradeReason.setVisibility(View.GONE);
            }
        }
    }
}