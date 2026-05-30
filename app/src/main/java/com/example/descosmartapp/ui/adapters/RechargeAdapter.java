package com.example.descosmartapp.ui.adapters;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.descosmartapp.R;
import com.example.descosmartapp.model.RechargeResponse;
import java.util.List;

public class RechargeAdapter extends RecyclerView.Adapter<RechargeAdapter.VH> {

    private final List<RechargeResponse.RechargeRecord> list;

    public RechargeAdapter(List<RechargeResponse.RechargeRecord> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recharge, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        RechargeResponse.RechargeRecord r = list.get(pos);

        // Date — শুধু তারিখ দেখাও, সময় বাদ দাও
        String date = r.rechargeDate != null
                ? r.rechargeDate.replace("T", " ").split("\\.")[0]
                : "N/A";
        h.tvDate.setText(date);

        // Amount
        h.tvAmount.setText("৳ " + String.format("%.0f", r.amount));

        // Energy
        if (r.energyAmount > 0) {
            h.tvMode.setText(String.format("%.2f kWh  |  %s",
                    r.energyAmount,
                    r.paymentMode != null ? r.paymentMode : ""));
        } else {
            h.tvMode.setText(r.paymentMode != null ? r.paymentMode : "");
        }

        // Transaction ID
        h.tvTxn.setText(r.transactionId != null && !r.transactionId.isEmpty()
                ? "TXN: " + r.transactionId : "");
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate, tvAmount, tvMode, tvTxn;

        VH(View v) {
            super(v);
            tvDate   = v.findViewById(R.id.tvRechargeDate);
            tvAmount = v.findViewById(R.id.tvRechargeAmount);
            tvMode   = v.findViewById(R.id.tvRechargeMode);
            tvTxn    = v.findViewById(R.id.tvRechargeTxn);
        }
    }
}