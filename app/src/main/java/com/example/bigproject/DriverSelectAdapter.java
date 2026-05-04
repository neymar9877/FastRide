package com.example.bigproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class DriverSelectAdapter extends RecyclerView.Adapter<DriverSelectAdapter.DriverViewHolder> {

    public interface OnDriverSelectedListener {
        void onDriverSelected(DriverWithUser driver);
    }

    private List<DriverWithUser> drivers;
    private OnDriverSelectedListener listener;
    private int selectedPosition = -1;

    public DriverSelectAdapter(List<DriverWithUser> drivers, OnDriverSelectedListener listener) {
        this.drivers = drivers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_driver_select, parent, false);
        return new DriverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverViewHolder holder, int position) {
        DriverWithUser driver = drivers.get(position);

        // Name
        if (driver.getUsers() != null) {
            holder.tvName.setText(driver.getUsers().getUserName());
            Glide.with(holder.imgDriver.getContext())
                    .load(driver.getUsers().getImageUrl())
                    .placeholder(R.drawable.baseline_directions_car_24)
                    .circleCrop()
                    .into(holder.imgDriver);
        } else {
            holder.tvName.setText("Driver");
        }

        // Distance
        double distKm = driver.getDistanceKm();
        if (distKm >= 0) {
            holder.tvDistance.setText(String.format("📍 %.1f km away", distKm));
            // Rough ETA: assume 30 km/h in city
            int etaMinutes = (int) Math.ceil((distKm / 30.0) * 60);
            holder.tvEta.setText(String.format("⏱ ~%d min away", etaMinutes));
        } else {
            holder.tvDistance.setText("Distance unknown");
            holder.tvEta.setText("");
        }

        // Highlight selected
        boolean isSelected = (position == selectedPosition);
        holder.ivSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.itemView.setBackgroundColor(isSelected ? 0xFFE3F2FD : 0xFFFFFFFF);

        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            listener.onDriverSelected(driver);
        });
    }

    @Override
    public int getItemCount() { return drivers.size(); }

    public static class DriverViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDistance, tvEta;
        ImageView imgDriver, ivSelected;

        public DriverViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvDriverSelectName);
            tvDistance = itemView.findViewById(R.id.tvDriverSelectDistance);
            tvEta = itemView.findViewById(R.id.tvDriverSelectEta);
            imgDriver = itemView.findViewById(R.id.imgDriverSelect);
            ivSelected = itemView.findViewById(R.id.ivSelected);
        }
    }
}
