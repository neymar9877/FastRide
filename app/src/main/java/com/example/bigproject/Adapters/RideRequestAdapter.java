package com.example.bigproject.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bigproject.Models.RideRequest;
import com.example.bigproject.Models.User;
import com.example.bigproject.R;
import com.example.bigproject.Repositories.BaseRepo;
import com.example.bigproject.Repositories.UserRepo;

import java.util.List;

public class RideRequestAdapter extends RecyclerView.Adapter<RideRequestAdapter.RequestViewHolder> {

    // ממשק (Interface) המאפשר להעביר את אירועי הטיפול בבקשה (אישור/דחייה) חזרה לפרגמנט המארח
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

    // Task: Inflates the XML layout representing a single ride request row in the driver's list.
    // Input: parent (ViewGroup), viewType (int)
    // Output: RequestViewHolder
    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ride_request, parent, false);
        return new RequestViewHolder(view);
    }

    // Task: Binds trip metadata (time, coordinates, status) to UI components and triggers an asynchronous profile query to load the passenger's name.
    // Input: holder (RequestViewHolder), position (int)
    // Output: None
    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        RideRequest ride = data.get(position);

        // עיצוב ותצוגת זמן יצירת הבקשה - הסרת האות T וחיתוך מילישניות מיותרות
        String createdAt = ride.getCreatedAt();
        if (createdAt != null && createdAt.length() > 16) {
            createdAt = createdAt.substring(0, 16).replace("T", " ");
        }
        holder.tvCreatedAt.setText("🕐 " + (createdAt != null ? createdAt : "Unknown time"));

        // הצגת קואורדינטות האיסוף, היעד והסטטוס הנוכחי
        holder.tvPickup.setText("📍 Pickup: " + ride.getPickupLat() + ", " + ride.getPickupLng());
        holder.tvDropoff.setText("🏁 Drop-off: " + ride.getDropoffLat() + ", " + ride.getDropoffLng());
        holder.tvStatus.setText(ride.getStatus());

        // שליפת שם הנוסע מהשרת בצורה אסינכרונית לפי ה-ID שלו
        holder.tvPassengerName.setText("👤 Loading...");
        UserRepo userRepo = new UserRepo();
        userRepo.getUserById(ride.getPassengerId(), new BaseRepo.RepoCallback<User>() {
            @Override
            public void onSuccess(User result) {
                // העברת העדכון של ה-UI חזרה ל-Main Thread (החוט הראשי)
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
                // במקרה של שגיאה ברשת, הצגת טקסט ברירת מחדל על גבי ה-UI
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                        holder.tvPassengerName.setText("👤 Unknown passenger"));
            }
        });
    }

    // Task: Returns the total number of ride request entries registered inside the bound data list.
    // Input: None
    // Output: int
    @Override
    public int getItemCount() { return data.size(); }

    // מחלקת ViewHolder המחזיקה הפניות (References) לרכיבי ה-UI של כל בקשת נסיעה ברשימה
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