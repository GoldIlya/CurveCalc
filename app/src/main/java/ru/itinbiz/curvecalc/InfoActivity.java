package ru.itinbiz.curvecalc;

import static ru.itinbiz.curvecalc.ZamerActivity.PREFS_NAME;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);


        Button telegramButton = findViewById(R.id.btnTelega);
        Button websiteButton = findViewById(R.id.btnUpdate);
        Button resetButton = findViewById(R.id.btnReset);
        telegramButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Relzcontract"));
                startActivity(intent);
            }
        });

        websiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://apps.rustore.ru/app/ru.itinbiz.curvecalc"));
                startActivity(intent);
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Сбрасываем все настройки
                SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();

                // Уведомляем пользователя
                Toast.makeText(InfoActivity.this, "Все настройки сброшены", Toast.LENGTH_SHORT).show();
            }
        });

    }
    }