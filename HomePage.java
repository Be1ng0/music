package com.example.musicgenerator;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class HomePage extends AppCompatActivity {

    private RadioGroup styleRadioGroup;
    private RadioGroup instrumentRadioGroup;
    private String selectedStyle = "pop";
    private String selectedInstrument = "piano";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // 初始化风格选择
        styleRadioGroup = findViewById(R.id.styleRadioGroup);
        styleRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton radioButton = findViewById(checkedId);
            selectedStyle = radioButton.getText().toString().toLowerCase();
            Toast.makeText(HomePage.this, "选择风格: " + selectedStyle, Toast.LENGTH_SHORT).show();
        });

        // 初始化乐器选择
        instrumentRadioGroup = findViewById(R.id.instrumentRadioGroup);
        instrumentRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton radioButton = findViewById(checkedId);
            selectedInstrument = radioButton.getText().toString().toLowerCase();
            Toast.makeText(HomePage.this, "选择乐器: " + selectedInstrument, Toast.LENGTH_SHORT).show();
        });

        // 录音按钮
        Button recordButton = findViewById(R.id.recordButton);
        recordButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, RecordActivity.class);
            intent.putExtra("style", selectedStyle);
            intent.putExtra("instrument", selectedInstrument);
            startActivity(intent);
        });
    }
}