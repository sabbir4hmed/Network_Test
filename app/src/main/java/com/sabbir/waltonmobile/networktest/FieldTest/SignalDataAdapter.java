package com.sabbir.waltonmobile.networktest.FieldTest;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sabbir.waltonmobile.networktest.R;

import java.util.List;

public class SignalDataAdapter extends RecyclerView.Adapter<SignalDataAdapter.ViewHolder> {

    private List<SignalData> signalDataList;

    public SignalDataAdapter(List<SignalData> signalDataList) {
        this.signalDataList = signalDataList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_signal_data, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SignalData data = signalDataList.get(position);

        holder.tvModel.setText(data.getModel());
        holder.tvTimestamp.setText(data.getTimestamp());
        holder.tvDbm.setText(String.valueOf(data.getDbm()));
        holder.tvOperator.setText(data.getOperatorName());
        holder.tvLocation.setText(data.getLocation());
        holder.tvSignalQuality.setText(data.getSignalQuality());

        // Color coding based on signal quality
        int color = getSignalColor(data.getSignalQuality());
        holder.tvSignalQuality.setTextColor(color);
        holder.tvDbm.setTextColor(color);
    }

    private int getSignalColor(String signalQuality) {
        switch (signalQuality) {
            case "Excellent":
                return Color.parseColor("#4CAF50"); // Green
            case "Good":
                return Color.parseColor("#8BC34A"); // Light Green
            case "Fair":
                return Color.parseColor("#FF9800"); // Orange
            case "Poor":
                return Color.parseColor("#FF5722"); // Red Orange
            case "Very Poor":
                return Color.parseColor("#F44336"); // Red
            default:
                return Color.parseColor("#757575"); // Gray
        }
    }

    @Override
    public int getItemCount() {
        return signalDataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvModel, tvTimestamp, tvDbm, tvOperator, tvLocation, tvSignalQuality;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvModel = itemView.findViewById(R.id.tvModel);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvDbm = itemView.findViewById(R.id.tvDbm);
            tvOperator = itemView.findViewById(R.id.tvOperator);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvSignalQuality = itemView.findViewById(R.id.tvSignalQuality);
        }
    }
}
