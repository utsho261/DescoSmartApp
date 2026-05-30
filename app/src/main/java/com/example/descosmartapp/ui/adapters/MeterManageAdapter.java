package com.example.descosmartapp.ui.adapters;

import android.content.Context;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.descosmartapp.R;
import com.example.descosmartapp.db.AppDatabase;
import com.example.descosmartapp.db.MeterProfile;

import java.util.List;

public class MeterManageAdapter extends RecyclerView.Adapter<MeterManageAdapter.VH> {

    private final List<MeterProfile> list;
    private final Context ctx;
    private final AppDatabase db;
    private final Runnable onRefresh;

    public MeterManageAdapter(List<MeterProfile> list, Context ctx, AppDatabase db, Runnable onRefresh) {
        this.list = list; this.ctx = ctx; this.db = db; this.onRefresh = onRefresh;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_meter_manage, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        MeterProfile m = list.get(pos);
        h.tvLabel.setText(m.label);
        h.tvAccount.setText("Account: " + m.accountNo);
        h.tvThreshold.setText("Alert সীমা: ৳ " + m.lowBalanceThreshold);
        h.tvActive.setText(m.isActive ? "✅ সক্রিয়" : "");

        h.btnRename.setOnClickListener(v -> showRenameDialog(m));
        h.btnThreshold.setOnClickListener(v -> showThresholdDialog(m));
        h.btnDelete.setOnClickListener(v -> confirmDelete(m));
    }

    @Override public int getItemCount() { return list.size(); }

    private void showRenameDialog(MeterProfile m) {
        EditText et = new EditText(ctx);
        et.setText(m.label);
        et.setSingleLine();
        new AlertDialog.Builder(ctx)
                .setTitle("মিটার নাম পরিবর্তন করুন")
                .setView(et)
                .setPositiveButton("সংরক্ষণ", (d, w) -> {
                    String newLabel = et.getText().toString().trim();
                    if (!newLabel.isEmpty()) {
                        m.label = newLabel;
                        db.meterDao().update(m);
                        onRefresh.run();
                    }
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void showThresholdDialog(MeterProfile m) {
        EditText et = new EditText(ctx);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setText(String.valueOf(m.lowBalanceThreshold));
        et.setHint("যেমন: 200");
        new AlertDialog.Builder(ctx)
                .setTitle("Low Balance Alert সীমা (৳)")
                .setMessage("এই পরিমাণের নিচে গেলে সতর্কতা আসবে")
                .setView(et)
                .setPositiveButton("সংরক্ষণ", (d, w) -> {
                    try {
                        m.lowBalanceThreshold = Double.parseDouble(et.getText().toString());
                        db.meterDao().update(m);
                        onRefresh.run();
                        Toast.makeText(ctx, "সীমা আপডেট হয়েছে: ৳ " + m.lowBalanceThreshold, Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException ignored) {}
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void confirmDelete(MeterProfile m) {
        new AlertDialog.Builder(ctx)
                .setTitle("মিটার মুছুন?")
                .setMessage("\"" + m.label + "\" মুছে ফেলা হবে। নিশ্চিত?")
                .setPositiveButton("হ্যাঁ, মুছুন", (d, w) -> {
                    db.meterDao().delete(m);
                    onRefresh.run();
                })
                .setNegativeButton("না", null)
                .show();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvLabel, tvAccount, tvThreshold, tvActive;
        Button btnRename, btnThreshold, btnDelete;
        VH(View v) {
            super(v);
            tvLabel     = v.findViewById(R.id.tvMeterLabel);
            tvAccount   = v.findViewById(R.id.tvMeterAccount);
            tvThreshold = v.findViewById(R.id.tvMeterThreshold);
            tvActive    = v.findViewById(R.id.tvMeterActive);
            btnRename   = v.findViewById(R.id.btnMeterRename);
            btnThreshold = v.findViewById(R.id.btnMeterThreshold);
            btnDelete   = v.findViewById(R.id.btnMeterDelete);
        }
    }
}