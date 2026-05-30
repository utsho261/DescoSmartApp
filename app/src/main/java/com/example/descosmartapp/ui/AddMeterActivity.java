package com.example.descosmartapp.ui;

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

public class AddMeterActivity extends AppCompatActivity {

    EditText etInput, etLabel;
    Button btnAdd;
    ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_meter);

        etInput  = findViewById(R.id.etInput);
        etLabel  = findViewById(R.id.etLabel);
        btnAdd   = findViewById(R.id.btnAdd);
        progress = findViewById(R.id.progressAdd);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("নতুন মিটার যোগ করুন");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        btnAdd.setOnClickListener(v -> {
            String input = etInput.getText().toString().trim();
            String label = etLabel.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(this, "Account / Meter Number দিন", Toast.LENGTH_SHORT).show();
                return;
            }
            if (label.isEmpty()) label = "মিটার " + (AppDatabase.getInstance(this).meterDao().getCount() + 1);
            addMeter(input, label);
        });
    }

    private void addMeter(String input, String label) {
        progress.setVisibility(View.VISIBLE);
        btnAdd.setEnabled(false);
        final String finalLabel = label;

        DescoServiceFacade.getInstance().smartFetch(input, new Callback<CustomerResponse>() {
            @Override
            public void onResponse(Call<CustomerResponse> call, Response<CustomerResponse> response) {
                progress.setVisibility(View.GONE);
                btnAdd.setEnabled(true);

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().data != null) {

                    CustomerResponse.Customer c = response.body().data;
                    MeterProfile meter = new MeterProfile();
                    meter.label     = finalLabel;
                    meter.accountNo = c.accountNo;
                    meter.meterNo   = c.meterNo;
                    meter.lowBalanceThreshold = 200.0;
                    meter.isActive  = false;
                    meter.meterState = "ACTIVE";
                    meter.createdAt = System.currentTimeMillis();

                    AppDatabase.getInstance(AddMeterActivity.this).meterDao().insert(meter);
                    Toast.makeText(AddMeterActivity.this, finalLabel + " যোগ হয়েছে!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddMeterActivity.this, "মিটার পাওয়া যায়নি।", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<CustomerResponse> call, Throwable t) {
                progress.setVisibility(View.GONE);
                btnAdd.setEnabled(true);
                Toast.makeText(AddMeterActivity.this, "সংযোগ ব্যর্থ হয়েছে।", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}