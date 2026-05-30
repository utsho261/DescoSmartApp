package com.example.descosmartapp.ui.adapters;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.descosmartapp.R;
import com.example.descosmartapp.model.RechargeResponse;
import java.util.List;

public class RechargeAdapter extends RecyclerView.Adapter<RechargeAdapter.VH> {

    private final List<RechargeResponse.RechargeRecord> list;

    public RechargeAdapter(List<RechargeResponse.RechargeRecord> list) { this.list = list; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recharge, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        RechargeResponse.RechargeRecord r = list.get(pos);
        h.tvDate.setText(r.rechargeDate);
        h.tvAmount.setText("৳ " + r.amount);
        h.tvMode.setText(r.paymentMode != null ? r.paymentMode : "N/A");
        h.tvTxn.setText("TXN: " + (r.transactionId != null ? r.transactionId : "N/A"));
    }

    @Override public int getItemCount() { return list.size(); }

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