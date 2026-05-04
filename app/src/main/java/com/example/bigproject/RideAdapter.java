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

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.RideViewHolder> {

    public interface OnItemActionListener {
        void onEdit(DriverWithUser driver, int position);
        void onDelete(DriverWithUser driver, int position);
        void onClick(DriverWithUser driver, int position);

    }

    private List<DriverWithUser> data;
    private OnItemActionListener listener;

    public RideAdapter(List<DriverWithUser> data, OnItemActionListener listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.driver_item, parent, false);
        return new RideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        DriverWithUser driver = data.get(position);

        // Username from USERS table
        holder.txtDriver.setText(driver.getUsers().getUserName());

        // Driver fields from DRIVERS table
        holder.txtDestination.setText(driver.getCurrentLocation());
        holder.txtStatus.setText(driver.getStatus());


        // Load the image from the DB
        Glide.with(holder.imgRide.getContext())
                .load(driver.getUsers().getImageUrl())
                .into(holder.imgRide);

        holder.itemView.setOnClickListener(v -> listener.onClick(driver, position));
    }


    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class RideViewHolder extends RecyclerView.ViewHolder {
        TextView txtDriver, txtDestination, txtStatus;
        ImageView imgRide;

        public RideViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDriver = itemView.findViewById(R.id.tvDriverName);
            txtDestination = itemView.findViewById(R.id.tvCurrLocation);
            txtStatus = itemView.findViewById(R.id.tvStatus);
            imgRide = itemView.findViewById(R.id.imgDriver);
        }
    }
}

