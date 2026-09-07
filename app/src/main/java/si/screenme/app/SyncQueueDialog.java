package si.screenme.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

final class SyncQueueDialog {
    private SyncQueueDialog() {}

    static void show(Activity activity, Runnable onChanged, Runnable onChooseFolder) {
        Storage.refreshStatus(activity);
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(Ui.dp(activity, 6), Ui.dp(activity, 4),
                Ui.dp(activity, 6), Ui.dp(activity, 4));

        TextView state = Ui.title(activity, "Pripravljeno");
        panel.addView(state);
        ProgressBar progress = new ProgressBar(activity, null,
                android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(false);
        panel.addView(progress, new LinearLayout.LayoutParams(-1, Ui.dp(activity, 18)));
        Ui.margin(progress, 0, 12, 0, 0);
        TextView counts = Ui.text(activity, "", 14, Ui.MUTED);
        panel.addView(counts);
        Ui.margin(counts, 0, 10, 0, 0);
        TextView current = Ui.text(activity, "", 14, Ui.INK);
        panel.addView(current);
        Ui.margin(current, 0, 8, 0, 0);
        TextView error = Ui.text(activity, "", 13, Ui.RED);
        panel.addView(error);
        Ui.margin(error, 0, 8, 0, 0);
        TextView chooseFolder = Ui.button(activity, "NASTAVI OBLAČNO MAPO", false);
        panel.addView(chooseFolder);
        Ui.margin(chooseFolder, 0, 8, 0, 0);

        TextView destination = Ui.text(activity, "", 13, Ui.MUTED);
        destination.setPadding(Ui.dp(activity, 12), Ui.dp(activity, 10),
                Ui.dp(activity, 12), Ui.dp(activity, 10));
        destination.setBackground(Ui.shape(0xFFF3F0FA, 12, activity));
        panel.addView(destination);
        Ui.margin(destination, 0, 8, 0, 0);

        LinearLayout historyHeader = Ui.row(activity);
        TextView historyLabel = Ui.label(activity, "PRENESENE DATOTEKE");
        historyHeader.addView(historyLabel, Ui.weight(0, -2, 1));
        TextView historyCount = Ui.label(activity, "0 PRENOSOV");
        historyCount.setTextColor(Ui.GREEN);
        historyHeader.addView(historyCount);
        panel.addView(historyHeader);
        Ui.margin(historyHeader, 0, 18, 0, 8);
        LinearLayout historyList = new LinearLayout(activity);
        historyList.setOrientation(LinearLayout.VERTICAL);
        panel.addView(historyList);

        LinearLayout first = Ui.row(activity);
        TextView pause = Ui.button(activity, "PAVZA", false);
        TextView start = Ui.button(activity, "ZAŽENI", true);
        first.addView(pause, Ui.weight(0, Ui.dp(activity, 50), 1));
        first.addView(start, Ui.weight(0, Ui.dp(activity, 50), 1));
        Ui.margin(start, 8, 0, 0, 0);
        panel.addView(first);
        Ui.margin(first, 0, 16, 0, 0);

        LinearLayout second = Ui.row(activity);
        TextView restart = Ui.button(activity, "ZAŽENI ZNOVA", false);
        TextView stop = Ui.button(activity, "USTAVI", false);
        stop.setTextColor(Ui.RED);
        second.addView(restart, Ui.weight(0, Ui.dp(activity, 50), 1));
        second.addView(stop, Ui.weight(0, Ui.dp(activity, 50), 1));
        Ui.margin(stop, 8, 0, 0, 0);
        panel.addView(second);
        Ui.margin(second, 0, 8, 0, 0);

        TextView refresh = Ui.button(activity, "OSVEŽI OKNO", false);
        panel.addView(refresh);
        Ui.margin(refresh, 0, 8, 0, 0);
        TextView deleteQueue = Ui.button(activity, "IZBRIŠI ČAKALNO VRSTO", false);
        deleteQueue.setTextColor(Ui.RED);
        panel.addView(deleteQueue);
        Ui.margin(deleteQueue, 0, 8, 0, 0);
        TextView clearHistory = Ui.button(activity, "POČISTI VSE POSLANE", false);
        panel.addView(clearHistory);

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(panel);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Pošiljanje v Drive")
                .setView(scroll)
                .setPositiveButton("Zapri", null)
                .create();

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable[] updater = new Runnable[1];
        String[] historyStamp = new String[]{null};
        updater[0] = () -> {
            if (!dialog.isShowing()) return;
            SharedPreferences prefs = activity.getSharedPreferences("screenme", 0);
            int pending = prefs.getInt(Storage.PREF_PENDING, Storage.countPending(activity));
            int total = Math.max(prefs.getInt(Storage.PREF_BATCH_TOTAL, 0),
                    prefs.getInt(Storage.PREF_BATCH_DONE, 0) + pending);
            int done = Math.min(total, prefs.getInt(Storage.PREF_BATCH_DONE, 0));
            int fileIndex = prefs.getInt(Storage.PREF_FILE_INDEX, 0);
            int fileTotal = prefs.getInt(Storage.PREF_FILE_TOTAL, 0);
            long fileBytes = prefs.getLong(Storage.PREF_FILE_BYTES, 0);
            long fileSize = prefs.getLong(Storage.PREF_FILE_SIZE, 0);
            int max = Math.max(100, total * 100);
            int value = done * 100;
            if (pending > 0 && fileIndex > 0 && fileTotal > 0) {
                int filePercent = fileSize <= 0 ? 0
                        : (int) Math.min(100, fileBytes * 100 / fileSize);
                value += (Math.max(0, fileIndex - 1) * 100 + filePercent) / fileTotal;
            }
            progress.setMax(max);
            progress.setProgress(Math.min(max, value));

            String mode = SyncScheduler.state(activity);
            String problem = prefs.getString(Storage.PREF_ERROR, "");
            boolean folder = !prefs.getString("syncTree", "").isEmpty();
            chooseFolder.setText(folder ? "SPREMENI OBLAČNO MAPO" : "NASTAVI OBLAČNO MAPO");
            destination.setText(folder ? "Ciljna mapa  ·  " + Storage.syncDestinationName(activity)
                    : "Izberi mapo, da se bodo novi in obstoječi zapisi prenesli v oblak.");
            if (!folder) {
                state.setText("Oblačna mapa ni nastavljena");
                state.setTextColor(Ui.AMBER);
            } else if (SyncScheduler.PAUSED.equals(mode)) {
                state.setText("Pošiljanje je na pavzi");
                state.setTextColor(Ui.AMBER);
            } else if (SyncScheduler.STOPPED.equals(mode)) {
                state.setText("Pošiljanje je ustavljeno");
                state.setTextColor(Ui.RED);
            } else if (!problem.isEmpty()) {
                state.setText("Napaka pri pošiljanju");
                state.setTextColor(Ui.RED);
            } else if (pending > 0) {
                state.setText(prefs.getString(Storage.PREF_CURRENT, "").isEmpty()
                        ? "Čaka na omrežje ali začetek" : "Pošiljanje poteka");
                state.setTextColor(Ui.PURPLE);
            } else {
                state.setText("Čakalna vrsta je prazna");
                state.setTextColor(Ui.GREEN);
            }
            counts.setText("Končano: " + done + " od " + total + "  ·  Čaka: " + pending);
            String record = prefs.getString(Storage.PREF_CURRENT, "");
            String file = prefs.getString(Storage.PREF_CURRENT_FILE, "");
            current.setText(record.isEmpty() ? "" : "Trenutno: " + record
                    + (file.isEmpty() ? "" : "\nDatoteka " + fileIndex + " od "
                    + fileTotal + ": " + file + (fileSize <= 0 ? "" : "  ·  "
                    + size(fileBytes) + " / " + size(fileSize))));
            current.setVisibility(record.isEmpty() ? View.GONE : View.VISIBLE);
            error.setText(problem.isEmpty() ? "" : "⚠ " + problem);
            error.setVisibility(problem.isEmpty() ? View.GONE : View.VISIBLE);
            String rawHistory = prefs.getString(Storage.PREF_HISTORY, "");
            if (!rawHistory.equals(historyStamp[0])) {
                historyStamp[0] = rawHistory;
                renderHistory(activity, historyList, historyCount, Storage.history(activity));
            }
            handler.postDelayed(updater[0], 700);
        };

        Runnable updateNow = () -> {
            handler.removeCallbacks(updater[0]);
            updater[0].run();
        };
        pause.setOnClickListener(v -> {
            SyncScheduler.pause(activity);
            Ui.toast(activity, "Pošiljanje je na pavzi");
            updateNow.run();
        });
        start.setOnClickListener(v -> {
            SyncScheduler.resume(activity);
            Ui.toast(activity, "Pošiljanje se nadaljuje");
            updateNow.run();
        });
        restart.setOnClickListener(v -> {
            SyncScheduler.resume(activity);
            int queued = Storage.queueAllRecords(activity);
            Ui.toast(activity, "Znova pošiljam " + queued + " zapisov");
            updateNow.run();
        });
        stop.setOnClickListener(v -> {
            SyncScheduler.stop(activity);
            Ui.toast(activity, "Pošiljanje je ustavljeno; čakalna vrsta je ohranjena");
            updateNow.run();
        });
        refresh.setOnClickListener(v -> {
            Storage.refreshStatus(activity);
            SyncScheduler.scheduleNow(activity);
            updateNow.run();
        });
        deleteQueue.setOnClickListener(v -> new AlertDialog.Builder(activity)
                .setTitle("Izbrišem čakalno vrsto?")
                .setMessage("Odstranjena bodo samo čakajoča opravila. Lokalni posnetki in opombe ostanejo shranjeni.")
                .setPositiveButton("Izbriši", (d, which) -> {
                    int removed = Storage.clearPendingQueue(activity);
                    SyncScheduler.resume(activity);
                    Ui.toast(activity, "Odstranjenih opravil: " + removed);
                    updateNow.run();
                })
                .setNegativeButton("Prekliči", null)
                .show());
        clearHistory.setOnClickListener(v -> {
            Storage.clearHistory(activity);
            Ui.toast(activity, "Zgodovina poslanih je počiščena");
            updateNow.run();
        });
        chooseFolder.setOnClickListener(v -> {
            dialog.dismiss();
            if (onChooseFolder != null) onChooseFolder.run();
        });

        dialog.setOnShowListener(v -> updater[0].run());
        dialog.setOnDismissListener(v -> {
            handler.removeCallbacks(updater[0]);
            if (onChanged != null) onChanged.run();
        });
        dialog.show();
    }

    private static String size(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1048576d);
    }

    private static void renderHistory(Activity activity, LinearLayout list, TextView count,
                                      ArrayList<Storage.HistoryEntry> entries) {
        list.removeAllViews();
        count.setText(entries.size() == 1 ? "1 PRENOS" : entries.size() + " PRENOSOV");
        if (entries.isEmpty()) {
            TextView empty = Ui.text(activity,
                    "Še ni uspešnih prenosov. Ko Drive sprejme vse datoteke zapisa, se bodo prikazale tukaj.",
                    13, Ui.MUTED);
            empty.setPadding(Ui.dp(activity, 14), Ui.dp(activity, 13),
                    Ui.dp(activity, 14), Ui.dp(activity, 13));
            empty.setBackground(Ui.shape(0xFFF3F0FA, 12, activity));
            list.addView(empty);
            return;
        }

        SimpleDateFormat date = new SimpleDateFormat("dd. MMM yyyy · HH:mm:ss",
                new Locale("sl", "SI"));
        for (int index = 0; index < entries.size() && index < 12; index++) {
            Storage.HistoryEntry entry = entries.get(index);
            LinearLayout card = Ui.card(activity);
            card.setBackground(Ui.outlined(activity, 0xFFF7FCF9, 1));
            card.setPadding(Ui.dp(activity, 14), Ui.dp(activity, 13),
                    Ui.dp(activity, 14), Ui.dp(activity, 13));

            LinearLayout top = Ui.row(activity);
            TextView sent = Ui.text(activity, "✓  PRENESENO", 11, Ui.GREEN);
            sent.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            sent.setLetterSpacing(.05f);
            sent.setPadding(Ui.dp(activity, 9), Ui.dp(activity, 5),
                    Ui.dp(activity, 9), Ui.dp(activity, 5));
            sent.setBackground(Ui.shape(0xFFE4F6ED, 99, activity));
            top.addView(sent);
            TextView when = Ui.text(activity, date.format(new Date(entry.time)), 12, Ui.MUTED);
            when.setGravity(Gravity.END);
            top.addView(when, Ui.weight(0, -2, 1));
            card.addView(top);

            TextView project = Ui.text(activity, entry.project.isEmpty()
                    ? "Neznan projekt" : entry.project, 17, Ui.INK);
            project.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            card.addView(project);
            Ui.margin(project, 0, 10, 0, 0);

            if (!entry.title.isEmpty() && !"Brez naslova".equals(entry.title)) {
                TextView title = Ui.text(activity, entry.title, 14, Ui.INK);
                card.addView(title);
                Ui.margin(title, 0, 3, 0, 0);
            }

            TextView record = Ui.text(activity, "Zapis  ·  " + entry.record, 12, Ui.MUTED);
            record.setTypeface(Typeface.MONOSPACE);
            card.addView(record);
            Ui.margin(record, 0, 6, 0, 0);

            if (!entry.destination.isEmpty()) {
                TextView target = Ui.text(activity, "Cilj  ·  " + entry.destination, 12, Ui.MUTED);
                card.addView(target);
                Ui.margin(target, 0, 4, 0, 0);
            }

            StringBuilder names = new StringBuilder();
            for (String file : entry.files) {
                if (names.length() > 0) names.append("  •  ");
                names.append(file);
            }
            String total = entry.files.size() + (entry.files.size() == 1
                    ? " datoteka" : " datoteke")
                    + (entry.bytes > 0 ? "  ·  " + size(entry.bytes) : "");
            TextView fileSummary = Ui.text(activity, total, 13, Ui.GREEN);
            fileSummary.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            card.addView(fileSummary);
            Ui.margin(fileSummary, 0, 9, 0, 0);
            if (names.length() > 0) {
                TextView files = Ui.text(activity, names.toString(), 12, Ui.MUTED);
                files.setLineSpacing(0, 1.12f);
                card.addView(files);
                Ui.margin(files, 0, 4, 0, 0);
            }

            list.addView(card);
            if (index + 1 < entries.size() && index < 11) Ui.margin(card, 0, 0, 0, 8);
        }
    }
}
