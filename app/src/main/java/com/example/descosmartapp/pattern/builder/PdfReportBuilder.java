package com.example.descosmartapp.pattern.builder;

import android.content.Context;
import android.os.Environment;

import com.example.descosmartapp.db.MeterProfile;
import com.example.descosmartapp.model.BalanceResponse;
import com.example.descosmartapp.model.MonthlyResponse;
import com.example.descosmartapp.model.RechargeResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * BUILDER PATTERN — Step by step PDF report construction
 */
public class PdfReportBuilder {

    private final Context context;
    private MeterProfile meter;
    private BalanceResponse.Balance balance;
    private List<MonthlyResponse.MonthlyRecord> monthlyData;
    private List<RechargeResponse.RechargeRecord> rechargeData;
    private String reportTitle = "DESCO বিদ্যুৎ রিপোর্ট";

    private PdfReportBuilder(Context context) {
        this.context = context;
    }

    public static PdfReportBuilder newBuilder(Context context) {
        return new PdfReportBuilder(context);
    }

    public PdfReportBuilder setMeter(MeterProfile meter) {
        this.meter = meter;
        return this;
    }

    public PdfReportBuilder setBalance(BalanceResponse.Balance balance) {
        this.balance = balance;
        return this;
    }

    public PdfReportBuilder setMonthlyData(List<MonthlyResponse.MonthlyRecord> data) {
        this.monthlyData = data;
        return this;
    }

    public PdfReportBuilder setRechargeData(List<RechargeResponse.RechargeRecord> data) {
        this.rechargeData = data;
        return this;
    }

    public PdfReportBuilder setTitle(String title) {
        this.reportTitle = title;
        return this;
    }

    /** Build and save PDF — returns file path or null on failure */
    public String build() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("DESCO SMART APP - বিদ্যুৎ রিপোর্ট\n");
            sb.append("======================================\n\n");

            sb.append("রিপোর্ট তারিখ: ")
                    .append(new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(new Date()))
                    .append("\n\n");

            if (meter != null) {
                sb.append("মিটার তথ্য\n");
                sb.append("----------\n");
                sb.append("লেবেল: ").append(meter.label).append("\n");
                sb.append("Account No: ").append(meter.accountNo).append("\n");
                sb.append("Meter No: ").append(meter.meterNo).append("\n\n");
            }

            if (balance != null) {
                sb.append("ব্যালেন্স তথ্য\n");
                sb.append("----------\n");
                sb.append("বর্তমান ব্যালেন্স: ৳ ").append(balance.balance).append("\n");
                sb.append("এই মাসের ব্যবহার: ").append(balance.currentMonthConsumption).append(" kWh\n");
                sb.append("সর্বশেষ রিডিং: ").append(balance.readingTime).append("\n\n");
            }

            if (monthlyData != null && !monthlyData.isEmpty()) {
                sb.append("মাসিক ব্যবহার\n");
                sb.append("----------\n");
                for (MonthlyResponse.MonthlyRecord r : monthlyData) {
                    sb.append(r.month).append(": ").append(r.consumption)
                            .append(" kWh  |  ৳ ").append(r.amount).append("\n");
                }
                sb.append("\n");
            }

            if (rechargeData != null && !rechargeData.isEmpty()) {
                sb.append("রিচার্জ ইতিহাস\n");
                sb.append("----------\n");
                for (RechargeResponse.RechargeRecord r : rechargeData) {
                    sb.append(r.rechargeDate).append("  |  ৳ ").append(r.amount)
                            .append("  |  ").append(r.paymentMode).append("\n");
                }
            }

            // Save as .txt (readable as report); for true PDF use iText
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "DescoReports");
            if (!dir.exists()) dir.mkdirs();

            String fileName = "DESCO_Report_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date()) + ".txt";
            File file = new File(dir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes("UTF-8"));
            fos.close();

            return file.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}