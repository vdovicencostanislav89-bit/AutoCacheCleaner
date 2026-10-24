
package com.autocachecleaner.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int BG = Color.rgb(9, 14, 23);
    private static final int CARD = Color.rgb(21, 29, 42);
    private static final int TEAL = Color.rgb(45, 225, 205);
    private static final int WHITE = Color.rgb(240, 246, 252);
    private static final int MUTED = Color.rgb(155, 171, 190);
    private static final int ORANGE = Color.rgb(255, 185, 85);

    private static final int REQUEST_CLEAR_CACHE = 2101;
    private static final String GAME_PACKAGE = "com.mobile.legends";

    private LinearLayout content;
    private TextView dashboardTab;
    private TextView scanTab;
    private TextView gameTab;
    private TextView statusView;

    private boolean pendingCacheRequest = false;
    private String lastReport = "Сканирование ещё не выполнялось.";

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(12));
        root.setBackgroundColor(BG);
        setContentView(root);

        root.addView(text("AUTO CACHE CLEANER", 23, TEAL, true));

        TextView subtitle = text(
            "SYSTEM CONTROL  •  GAMING EDITION", 11, MUTED, true
        );
        subtitle.setPadding(0, dp(6), 0, dp(18));
        root.addView(subtitle);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(tabs);

        dashboardTab = tab("Главная");
        scanTab = tab("Сканер");
        gameTab = tab("Game Mode");

        tabs.addView(dashboardTab, weightParams());
        tabs.addView(scanTab, weightParams());
        tabs.addView(gameTab, weightParams());

        dashboardTab.setOnClickListener(v -> showDashboard());
        scanTab.setOnClickListener(v -> showScanner());
        gameTab.setOnClickListener(v -> showGameMode());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout.LayoutParams scrollParams =
            new LinearLayout.LayoutParams(-1, 0, 1);
        scrollParams.topMargin = dp(14);
        root.addView(scroll, scrollParams);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(10));
        scroll.addView(content);

        showDashboard();
    }

    private int dp(int value) {
        return (int) (value *
            getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(
        String value, int size, int color, boolean bold
    ) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(17), dp(16), dp(17));
        box.setBackground(round(CARD, 19));
        return box;
    }

    private void addCard(LinearLayout box) {
        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(12);
        content.addView(box, p);
    }

    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(0, dp(46), 1);
        p.setMargins(dp(3), 0, dp(3), 0);
        return p;
    }

    private TextView tab(String label) {
        TextView t = text(label, 12, WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(CARD, 13));
        return t;
    }

    private void selectTab(TextView selected) {
        dashboardTab.setBackground(round(CARD, 13));
        scanTab.setBackground(round(CARD, 13));
        gameTab.setBackground(round(CARD, 13));

        dashboardTab.setTextColor(MUTED);
        scanTab.setTextColor(MUTED);
        gameTab.setTextColor(MUTED);

        selected.setBackground(round(TEAL, 13));
        selected.setTextColor(BG);
    }

    private void addHeading(String heading, String description) {
        content.addView(text(heading, 23, WHITE, true));
        TextView d = text(description, 13, MUTED, false);
        d.setPadding(0, dp(6), 0, dp(16));
        content.addView(d);
    }

    private void addButton(String label, Runnable action) {
        TextView button = text(label, 13, BG, true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), dp(16), dp(14), dp(16));
        button.setBackground(round(TEAL, 15));
        button.setClickable(true);
        button.setFocusable(true);

        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(10);
        content.addView(button, p);
        button.setOnClickListener(v -> action.run());
    }

    private void addNote(String message) {
        TextView note = text(message, 12, MUTED, false);
        note.setPadding(dp(4), dp(6), dp(4), dp(16));
        content.addView(note);
    }

    private void showDashboard() {
        selectTab(dashboardTab);
        content.removeAllViews();
        addHeading("Обзор устройства", "Текущие показатели телефона");

        MemoryData data = readMemory();

        LinearLayout storage = card();
        addCard(storage);

        storage.addView(text("ВНУТРЕННЯЯ ПАМЯТЬ", 12, MUTED, true));
        storage.addView(text(
            Formatter.formatFileSize(this, data.used) + " занято",
            24, WHITE, true
        ));
        storage.addView(text(
            Formatter.formatFileSize(this, data.free)
                + " свободно из "
                + Formatter.formatFileSize(this, data.total),
            13, MUTED, false
        ));

        ProgressBar bar = new ProgressBar(
            this, null, android.R.attr.progressBarStyleHorizontal
        );
        bar.setMax(100);
        bar.setProgressTintList(
            android.content.res.ColorStateList.valueOf(
                data.percent >= 90 ? ORANGE : TEAL
            )
        );
        bar.setProgressBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                Color.rgb(47, 59, 73)
            )
        );
        LinearLayout.LayoutParams bp =
            new LinearLayout.LayoutParams(-1, dp(10));
        bp.topMargin = dp(14);
        storage.addView(bar, bp);

        bar.post(() ->
            android.animation.ObjectAnimator.ofInt(
                bar, "progress", 0, data.percent
            ).setDuration(850).start()
        );

        storage.addView(text(
            "\nЗаполнено: " + data.percent + "%",
            13, data.percent >= 90 ? ORANGE : TEAL, true
        ));

        ActivityManager.MemoryInfo ram = readRam();

        LinearLayout ramCard = card();
        addCard(ramCard);
        ramCard.addView(text("ОПЕРАТИВНАЯ ПАМЯТЬ", 12, TEAL, true));
        ramCard.addView(text(
            ram == null ? "Данные недоступны" :
                "Всего: " + Formatter.formatFileSize(this, ram.totalMem)
                + "\nДоступно: "
                + Formatter.formatFileSize(this, ram.availMem),
            15, WHITE, true
        ));

        LinearLayout device = card();
        addCard(device);
        device.addView(text("ИНФОРМАЦИЯ ОБ УСТРОЙСТВЕ",
            12, TEAL, true));
        device.addView(text(
            "\nПроизводитель: " + Build.MANUFACTURER
            + "\nМодель: " + Build.MODEL
            + "\nAndroid: " + Build.VERSION.RELEASE
            + "\nAPI: " + Build.VERSION.SDK_INT,
            14, MUTED, false
        ));

        addButton("СКАНИРОВАТЬ СИСТЕМУ", this::showScanner);
        addButton("ЗАПРОСИТЬ ОЧИСТКУ КЭША",
            this::requestCacheClear);
        addButton("ОТКРЫТЬ GAME MODE", this::showGameMode);

        addNote("Android ограничивает доступ к приватным данным "
            + "и кэшу других приложений.");
    }

    private void showScanner() {
        selectTab(scanTab);
        content.removeAllViews();
        addHeading("Smart Scan", "Проверка системных показателей");

        LinearLayout box = card();
        addCard(box);

        box.addView(text("РЕЗУЛЬТАТ СКАНИРОВАНИЯ",
            13, TEAL, true));

        ProgressBar progress = new ProgressBar(this);
        progress.setMax(100);
        progress.setProgressTintList(
            android.content.res.ColorStateList.valueOf(TEAL)
        );
        LinearLayout.LayoutParams pp =
            new LinearLayout.LayoutParams(-1, dp(9));
        pp.topMargin = dp(16);
        box.addView(progress, pp);

        statusView = text("Готов к сканированию", 14, MUTED, false);
        statusView.setPadding(0, dp(12), 0, dp(8));
        box.addView(statusView);

        TextView report = text(lastReport, 14, WHITE, false);
        report.setPadding(0, dp(10), 0, 0);
        box.addView(report);

        addButton("НАЧАТЬ СКАНИРОВАНИЕ", () -> {
            progress.setProgress(0);
            statusView.setText("Проверяем систему...");
            report.setText("");
            runScan(progress, report, 0);
        });

        addButton("ЗАПРОСИТЬ ОЧИСТКУ КЭША",
            this::requestCacheClear);

        addNote("Сканирование проверяет системные показатели. "
            + "Оно не может измерить весь кэш сторонних приложений.");
    }

    private void runScan(
        ProgressBar progress, TextView report, int step
    ) {
        int[] values = {20, 40, 65, 85, 100};
        String[] messages = {
            "Проверяем хранилище...",
            "Проверяем ОЗУ...",
            "Анализируем заполненность...",
            "Готовим рекомендации...",
            "Сканирование завершено"
        };

        if (step >= values.length) {
            lastReport = buildReport();
            report.setText(lastReport);
            statusView.setText("✓ Проверка завершена");
            return;
        }

        int from = progress.getProgress();
        android.animation.ObjectAnimator animation =
            android.animation.ObjectAnimator.ofInt(
                progress, "progress", from, values[step]
            );
        animation.setDuration(250);
        animation.start();

        statusView.setText(messages[step]);
        handler.postDelayed(
            () -> runScan(progress, report, step + 1), 300
        );
    }

    private String buildReport() {
        MemoryData data = readMemory();
        ActivityManager.MemoryInfo ram = readRam();

        StringBuilder report = new StringBuilder();
        report.append("ХРАНИЛИЩЕ\n")
            .append("Занято: ")
            .append(Formatter.formatFileSize(this, data.used))
            .append("\nСвободно: ")
            .append(Formatter.formatFileSize(this, data.free))
            .append("\nЗаполнено: ").append(data.percent).append("%\n\n");

        report.append("ОПЕРАТИВНАЯ ПАМЯТЬ\n");
        if (ram != null) {
            report.append("Всего: ")
                .append(Formatter.formatFileSize(this, ram.totalMem))
                .append("\nДоступно: ")
                .append(Formatter.formatFileSize(this, ram.availMem))
                .append("\n");
        } else {
            report.append("Показатели недоступны.\n");
        }

        report.append("\nРЕКОМЕНДАЦИИ\n");

        if (data.percent >= 90) {
            report.append("• Хранилище почти заполнено. "
                + "Проверь крупные файлы и приложения.\n");
        } else if (data.percent >= 75) {
            report.append("• Проверь свободное место "
                + "при необходимости.\n");
        } else {
            report.append("• Критического заполнения "
                + "хранилища не обнаружено.\n");
        }

        if (ram != null && ram.lowMemory) {
            report.append("• Android сообщает о низком запасе ОЗУ. "
                + "Закрой ненужные приложения.\n");
        } else {
            report.append("• Android не сообщает о критически "
                + "низкой доступной ОЗУ.\n");
        }

        report.append("\nРазмер кэша сторонних приложений "
            + "не определён: Android ограничивает доступ.");

        return report.toString();
    }

    private void showGameMode() {
        selectTab(gameTab);
        content.removeAllViews();
        addHeading("Game Mode", "Подготовка к Mobile Legends");

        LinearLayout hero = card();
        addCard(hero);
        hero.addView(text("MOBILE LEGENDS", 22, TEAL, true));
        hero.addView(text(
            "\nИгровые инструменты без root-доступа.",
            14, MUTED, false
        ));

        addButton("ЗАПУСТИТЬ MOBILE LEGENDS", () -> {
            Intent launch = getPackageManager()
                .getLaunchIntentForPackage(GAME_PACKAGE);

            if (launch != null) {
                startActivity(launch);
            } else {
                toast("Игра не найдена. Проверь, установлена ли она.");
            }
        });

        addButton("НАСТРОЙКИ ЭКРАНА", () ->
            openSettings(Settings.ACTION_DISPLAY_SETTINGS));

        addButton("НАСТРОЙКИ ПРИЛОЖЕНИЯ ИГРЫ", () -> {
            try {
                startActivity(new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + GAME_PACKAGE)
                ));
            } catch (Exception e) {
                openSettings(Settings.ACTION_SETTINGS);
            }
        });

        LinearLayout tips = card();
        addCard(tips);
        tips.addView(text("СОВЕТЫ ДЛЯ ИГРЫ", 13, TEAL, true));
        tips.addView(text(
            "\n01  Закрой ненужные приложения.\n\n"
            + "02  Выбери подходящие настройки графики и FPS.\n\n"
            + "03  При нагреве снизь нагрузку и дай телефону остыть.\n\n"
            + "04  Используй стабильное интернет-соединение.\n\n"
            + "05  Проверь игровой режим производителя телефона.",
            14, WHITE, false
        ));

        addNote("Приложение не разгоняет процессор "
            + "и не гарантирует увеличение FPS.");
    }

    private void requestCacheClear() {
        if (Build.VERSION.SDK_INT < 30) {
            toast("Этот системный механизм требует Android 11 или новее.");
            openSettings(Settings.ACTION_SETTINGS);
            return;
        }

        if (!Environment.isExternalStorageManager()) {
            pendingCacheRequest = true;

            try {
                startActivity(new Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                ));

                toast("Проверь специальный доступ в настройках Android. "
                    + "Он не гарантирует разрешение на очистку кэша.");
            } catch (Exception e) {
                pendingCacheRequest = false;
                toast("Не удалось открыть настройки доступа.");
                openSettings(Settings.ACTION_SETTINGS);
            }

            return;
        }

        launchCacheClearRequest();
    }

    private void launchCacheClearRequest() {
        try {
            Intent intent = new Intent(
                StorageManager.ACTION_CLEAR_APP_CACHE
            );
            startActivityForResult(intent, REQUEST_CLEAR_CACHE);
        } catch (Exception e) {
            toast("Android не разрешил открыть системную очистку.");
            openSettings(Settings.ACTION_SETTINGS);
        }
    }

    @Override
    protected void onActivityResult(
        int requestCode, int resultCode, Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CLEAR_CACHE) {
            toast("Запрос завершён. Обнови показатели памяти, "
                + "чтобы проверить изменения.");
        }
    }

    private void openSettings(String action) {
        try {
            startActivity(new Intent(action));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {
                toast("Не удалось открыть настройки.");
            }
        }
    }

    private void toast(String message) {
        Toast.makeText(
            this, message, Toast.LENGTH_LONG
        ).show();
    }

    private MemoryData readMemory() {
        try {
            StatFs fs = new StatFs(
                Environment.getDataDirectory().getPath()
            );

            long total = fs.getTotalBytes();
            long free = fs.getAvailableBytes();
            long used = Math.max(0, total - free);

            int percent = total > 0
                ? (int) Math.min(
                    100, Math.round(used * 100.0 / total)
                ) : 0;

            return new MemoryData(total, free, used, percent);
        } catch (Exception e) {
            return new MemoryData(0, 0, 0, 0);
        }
    }

    private ActivityManager.MemoryInfo readRam() {
        try {
            ActivityManager manager =
                (ActivityManager) getSystemService(ACTIVITY_SERVICE);

            ActivityManager.MemoryInfo info =
                new ActivityManager.MemoryInfo();

            manager.getMemoryInfo(info);
            return info;
        } catch (Exception e) {
            return null;
        }
    }

    private static class MemoryData {
        final long total;
        final long free;
        final long used;
        final int percent;

        MemoryData(long total, long free, long used, int percent) {
            this.total = total;
            this.free = free;
            this.used = used;
            this.percent = percent;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (pendingCacheRequest) {
            pendingCacheRequest = false;

            if (Build.VERSION.SDK_INT >= 30
                    && Environment.isExternalStorageManager()) {
                launchCacheClearRequest();
            } else {
                toast("Специальный доступ не предоставлен. "
                    + "Сканер остаётся доступен.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
