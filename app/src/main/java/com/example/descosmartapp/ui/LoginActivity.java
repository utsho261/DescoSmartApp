package com.example.descosmartapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.descosmartapp.R;
import com.example.descosmartapp.db.AppDatabase;
import com.example.descosmartapp.db.MeterProfile;
import com.example.descosmartapp.model.CustomerResponse;
import com.example.descosmartapp.pattern.facade.DescoServiceFacade;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    EditText etInput, etLabel;
    Button btnVerify;
    ProgressBar progress;
    RadioGroup rgType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etInput  = findViewById(R.id.etInput);
        etLabel  = findViewById(R.id.etLabel);
        btnVerify = findViewById(R.id.btnVerify);
        progress = findViewById(R.id.progressLogin);
        rgType   = findViewById(R.id.rgType);

        btnVerify.setOnClickListener(v -> {
            String input = etInput.getText().toString().trim();
            String label = etLabel.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(this, "Account / Meter Number দিন", Toast.LENGTH_SHORT).show();
                return;
            }
            if (label.isEmpty()) label = "আমার মিটার";
            verifyAndSave(input, label);
        });
    }

    private void verifyAndSave(String input, String label) {
        progress.setVisibility(View.VISIBLE);
        btnVerify.setEnabled(false);

        final String finalLabel = label;

        DescoServiceFacade.getInstance().smartFetch(input, new Callback<CustomerResponse>() {
            @Override
            public void onResponse(Call<CustomerResponse> call, Response<CustomerResponse> response) {
                progress.setVisibility(View.GONE);
                btnVerify.setEnabled(true);

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().data != null) {

                    CustomerResponse.Customer c = response.body().data;

                    MeterProfile meter = new MeterProfile();
                    meter.label     = finalLabel;
                    meter.accountNo = c.accountNo;
                    meter.meterNo   = c.meterNo;
                    meter.lowBalanceThreshold = 200.0; // default ৳200
                    meter.isActive  = true;
                    meter.meterState = "ACTIVE";
                    meter.createdAt = System.currentTimeMillis();

                    AppDatabase.getInstance(LoginActivity.this).meterDao().insert(meter);

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this,
                            "মিটার খুঁজে পাওয়া যায়নি। নম্বর চেক করুন।",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<CustomerResponse> call, Throwable t) {
                progress.setVisibility(View.GONE);
                btnVerify.setEnabled(true);
                Toast.makeText(LoginActivity.this,
                        "নেটওয়ার্ক সমস্যা: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}