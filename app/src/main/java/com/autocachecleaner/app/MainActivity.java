
package com.autocachecleaner.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private final int BG = Color.rgb(10, 15, 23);
    private final int CARD = Color.rgb(22, 30, 42);
    private final int TEAL = Color.rgb(40, 225, 205);
    private final int WHITE = Color.rgb(240, 246, 250);
    private final int MUTED = Color.rgb(155, 171, 188);

    private LinearLayout root;
    private TextView storageText;
    private TextView ramText;
    private TextView status;
    private ProgressBar storageBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(28));
        scroll.addView(root);
        setContentView(scroll);

        TextView title = text("AUTO CACHE CLEANER", 24, TEAL, true);
        root.addView(title);

        TextView subtitle = text(
            "Умное управление памятью", 15, MUTED, false
        );
        subtitle.setPadding(0, dp(7), 0, dp(22));
        root.addView(subtitle);

        LinearLayout storageCard = card();
        root.addView(storageCard);

        storageCard.addView(text(
            "ВНУТРЕННЯЯ ПАМЯТЬ", 13, MUTED, true
        ));

        storageText = text("Загрузка...", 18, WHITE, true);
        storageText.setPadding(0, dp(14), 0, dp(12));
        storageCard.addView(storageText);

        storageBar = new ProgressBar(
            this, null, android.R.attr.progressBarStyleHorizontal
        );
        storageBar.setMax(100);
        storageBar.setProgressTintList(
            android.content.res.ColorStateList.valueOf(TEAL)
        );
        storageBar.setProgressBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                Color.rgb(48, 60, 72)
            )
        );
        storageCard.addView(storageBar);

        LinearLayout ramCard = card();
        LinearLayout.LayoutParams ramParams = new LinearLayout.LayoutParams(
            -1, -2
        );
        ramParams.topMargin = dp(14);
        root.addView(ramCard, ramParams);

        ramCard.addView(text(
            "ОПЕРАТИВНАЯ ПАМЯТЬ (ОЗУ)", 13, MUTED, true
        ));

        ramText = text("Загрузка...", 18, WHITE, true);
        ramText.setPadding(0, dp(12), 0, 0);
        ramCard.addView(ramText);

        status = text("Готов к работе", 14, TEAL, true);
        status.setPadding(0, dp(20), 0, dp(8));
        root.addView(status);

        addButton("⟳  ОБНОВИТЬ ДАННЫЕ", new Runnable() {
            @Override
            public void run() {
                refreshInfo();
            }
        });

        addButton("⌕  ПОПРОБОВАТЬ ОЧИСТКУ КЭША",
            new Runnable() {
                @Override
                public void run() {
                    requestCacheClear();
                }
            });

        addButton("⚙  НАСТРОЙКИ ПРИЛОЖЕНИЙ",
            new Runnable() {
                @Override
                public void run() {
                    openAppSettings();
                }
            });

        LinearLayout infoCard = card();
        LinearLayout.LayoutParams infoParams =
            new LinearLayout.LayoutParams(-1, -2);
        infoParams.topMargin = dp(18);
        root.addView(infoCard, infoParams);

        infoCard.addView(text("ИНФОРМАЦИЯ ОБ УСТРОЙСТВЕ",
            13, TEAL, true));

        infoCard.addView(text(
            "\nМодель: " + Build.MANUFACTURER + " " + Build.MODEL
            + "\nAndroid: " + Build.VERSION.RELEASE
            + "\n\nОчистка кэша зависит от возможностей Android. "
            + "Приложение не показывает выдуманные результаты "
            + "и не гарантирует удаление кэша всех приложений.",
            14, MUTED, false
        ));

        refreshInfo();
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(17), dp(18), dp(17), dp(18));
        box.setBackground(roundRect(CARD, 20));
        return box;
    }

    private TextView text(
        String value, int size, int color, boolean bold
    ) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(null, Typeface.BOLD);
        }
        return view;
    }

    private void addButton(String label, final Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setTextColor(BG);
        button.setAllCaps(false);
        button.setTypeface(null, Typeface.BOLD);
        button.setPadding(dp(12), dp(10), dp(12), dp(10));
        button.setBackground(roundRect(TEAL, 16));

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(10);
        root.addView(button, params);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });
    }

    private GradientDrawable roundRect(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private void refreshInfo() {
        try {
            StatFs fs = new StatFs(
                Environment.getDataDirectory().getPath()
            );

            long total = fs.getTotalBytes();
            long free = fs.getAvailableBytes();
            long used = total - free;

            int percent = total > 0
                ? (int) ((used * 100L) / total) : 0;

            storageText.setText(
                Formatter.formatFileSize(this, used) + " занято"
                + "\n" + Formatter.formatFileSize(this, free)
                + " свободно"
                + "\n" + percent + "% использовано"
            );
            storageBar.setProgress(percent);

            ActivityManager manager =
                (ActivityManager) getSystemService(ACTIVITY_SERVICE);

            ActivityManager.MemoryInfo mi =
                new ActivityManager.MemoryInfo();

            manager.getMemoryInfo(mi);

            long ramTotal = mi.totalMem;
            long ramFree = mi.availMem;
            long ramUsed = ramTotal - ramFree;

            ramText.setText(
                Formatter.formatFileSize(this, ramUsed)
                + " используется системой и приложениями"
                + "\nСвободно: "
                + Formatter.formatFileSize(this, ramFree)
                + "\nВсего: "
                + Formatter.formatFileSize(this, ramTotal)
            );

            status.setText("✓ Данные обновлены");
        } catch (Exception e) {
            status.setText("Не удалось получить все данные");
        }
    }

    private void requestCacheClear() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                Intent intent = new Intent(
                    StorageManager.ACTION_CLEAR_APP_CACHE
                );
                startActivity(intent);
                status.setText(
                    "Запрос передан Android. Следуй указаниям системы."
                );
            } catch (Exception e) {
                status.setText(
                    "Открой системные настройки хранилища."
                );
                openAppSettings();
            }
        } else {
            status.setText(
                "Открой настройки для управления приложениями."
            );
            openAppSettings();
        }
    }

    private void openAppSettings() {
        try {
            startActivity(new Intent(
                Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS
            ));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private int dp(int value) {
        return (int) (value * getResources()
            .getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (storageText != null) {
            refreshInfo();
        }
    }
}
