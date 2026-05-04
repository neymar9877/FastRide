package com.example.bigproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class RideRequestAdapter extends RecyclerView.Adapter<RideRequestAdapter.RequestViewHolder> {

    public interface OnRideActionListener {
        void onAccept(RideRequest ride, int position);
        void onDecline(RideRequest ride, int position);
    }

    private List<RideRequest> data;
    private OnRideActionListener listener;

    public RideRequestAdapter(List<RideRequest> data, OnRideActionListener listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ride_request, parent, false);
        return new RequestViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        RideRequest ride = data.get(position);

        // Format time - remove the T and trim milliseconds
        String createdAt = ride.getCreatedAt();
        if (createdAt != null && createdAt.length() > 16) {
            createdAt = createdAt.substring(0, 16).replace("T", " ");
        }
        holder.tvCreatedAt.setText("🕐 " + (createdAt != null ? createdAt : "Unknown time"));

        holder.tvPickup.setText("📍 Pickup: " + ride.getPickupLat() + ", " + ride.getPickupLng());
        holder.tvDropoff.setText("🏁 Drop-off: " + ride.getDropoffLat() + ", " + ride.getDropoffLng());
        holder.tvStatus.setText(ride.getStatus());

        // Fetch passenger name
        holder.tvPassengerName.setText("👤 Loading...");
        UserRepo userRepo = new UserRepo();
        userRepo.getUserById(ride.getPassengerId(), new BaseRepo.RepoCallback<User>() {
            @Override
            public void onSuccess(User result) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (result != null) {
                        holder.tvPassengerName.setText("👤 " + result.getUserName());
                    } else {
                        holder.tvPassengerName.setText("👤 Unknown passenger");
                    }
                });
            }
            @Override
            public void onError(Exception error) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                        holder.tvPassengerName.setText("👤 Unknown passenger"));
            }
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvPassengerName, tvPickup, tvDropoff, tvStatus, tvCreatedAt;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            tvPassengerName = itemView.findViewById(R.id.tvPassengerName);
            tvPickup = itemView.findViewById(R.id.tvPickup);
            tvDropoff = itemView.findViewById(R.id.tvDropoff);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
