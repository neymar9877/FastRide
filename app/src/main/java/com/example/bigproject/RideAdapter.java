package com.example.bigproject;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.List;

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.RideViewHolder> {

    // ממשק (Interface) להגדרת פעולות אינטראקטיביות על פריטי הרשימה (עריכה, מחיקה או לחיצה רגילה)
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

    // Task: Inflates the XML layout representing a single driver item card inside the list.
    // Input: parent (ViewGroup), viewType (int)
    // Output: RideViewHolder
    @NonNull
    @Override
    public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.driver_item, parent, false);
        return new RideViewHolder(view);
    }

    // Task: Binds consolidated driver and user row details to the UI controls, implements image load error fallbacks, and hooks item view click listeners.
    // Input: holder (RideViewHolder), position (int)
    // Output: None
    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        DriverWithUser driver = data.get(position);

        // הצגת שם המשתמש השמור בטבלת המשתמשים (Users)
        holder.txtDriver.setText(driver.getUsers().getUserName());

        // הצגת נתוני הנהג השמורים בטבלת הנהגים (Drivers)
        holder.txtDestination.setText(driver.getCurrentLocation());
        holder.txtStatus.setText(driver.getStatus());

        // טעינת תמונת הפרופיל של הנהג בצורה אסינכרונית עם טיפול בשגיאות טעינה
        Glide.with(holder.imgRide.getContext())
                .load(driver.getUsers().getImageUrl())
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        // הדפסת שגיאה ל-Logcat במידה והקישור לתמונה שבור או שאין חיבור לרשת
                        Log.e("ImageLoad", "Failed to load image for driver: "
                                + driver.getUsers().getUserName() + " → showing default");

                        // הצגת תמונת רכב כברירת מחדל במקרה של כישלון
                        holder.imgRide.setImageResource(R.drawable.baseline_directions_car_24);
                        return true; // החזרת true מסמנת ל-Glide שטיפלנו בשגיאה בעצמנו
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        return false; // החזרת false מאפשרת ל-Glide להמשיך ולהציג את התמונה כרגיל
                    }
                })
                .placeholder(R.drawable.baseline_directions_car_24) // תמונה זמנית עד לסיום הטעינה
                .circleCrop() // עיגול פינות התמונה
                .into(holder.imgRide);

        // הגדרת מאזין ללחיצה על כל שורת נהג ברשימה
        holder.itemView.setOnClickListener(v -> listener.onClick(driver, position));
    }

    // Task: Returns the total count of driver entries populated inside the current data collection source list.
    // Input: None
    // Output: int
    @Override
    public int getItemCount() {
        return data.size();
    }

    // מחלקת ViewHolder המחזיקה הפניות (References) לרכיבי ה-UI הוויזואליים של כל שורת נהג
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