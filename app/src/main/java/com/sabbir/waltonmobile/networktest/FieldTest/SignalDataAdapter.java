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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_signal_data, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SignalData data = signalDataList.get(position);

        // Split timestamp into time & date
        String[] tsParts = data.getTimestamp().split("\n");
        String time = tsParts.length > 0 ? tsParts[0] : data.getTimestamp();
        String date = tsParts.length > 1 ? tsParts[1] : "";

        holder.tvTime.setText("Time: " + time);
        holder.tvDate.setText("Date: " + date);
        holder.tvOperator.setText("Operator: " + data.getOperatorName());
        holder.tvLocation.setText("Location: " + data.getLocation());
        holder.tvDbm.setText("DBM: " + data.getDbm());
        holder.tvQuality.setText("Quality: " + data.getSignalQuality());

        // Color coding for signal quality
        int color = getSignalColor(data.getSignalQuality());
        holder.tvQuality.setTextColor(color);
        holder.tvDbm.setTextColor(color);
    }

    private int getSignalColor(String signalQuality) {
        switch (signalQuality) {
            case "Excellent": return Color.parseColor("#4CAF50");
            case "Good": return Color.parseColor("#8BC34A");
            case "Fair": return Color.parseColor("#FF9800");
            case "Poor": return Color.parseColor("#FF5722");
            case "Very Poor": return Color.parseColor("#F44336");
            default: return Color.parseColor("#757575");
        }
    }

    @Override
    public int getItemCount() {
        return signalDataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvDate, tvOperator, tvLocation, tvDbm, tvQuality;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvOperator = itemView.findViewById(R.id.tvOperator);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDbm = itemView.findViewById(R.id.tvDBM);
            tvQuality = itemView.findViewById(R.id.tvQuality);
        }
    }
}
