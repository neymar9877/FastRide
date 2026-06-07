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

    // מאזין (Interface) המאפשר להעביר את אירוע בחירת הנהג לפרגמנט או לאקטיביטי המארח
    public interface OnDriverSelectedListener {
        void onDriverSelected(DriverWithUser driver);
    }

    private List<DriverWithUser> drivers;
    private OnDriverSelectedListener listener;
    private int selectedPosition = -1; // שמירת המיקום של הנהג שנבחר כרגע (1- ברירת מחדל: אף אחד)

    public DriverSelectAdapter(List<DriverWithUser> drivers, OnDriverSelectedListener listener) {
        this.drivers = drivers;
        this.listener = listener;
    }

    // Task: Inflates the XML layout representing a single driver option row in the selection list.
    // Input: parent (ViewGroup), viewType (int)
    // Output: DriverViewHolder
    @NonNull
    @Override
    public DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_driver_select, parent, false);
        return new DriverViewHolder(view);
    }

    // Task: Binds driver details (name, profile picture, calculated ETA, driver Price, and distance) to the UI components and updates the row selection highlights.
    // Input: holder (DriverViewHolder), position (int)
    // Output: None
    @Override
    public void onBindViewHolder(@NonNull DriverViewHolder holder, int position) {
        DriverWithUser driver = drivers.get(position);

        // טעינת שם הנהג ותמונת הפרופיל שלו
        if (driver.getUsers() != null) {
            holder.tvName.setText(driver.getUsers().getUserName());
            Glide.with(holder.imgDriver.getContext())
                    .load(driver.getUsers().getImageUrl())
                    .placeholder(R.drawable.baseline_directions_car_24) // תמונת ברירת מחדל עד שהתמונה נטענת
                    .circleCrop() // חיתוך התמונה לצורה מעגלית
                    .into(holder.imgDriver);
        } else {
            holder.tvName.setText("Driver");
        }

        // חישוב והצגת מרחק וזמן הגעה משוער (ETA)
        double distKm = driver.getDistanceKm();
        if (distKm >= 0) {
            holder.tvDistance.setText(String.format("📍 %.1f km away", distKm));
            // חישוב זמנים משוער: הנחה של מהירות ממוצעת 30 קמ"ש בעיר
            int etaMinutes = (int) Math.ceil((distKm / 30.0) * 60);
            holder.tvEta.setText(String.format("⏱ ~%d min away", etaMinutes));

            // ===== מחיר נסיעה =====
            // נוסחה: 10 שח תוספת אישית + 3.5 שח לכל ק"מ + 0.5 שח לכל דקת המתנה (ETA)
            // זו נוסחה שמחקה תעריפי מונית ישראליים
            double price = 10.0 + (distKm * 3.5) + (etaMinutes * 0.5);
            holder.tvPrice.setText(String.format("💰 ~%.0f ₪", price));


        } else {
            holder.tvDistance.setText("Distance unknown");
            holder.tvEta.setText("");
            holder.tvPrice.setText("");
        }

        // ניהול נראות ויזואלית עבור השורה שנבחרה
        boolean isSelected = (position == selectedPosition);
        holder.ivSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.itemView.setBackgroundColor(isSelected ? 0xFFE3F2FD : 0xFFFFFFFF); // רקע כחלחל לנהג נבחר, לבן לאחרים

        // האזנה ללחיצה על שורת נהג ועדכון המיקומים בהתאם
        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            // רענון השורה הקודמת והשורה החדשה שנבחרה כדי לעדכן את העיצוב שלהן
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);

            // הפעלת פונקציית הכלבק להעברת הנהג הנבחר
            listener.onDriverSelected(driver);
        });
    }

    // Task: Returns the total number of driver items registered inside the bound data stream source list.
    // Input: None
    // Output: int
    @Override
    public int getItemCount() { return drivers.size(); }

    // מחלקת ViewHolder המחזיקה הפניות (References) לרכיבי ה-UI של כל שורה ברשימה
    public static class DriverViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDistance, tvEta, tvPrice;
        ImageView imgDriver, ivSelected;

        public DriverViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvDriverSelectName);
            tvDistance = itemView.findViewById(R.id.tvDriverSelectDistance);
            tvEta = itemView.findViewById(R.id.tvDriverSelectEta);
            tvPrice    = itemView.findViewById(R.id.tvDriverSelectPrice);
            imgDriver = itemView.findViewById(R.id.imgDriverSelect);
            ivSelected = itemView.findViewById(R.id.ivSelected);
        }
    }
}