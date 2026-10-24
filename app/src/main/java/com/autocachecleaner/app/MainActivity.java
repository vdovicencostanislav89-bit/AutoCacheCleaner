package com.autocachecleaner.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.Build;
import android.os.storage.StorageManager;
import android.content.Intent;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.format.Formatter;
import java.io.File;

public class MainActivity extends Activity {

    private LinearLayout root;
    private TextView memory;
    private TextView status;

    private final int BG = Color.rgb(13, 19, 27);
    private final int TEAL = Color.rgb(32, 214, 199);
    private final int MUTED = Color.rgb(165, 180, 195);

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(24));
        scroll.addView(root);
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("AUTO CACHE CLEANER");
        title.setTextColor(TEAL);
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Управление памятью телефона");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(15);
        subtitle.setPadding(0, dp(8), 0, dp(24));
        root.addView(subtitle);

        memory = new TextView(this);
        memory.setTextColor(Color.WHITE);
        memory.setTextSize(19);
        memory.setPadding(dp(16), dp(16), dp(16), dp(16));
        memory.setBackgroundColor(Color.rgb(25, 35, 46));
        root.addView(memory);

        status = new TextView(this);
        status.setTextColor(TEAL);
        status.setText("Готов к работе");
        status.setPadding(0, dp(12), 0, dp(12));
        root.addView(status);

        addButton("ОБНОВИТЬ ПАМЯТЬ", new Runnable() {
            @Override
            public void run() {
                updateMemory();
            }
        });

        addButton("ПОПРОБОВАТЬ ОЧИСТКУ КЭША", new Runnable() {
            @Override
            public void run() {
                clearCacheRequest();
            }
        });

        addButton("НАСТРОЙКИ ПРИЛОЖЕНИЙ", new Runnable() {
            @Override
            public void run() {
                openSettings();
            }
        });

        TextView note = new TextView(this);
        note.setTextColor(MUTED);
        note.setTextSize(13);
        note.setPadding(0, dp(20), 0, 0);
        note.setText(
            "Android может ограничивать массовую очистку кэша. " +
            "Приложение не может гарантировать удаление всех " +
            "временных файлов или выполнять его скрытно."
        );
        root.addView(note);

        updateMemory();
    }

    private void addButton(String text, final Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(BG);
        button.setTextSize(13);
        button.setTypeface(null, Typeface.BOLD);
        button.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(TEAL)
        );

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(10);
        root.addView(button, params);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                action.run();
            }
        });
    }

    private void updateMemory() {
        try {
            StatFs fs = new StatFs(
                Environment.getDataDirectory().getPath()
            );

            long total = fs.getTotalBytes();
            long free = fs.getAvailableBytes();
            long used = total - free;

            memory.setText(
                "ВНУТРЕННЯЯ ПАМЯТЬ\n\n" +
                "Занято: " + Formatter.formatFileSize(this, used) +
                "\nСвободно: " + Formatter.formatFileSize(this, free) +
                "\nВсего: " + Formatter.formatFileSize(this, total)
            );
            status.setText("Информация обновлена");
        } catch (Exception e) {
            memory.setText("Не удалось прочитать память");
            status.setText("Ошибка чтения");
        }
    }

    private void clearCacheRequest() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                Intent intent = new Intent(
                    StorageManager.ACTION_CLEAR_APP_CACHE
                );
                startActivity(intent);
                status.setText("Запрос передан Android");
            } catch (Exception e) {
                status.setText(
                    "Массовая очистка недоступна. Открой настройки."
                );
                openSettings();
            }
        } else {
            status.setText(
                "Открой настройки для управления хранилищем."
            );
            openSettings();
        }
    }

    private void openSettings() {
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
        if (memory != null) {
            updateMemory();
        }
    }
}
