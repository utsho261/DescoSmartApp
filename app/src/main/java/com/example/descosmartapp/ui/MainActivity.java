package com.example.descosmartapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.descosmartapp.R;
import com.example.descosmartapp.model.BalanceResponse;
import com.example.descosmartapp.model.CustomerResponse;
import com.example.descosmartapp.repository.DescoRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    TextView txtName, txtMeterNo, txtContact, txtAddress,
            txtFeeder, txtPhase, txtBalance, txtConsumption, txtReadingTime;
    EditText etAccountNo;
    Button btnSearch;
    ProgressBar progressBar;
    DescoRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etAccountNo    = findViewById(R.id.etAccountNo);
        btnSearch      = findViewById(R.id.btnSearch);
        progressBar    = findViewById(R.id.progressBar);
        txtName        = findViewById(R.id.txtName);
        txtMeterNo     = findViewById(R.id.txtMeterNo);
        txtContact     = findViewById(R.id.txtContact);
        txtAddress     = findViewById(R.id.txtAddress);
        txtFeeder      = findViewById(R.id.txtFeeder);
        txtPhase       = findViewById(R.id.txtPhase);
        txtBalance     = findViewById(R.id.txtBalance);
        txtConsumption = findViewById(R.id.txtConsumption);
        txtReadingTime = findViewById(R.id.txtReadingTime);

        repo = new DescoRepository();

        btnSearch.setOnClickListener(v -> {
            String accountNo = etAccountNo.getText().toString().trim();
            if (accountNo.isEmpty()) {
                Toast.makeText(this, "Account number likhun", Toast.LENGTH_SHORT).show();
                return;
            }
            loadData(accountNo);
        });
    }

    private void loadData(String accountNo) {
        progressBar.setVisibility(View.VISIBLE);
        btnSearch.setEnabled(false);
        clearFields();

        repo.getCustomerInfo(accountNo, new Callback<CustomerResponse>() {
            @Override
            public void onResponse(Call<CustomerResponse> call, Response<CustomerResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnSearch.setEnabled(true);

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().data != null) {

                    CustomerResponse.Customer c = response.body().data;
                    txtName.setText("নাম: " + safe(c.customerName));
                    txtMeterNo.setText("মিটার নং: " + safe(c.meterNo));
                    txtContact.setText("মোবাইল: " + safe(c.contactNo));
                    txtAddress.setText("ঠিকানা: " + safe(c.installationAddress));
                    txtFeeder.setText("ফিডার: " + safe(c.feederName));
                    txtPhase.setText("ফেজ: " + safe(c.phaseType));

                } else {
                    Toast.makeText(MainActivity.this,
                            "Customer পাওয়া যায়নি",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<CustomerResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSearch.setEnabled(true);
                Toast.makeText(MainActivity.this,
                        "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        repo.getBalance(accountNo, new Callback<BalanceResponse>() {
            @Override
            public void onResponse(Call<BalanceResponse> call, Response<BalanceResponse> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().data != null) {

                    BalanceResponse.Balance b = response.body().data;
                    txtBalance.setText("ব্যালেন্স: ৳ " + b.balance);
                    txtConsumption.setText("এই মাসের ব্যবহার: " + b.currentMonthConsumption + " kWh");
                    txtReadingTime.setText("সর্বশেষ রিডিং: " + safe(b.readingTime));
                }
            }

            @Override
            public void onFailure(Call<BalanceResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this,
                        "Balance Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearFields() {
        txtName.setText("");        txtMeterNo.setText("");
        txtContact.setText("");     txtAddress.setText("");
        txtFeeder.setText("");      txtPhase.setText("");
        txtBalance.setText("");     txtConsumption.setText("");
        txtReadingTime.setText("");
    }

    private String safe(String s) {
        return (s != null && !s.isEmpty()) ? s : "N/A";
    }
}