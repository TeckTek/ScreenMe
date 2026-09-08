package si.screenme.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.DocumentsContract;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

final class Storage {
    static final String PREF_PENDING = "syncPendingCount";
    static final String PREF_ERROR = "syncLastError";
    static final String PREF_SUCCESS = "syncLastSuccess";
    static final String PREF_BATCH_TOTAL = "syncBatchTotal";
    static final String PREF_BATCH_DONE = "syncBatchDone";
    static final String PREF_CURRENT = "syncCurrentRecord";
    static final String PREF_FILE_INDEX = "syncCurrentFileIndex";
    static final String PREF_FILE_TOTAL = "syncCurrentFileTotal";
    static final String PREF_CURRENT_FILE = "syncCurrentFile";
    static final String PREF_FILE_BYTES = "syncCurrentFileBytes";
    static final String PREF_FILE_SIZE = "syncCurrentFileSize";
    static final String PREF_HISTORY = "syncHistory";
    static final String PREF_CLEANUP_COUNT = "syncCleanupCount";
    static final String PREF_CLEANUP_TIME = "syncCleanupTime";
    static final String ACTION_SYNC_STATUS = "si.screenme.app.SYNC_STATUS";
    private static final String PENDING_FILE = ".sync-pending";
    private static final Object SYNC_LOCK = new Object();

    interface StopCheck { boolean stopped(); }

    static final class SyncResult {
        int succeeded;
        int failed;
    }

    private static final class RemoteDirectory {
        final Uri uri;
        final boolean created;

        RemoteDirectory(Uri uri, boolean created) {
            this.uri = uri;
            this.created = created;
        }
    }

    static final class HistoryEntry {
        long time;
        String project = "";
        String record = "";
        String title = "";
        String destination = "";
        long bytes;
        final ArrayList<String> files = new ArrayList<>();
    }

    private Storage() {}

    static File newRecord(Context context) {
        String project = context.getSharedPreferences("screenme", 0)
                .getString("profile", "Default");
        return newRecord(context, project);
    }

    static File newRecord(Context context, String project) {
        File root = ProjectStore.folder(context, project);
        if (!root.exists()) root.mkdirs();
        String base = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.ROOT)
                .format(new Date());
        File record = new File(root, base);
        int suffix = 2;
        while (record.exists()) record = new File(root, base + "-" + suffix++);
        record.mkdirs();
        return record;
    }

    static boolean bitmap(Bitmap bitmap, File file) {
        try (FileOutputStream output = new FileOutputStream(file)) {
            return bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        } catch (Exception error) {
            return false;
        }
    }

    static void text(File file, String value) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(value);
        } catch (Exception ignored) {}
    }

    static void sync(Context context, File record) {
        SharedPreferences prefs = context.getSharedPreferences("screenme", 0);
        if (prefs.getString("syncTree", "").isEmpty()) return;
        int before = countPending(context);
        boolean alreadyPending = new File(record, PENDING_FILE).isFile();
        markPending(record);
        int pending = countPending(context);
        SharedPreferences.Editor edit = prefs.edit().putInt(PREF_PENDING, pending);
        if (!alreadyPending) {
            if (before == 0) edit.putInt(PREF_BATCH_TOTAL, 1).putInt(PREF_BATCH_DONE, 0);
            else edit.putInt(PREF_BATCH_TOTAL, Math.max(prefs.getInt(PREF_BATCH_TOTAL, 0),
                    prefs.getInt(PREF_BATCH_DONE, 0) + pending));
        }
        edit.apply();
        SyncScheduler.runNow(context);
    }

    static int queueAllRecords(Context context) {
        if (context.getSharedPreferences("screenme", 0).getString("syncTree", "").isEmpty()) {
            return 0;
        }
        File external = context.getExternalFilesDir(null);
        File projects = external == null ? null : new File(external, "ScreenMe/projekti");
        File[] projectDirs = projects == null ? null : projects.listFiles(File::isDirectory);
        if (projectDirs != null) {
            for (File project : projectDirs) {
                File[] records = project.listFiles(File::isDirectory);
                if (records == null) continue;
                for (File record : records) {
                    if (!new File(record, "screenshot.png").isFile()) continue;
                    markPending(record);
                }
            }
        }
        int pending = countPending(context);
        context.getSharedPreferences("screenme", 0).edit()
                .putInt(PREF_PENDING, pending)
                .putInt(PREF_BATCH_TOTAL, pending)
                .putInt(PREF_BATCH_DONE, 0)
                .remove(PREF_CURRENT).remove(PREF_CURRENT_FILE)
                .putInt(PREF_FILE_INDEX, 0).putInt(PREF_FILE_TOTAL, 0)
                .putLong(PREF_FILE_BYTES, 0).putLong(PREF_FILE_SIZE, 0)
                .remove(PREF_ERROR).apply();
        if (pending > 0) SyncScheduler.runNow(context);
        return pending;
    }

    static void requeueForUpgrade(Context context, int repairVersion) {
        SharedPreferences prefs = context.getSharedPreferences("screenme", 0);
        if (prefs.getInt("syncRepairVersion", 0) >= repairVersion) return;
        prefs.edit().putInt("syncRepairVersion", repairVersion).apply();
        if (!prefs.getString("syncTree", "").isEmpty()) queueAllRecords(context);
    }

    static SyncResult syncPending(Context context, StopCheck stopCheck) {
        synchronized (SYNC_LOCK) {
            SyncResult result = new SyncResult();
            SharedPreferences prefs = context.getSharedPreferences("screenme", 0);
            String raw = prefs.getString("syncTree", "");
            if (raw.isEmpty()) return result;
            ArrayList<File> pending = pendingRecords(context);
            if (pending.size() > prefs.getInt(PREF_BATCH_TOTAL, 0)) {
                prefs.edit().putInt(PREF_BATCH_TOTAL, pending.size())
                        .putInt(PREF_BATCH_DONE, 0).apply();
            }
            String lastError = "";
            int[] cleaned = new int[]{0};
            for (File record : pending) {
                if ((stopCheck != null && stopCheck.stopped())
                        || !SyncScheduler.isRunning(context)) break;
                prefs.edit().putString(PREF_CURRENT, record.getParentFile().getName()
                        + " / " + record.getName()).putInt(PREF_FILE_INDEX, 0)
                        .putInt(PREF_FILE_TOTAL, 0).putLong(PREF_FILE_BYTES, 0)
                        .putLong(PREF_FILE_SIZE, 0).remove(PREF_CURRENT_FILE).apply();
                broadcastStatus(context);
                try {
                    syncRecordWithRetries(context, Uri.parse(raw), record, stopCheck, cleaned);
                    if ((stopCheck != null && stopCheck.stopped())
                            || !SyncScheduler.isRunning(context)) break;
                    File marker = new File(record, PENDING_FILE);
                    if (marker.exists() && !marker.delete()) {
                        throw new IOException("Končanega prenosa ni mogoče označiti");
                    }
                    result.succeeded++;
                    addHistory(context, record);
                    int done = prefs.getInt(PREF_BATCH_DONE, 0) + 1;
                    prefs.edit().putInt(PREF_BATCH_DONE, done)
                            .putInt(PREF_PENDING, countPending(context))
                            .remove(PREF_CURRENT).remove(PREF_CURRENT_FILE)
                            .putInt(PREF_FILE_INDEX, 0).putInt(PREF_FILE_TOTAL, 0)
                            .putLong(PREF_FILE_BYTES, 0).putLong(PREF_FILE_SIZE, 0)
                            .remove(PREF_ERROR).putLong(PREF_SUCCESS, System.currentTimeMillis())
                            .apply();
                    broadcastStatus(context);
                } catch (Exception error) {
                    if ((stopCheck != null && stopCheck.stopped())
                            || !SyncScheduler.isRunning(context)) break;
                    result.failed++;
                    lastError = readable(error);
                }
            }
            if ((stopCheck == null || !stopCheck.stopped())
                    && SyncScheduler.isRunning(context)) {
                prefs.edit().putInt(PREF_CLEANUP_COUNT, cleaned[0])
                        .putLong(PREF_CLEANUP_TIME, System.currentTimeMillis()).apply();
            }
            SharedPreferences.Editor edit = prefs.edit()
                    .putInt(PREF_PENDING, countPending(context));
            if (result.succeeded > 0) edit.putLong(PREF_SUCCESS, System.currentTimeMillis());
            if (result.failed > 0) edit.putString(PREF_ERROR, lastError);
            else edit.remove(PREF_ERROR);
            edit.apply();
            broadcastStatus(context);
            return result;
        }
    }

    static int countPending(Context context) {
        return pendingRecords(context).size();
    }

    static int refreshStatus(Context context) {
        int pending = countPending(context);
        SharedPreferences prefs = context.getSharedPreferences("screenme", 0);
        SharedPreferences.Editor edit = prefs.edit().putInt(PREF_PENDING, pending);
        if (pending == 0) {
            edit.remove(PREF_CURRENT).remove(PREF_CURRENT_FILE)
                    .putInt(PREF_FILE_INDEX, 0).putInt(PREF_FILE_TOTAL, 0)
                    .putLong(PREF_FILE_BYTES, 0).putLong(PREF_FILE_SIZE, 0);
        } else {
            edit.putInt(PREF_BATCH_TOTAL, Math.max(prefs.getInt(PREF_BATCH_TOTAL, 0),
                    prefs.getInt(PREF_BATCH_DONE, 0) + pending));
        }
        edit.apply();
        broadcastStatus(context);
        return pending;
    }

    static int clearPendingQueue(Context context) {
        int removed = 0;
        for (File record : pendingRecords(context)) {
            File marker = new File(record, PENDING_FILE);
            if (marker.isFile() && marker.delete()) removed++;
        }
        context.getSharedPreferences("screenme", 0).edit()
                .putInt(PREF_PENDING, countPending(context))
                .putInt(PREF_BATCH_TOTAL, 0).putInt(PREF_BATCH_DONE, 0)
                .putInt(PREF_FILE_INDEX, 0).putInt(PREF_FILE_TOTAL, 0)
                .putLong(PREF_FILE_BYTES, 0).putLong(PREF_FILE_SIZE, 0)
                .remove(PREF_CURRENT).remove(PREF_CURRENT_FILE).remove(PREF_ERROR).apply();
        SyncScheduler.cancel(context);
        broadcastStatus(context);
        return removed;
    }

    static ArrayList<HistoryEntry> history(Context context) {
        String raw = context.getSharedPreferences("screenme", 0)
                .getString(PREF_HISTORY, "").trim();
        ArrayList<HistoryEntry> result = new ArrayList<>();
        if (raw.isEmpty()) return result;
        if (raw.startsWith("[")) {
            try {
                JSONArray items = new JSONArray(raw);
                for (int index = 0; index < items.length() && result.size() < 50; index++) {
                    JSONObject item = items.optJSONObject(index);
                    if (item == null) continue;
                    HistoryEntry entry = new HistoryEntry();
                    entry.time = item.optLong("time", 0);
                    entry.project = item.optString("project", "");
                    entry.record = item.optString("record", "");
                    entry.title = item.optString("title", "");
                    entry.destination = item.optString("destination", "");
                    entry.bytes = item.optLong("bytes", 0);
                    JSONArray files = item.optJSONArray("files");
                    if (files != null) for (int file = 0; file < files.length(); file++) {
                        String name = files.optString(file, "");
                        if (!name.isEmpty()) entry.files.add(name);
                    }
                    if (entry.time > 0) result.add(entry);
                }
                return result;
            } catch (Exception ignored) {
                result.clear();
            }
        }

        // Read the compact history format used by ScreenMe 0.4.4–0.4.6.
        for (String row : raw.split("\\n")) {
            int separator = row.indexOf('\t');
            if (separator < 1 || result.size() >= 50) continue;
            try {
                HistoryEntry entry = new HistoryEntry();
                entry.time = Long.parseLong(row.substring(0, separator));
                String key = row.substring(separator + 1);
                int slash = key.indexOf(" / ");
                entry.project = slash < 0 ? key : key.substring(0, slash);
                entry.record = slash < 0 ? "" : key.substring(slash + 3);
                File local = entry.record.isEmpty() ? null
                        : new File(ProjectStore.folder(context, entry.project), entry.record);
                if (local != null && local.isDirectory()) fillHistoryFiles(context, entry, local);
                result.add(entry);
            } catch (Exception ignored) {}
        }
        return result;
    }

    static String historyText(Context context) {
        ArrayList<HistoryEntry> entries = history(context);
        if (entries.isEmpty()) return "Še ni uspešno prenesenih zapisov.";
        SimpleDateFormat format = new SimpleDateFormat("dd. MM. yyyy  HH:mm:ss", Locale.ROOT);
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < entries.size() && index < 12; index++) {
            HistoryEntry entry = entries.get(index);
            if (result.length() > 0) result.append('\n');
            result.append("✓ ").append(format.format(new Date(entry.time))).append("  ·  ")
                    .append(entry.project).append(" / ").append(entry.record);
            if (!entry.files.isEmpty()) result.append("  ·  ").append(entry.files.size())
                    .append(" datotek");
        }
        return result.toString();
    }

    static void clearHistory(Context context) {
        int pending = countPending(context);
        context.getSharedPreferences("screenme", 0).edit()
                .remove(PREF_HISTORY).putInt(PREF_BATCH_DONE, 0)
                .putInt(PREF_BATCH_TOTAL, pending).apply();
        broadcastStatus(context);
    }

    private static ArrayList<File> pendingRecords(Context context) {
        ArrayList<File> result = new ArrayList<>();
        File external = context.getExternalFilesDir(null);
        File projects = external == null ? null : new File(external, "ScreenMe/projekti");
        File[] projectDirs = projects == null ? null : projects.listFiles(File::isDirectory);
        if (projectDirs == null) return result;
        for (File project : projectDirs) {
            File[] records = project.listFiles(File::isDirectory);
            if (records == null) continue;
            for (File record : records) {
                if (new File(record, PENDING_FILE).isFile()) result.add(record);
            }
        }
        return result;
    }

    private static boolean markPending(File record) {
        try {
            File marker = new File(record, PENDING_FILE);
            return marker.exists() || marker.createNewFile();
        } catch (Exception error) {
            return false;
        }
    }

    private static void updatePendingCount(Context context) {
        context.getSharedPreferences("screenme", 0).edit()
                .putInt(PREF_PENDING, countPending(context)).apply();
    }

    private static void syncRecord(Context context, Uri tree, File record,
                                   int[] cleaned) throws Exception {
        File[] files = record.listFiles(file -> file.isFile()
                && !file.getName().equals(PENDING_FILE));
        if (files == null || files.length == 0) throw new IOException("Lokalni zapis je prazen");
        Arrays.sort(files, (left, right) -> Integer.compare(order(left), order(right)));
        Uri root = DocumentsContract.buildDocumentUriUsingTree(
                tree, DocumentsContract.getTreeDocumentId(tree));
        boolean direct = context.getSharedPreferences("screenme", 0)
                .getBoolean("syncDirect", false);
        Uri destination = direct ? root : dir(context, root, "ScreenMe");
        boolean turbo = context.getSharedPreferences("screenme", 0)
                .getBoolean("turbo", false);
        if (turbo) remoteTextIfMissing(context, destination,
                "TURBO_PROTOCOL.md", protocol());
        RemoteDirectory project = remoteDirectory(context, destination,
                record.getParentFile().getName());
        RemoteDirectory target = null;
        try {
            target = remoteDirectory(context, project.uri, record.getName());
            for (int index = 0; index < files.length; index++) {
                updateFileProgress(context, files[index], index + 1, files.length);
                copy(context, target.uri, files[index]);
            }
            if (turbo) {
                File meta = new File(record, "metadata.json");
                String title = RecordItem.json(meta, "title", "Brez naslova");
                String profile = RecordItem.json(meta, "profile", record.getParentFile().getName());
                String status = "{\n  \"protocolVersion\": 1,\n  \"state\": \"NEW\",\n"
                        + "  \"recordId\": \"" + esc(record.getParentFile().getName() + "/"
                        + record.getName()) + "\",\n  \"project\": \"" + esc(profile)
                        + "\",\n  \"title\": \"" + esc(title) + "\",\n  \"createdAt\": \""
                        + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT)
                        .format(new Date()) + "\"\n}\n";
                remoteTextIfMissing(context, target.uri, "turbo-status.json", status);
            }
        } catch (Exception error) {
            cleaned[0] += cleanupCreatedDirectories(context, project, target);
            throw error;
        }
    }

    private static void syncRecordWithRetries(Context context, Uri tree, File record,
                                              StopCheck stopCheck, int[] cleaned) throws Exception {
        Exception lastError = null;
        for (int attempt = 1; attempt <= 4; attempt++) {
            if (stopCheck != null && stopCheck.stopped()) {
                throw new IOException("Pošiljanje je bilo začasno prekinjeno");
            }
            try {
                syncRecord(context, tree, record, cleaned);
                return;
            } catch (Exception error) {
                lastError = error;
                if (attempt < 4) Thread.sleep(attempt * 750L);
            }
        }
        throw new IOException("Drive po štirih poskusih ni sprejel datotek: "
                + readable(lastError), lastError);
    }

    private static int order(File file) {
        String name = file.getName();
        if (name.equals("screenshot.png")) return 0;
        if (name.equals("annotated.png")) return 1;
        if (name.equals("note.md")) return 2;
        if (name.equals("metadata.json")) return 3;
        return 4;
    }

    private static void updateFileProgress(Context context, File file, int index, int total) {
        context.getSharedPreferences("screenme", 0).edit()
                .putString(PREF_CURRENT_FILE, file.getName())
                .putInt(PREF_FILE_INDEX, index).putInt(PREF_FILE_TOTAL, total)
                .putLong(PREF_FILE_BYTES, 0).putLong(PREF_FILE_SIZE, file.length()).apply();
        broadcastStatus(context);
    }

    private static void updateByteProgress(Context context, long copied, long size) {
        context.getSharedPreferences("screenme", 0).edit()
                .putLong(PREF_FILE_BYTES, copied).putLong(PREF_FILE_SIZE, size).apply();
        broadcastStatus(context);
    }

    private static void addHistory(Context context, File record) {
        SharedPreferences prefs = context.getSharedPreferences("screenme", 0);
        HistoryEntry newest = new HistoryEntry();
        newest.time = System.currentTimeMillis();
        newest.project = record.getParentFile().getName();
        newest.record = record.getName();
        fillHistoryFiles(context, newest, record);

        ArrayList<HistoryEntry> entries = history(context);
        JSONArray stored = new JSONArray();
        stored.put(historyJson(newest));
        int kept = 1;
        for (HistoryEntry entry : entries) {
            if (entry.project.equals(newest.project) && entry.record.equals(newest.record)) continue;
            if (kept++ >= 50) break;
            stored.put(historyJson(entry));
        }
        prefs.edit().putString(PREF_HISTORY, stored.toString()).apply();
    }

    private static void fillHistoryFiles(Context context, HistoryEntry entry, File record) {
        File[] copied = record.listFiles(file -> file.isFile()
                && !file.getName().equals(PENDING_FILE));
        if (copied != null) {
            Arrays.sort(copied, (left, right) -> Integer.compare(order(left), order(right)));
            for (File file : copied) {
                entry.files.add(file.getName());
                entry.bytes += Math.max(0, file.length());
            }
        }
        if (context.getSharedPreferences("screenme", 0).getBoolean("turbo", false)
                && !entry.files.contains("turbo-status.json")) {
            entry.files.add("turbo-status.json");
        }
        File metadata = new File(record, "metadata.json");
        entry.title = RecordItem.json(metadata, "title", "");
        entry.destination = syncDestinationName(context);
    }

    private static JSONObject historyJson(HistoryEntry entry) {
        JSONObject item = new JSONObject();
        try {
            item.put("time", entry.time);
            item.put("project", entry.project);
            item.put("record", entry.record);
            item.put("title", entry.title);
            item.put("destination", entry.destination);
            item.put("bytes", entry.bytes);
            JSONArray files = new JSONArray();
            for (String file : entry.files) files.put(file);
            item.put("files", files);
        } catch (Exception ignored) {}
        return item;
    }

    static void rememberSyncDestination(Context context, Uri tree) {
        String provider = tree.getAuthority() != null
                && tree.getAuthority().contains("google") ? "Google Drive" : "Oblačna mapa";
        String name = "";
        try {
            Uri root = DocumentsContract.buildDocumentUriUsingTree(
                    tree, DocumentsContract.getTreeDocumentId(tree));
            try (android.database.Cursor cursor = context.getContentResolver().query(root,
                    new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                    null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0);
            }
        } catch (Exception ignored) {}
        String label = name == null || name.trim().isEmpty() ? provider : provider + " / " + name;
        context.getSharedPreferences("screenme", 0).edit()
                .putString("syncFolderName", label).apply();
    }

    static String syncDestinationName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("screenme", 0);
        String name = prefs.getString("syncFolderName", "");
        if (!name.isEmpty()) return name;
        String raw = prefs.getString("syncTree", "");
        if (!raw.isEmpty()) rememberSyncDestination(context, Uri.parse(raw));
        return prefs.getString("syncFolderName", "Google Drive");
    }

    private static void broadcastStatus(Context context) {
        context.sendBroadcast(new Intent(ACTION_SYNC_STATUS).setPackage(context.getPackageName()));
    }

    private static Uri dir(Context context, Uri parent, String name) throws Exception {
        return remoteDirectory(context, parent, name).uri;
    }

    private static RemoteDirectory remoteDirectory(Context context, Uri parent, String name)
            throws Exception {
        try (android.database.Cursor cursor = context.getContentResolver().query(
                DocumentsContract.buildChildDocumentsUriUsingTree(parent,
                        DocumentsContract.getDocumentId(parent)),
                new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                if (name.equals(cursor.getString(1))
                        && DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2))) {
                    return new RemoteDirectory(DocumentsContract.buildDocumentUriUsingTree(
                            parent, cursor.getString(0)), false);
                }
            }
        }
        Uri created = DocumentsContract.createDocument(context.getContentResolver(), parent,
                DocumentsContract.Document.MIME_TYPE_DIR, name);
        if (created == null) throw new IOException("Mape »" + name + "« ni mogoče ustvariti");
        return new RemoteDirectory(created, true);
    }

    private static int cleanupCreatedDirectories(Context context, RemoteDirectory project,
                                                 RemoteDirectory target) {
        int removed = 0;
        boolean targetRemoved = target == null;
        if (target != null && target.created) {
            try {
                targetRemoved = DocumentsContract.deleteDocument(
                        context.getContentResolver(), target.uri);
                if (targetRemoved) removed++;
            } catch (Exception ignored) {
                targetRemoved = false;
            }
        }
        if (project.created && targetRemoved && (target == null || target.created)) {
            try {
                if (DocumentsContract.deleteDocument(context.getContentResolver(), project.uri)) {
                    removed++;
                }
            } catch (Exception ignored) {}
        }
        return removed;
    }

    private static void copy(Context context, Uri parent, File file) throws Exception {
        String mime = file.getName().endsWith(".png") ? "image/png"
                : file.getName().endsWith(".json") ? "application/json" : "text/markdown";
        Uri outputUri = child(context, parent, file.getName());
        if (outputUri == null) {
            outputUri = DocumentsContract.createDocument(context.getContentResolver(),
                    parent, mime, file.getName());
        }
        if (outputUri == null) throw new IOException("Datoteke »" + file.getName()
                + "« ni mogoče ustvariti");
        try (InputStream input = new FileInputStream(file);
             OutputStream output = openForWrite(context, outputUri)) {
            if (output == null) throw new IOException("Datoteke »" + file.getName()
                    + "« ni mogoče odpreti za pisanje");
            byte[] buffer = new byte[64 * 1024];
            int count;
            long copied = 0, reported = 0, size = file.length();
            while ((count = input.read(buffer)) > 0) {
                output.write(buffer, 0, count);
                copied += count;
                if (copied - reported >= 256 * 1024 || copied == size) {
                    reported = copied;
                    updateByteProgress(context, copied, size);
                }
            }
            output.flush();
        }
    }

    private static Uri child(Context context, Uri parent, String name) throws Exception {
        try (android.database.Cursor cursor = context.getContentResolver().query(
                DocumentsContract.buildChildDocumentsUriUsingTree(parent,
                        DocumentsContract.getDocumentId(parent)),
                new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                if (name.equals(cursor.getString(1))) {
                    return DocumentsContract.buildDocumentUriUsingTree(parent, cursor.getString(0));
                }
            }
        }
        return null;
    }

    private static void remoteTextIfMissing(Context context, Uri parent,
                                            String name, String value) throws Exception {
        if (child(context, parent, name) != null) return;
        Uri outputUri = DocumentsContract.createDocument(context.getContentResolver(), parent,
                name.endsWith(".json") ? "application/json" : "text/markdown", name);
        if (outputUri == null) throw new IOException("Datoteke »" + name + "« ni mogoče ustvariti");
        try (OutputStream output = openForWrite(context, outputUri)) {
            if (output == null) throw new IOException("Datoteke »" + name
                    + "« ni mogoče odpreti za pisanje");
            output.write(value.getBytes(StandardCharsets.UTF_8));
            output.flush();
        }
    }

    private static OutputStream openForWrite(Context context, Uri uri) throws Exception {
        Exception lastError = null;
        // Google Drive's Android document provider is most reliable with explicit
        // truncate mode. Other providers sometimes accept only the standard mode.
        for (String mode : new String[]{"wt", "w", "rwt"}) {
            try {
                OutputStream output = context.getContentResolver().openOutputStream(uri, mode);
                if (output != null) return output;
            } catch (Exception error) {
                lastError = error;
            }
        }
        throw new IOException("Datoteke ni mogoče odpreti za pisanje: "
                + readable(lastError), lastError);
    }

    static String protocol() {
        return "# ScreenMe Turbo Protocol v1\n\nVsaka mapa zapisa je pripravljena šele, "
                + "ko vsebuje `turbo-status.json`.\n\nStanja: `NEW`, `IN_PROGRESS`, `DONE`, "
                + "`NEEDS_INFO`.\n\nOb prevzemu nastavi `state` na `IN_PROGRESS` in dodaj "
                + "`worker`, `startedAt`. Ob zaključku nastavi `DONE` ter dodaj `result`, "
                + "`commit` in `finishedAt`. Ne briši izvornih datotek.\n";
    }

    static String json(String profile, String title, String severity, String note,
                       boolean edited) {
        return "{\n  \"schemaVersion\": 2,\n  \"profile\": \"" + esc(profile)
                + "\",\n  \"title\": \"" + esc(title) + "\",\n  \"severity\": \""
                + esc(severity) + "\",\n  \"note\": \"" + esc(note) + "\",\n  \"edited\": "
                + edited + ",\n  \"createdAt\": \""
                + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT)
                .format(new Date()) + "\"\n}";
    }

    static String esc(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private static String readable(Exception error) {
        if (error == null) return "neznana napaka";
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) return error.getClass().getSimpleName();
        return message.trim();
    }
}
