package si.screenme.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class UpdateChecker {
    static final String DEFAULT_URL = "https://raw.githubusercontent.com/TeckTek/ScreenMe/main/update.json";
    static final String CHANNEL_ID = "screenme_updates";
    static final String PREF_LAST_CHECK = "updateLastCheck";
    static final String PREF_REMOTE_CODE = "updateRemoteCode";
    static final String PREF_REMOTE_NAME = "updateRemoteName";
    static final String PREF_APK_URL = "updateApkUrl";
    static final String PREF_ERROR = "updateLastError";
    static final String PREF_PENDING_NOTICE = "updatePendingNotice";

    interface Callback {
        void complete(UpdateInfo info, Exception error);
    }

    private UpdateChecker() {}

    static void check(Context context, boolean notify, Callback callback) {
        Context app = context.getApplicationContext();
        new Thread(() -> {
            UpdateInfo info = null;
            Exception failure = null;
            HttpURLConnection connection = null;
            try {
                SharedPreferences prefs = prefs(app);
                String source = prefs.getString("updateUrl", DEFAULT_URL).trim();
                if (source.isEmpty()) throw new IOException("Vir posodobitev ni nastavljen");
                connection = (HttpURLConnection) new URL(source).openConnection();
                connection.setConnectTimeout(15_000);
                connection.setReadTimeout(15_000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Cache-Control", "no-cache");
                int status = connection.getResponseCode();
                if (status < 200 || status >= 300) {
                    throw new IOException("Strežnik je vrnil HTTP " + status);
                }
                try (InputStream input = connection.getInputStream()) {
                    info = UpdateInfo.parse(new String(read(input), StandardCharsets.UTF_8));
                }
                boolean available = info.versionCode > installedVersionCode(app);
                SharedPreferences.Editor edit = prefs.edit()
                        .putLong(PREF_LAST_CHECK, System.currentTimeMillis())
                        .putLong(PREF_REMOTE_CODE, info.versionCode)
                        .putString(PREF_REMOTE_NAME, info.versionName)
                        .putString(PREF_APK_URL, info.apkUrl)
                        .putBoolean(PREF_PENDING_NOTICE, available)
                        .remove(PREF_ERROR);
                edit.apply();
                if (available && notify && postNotification(app, info)) {
                    prefs.edit().putBoolean(PREF_PENDING_NOTICE, false).apply();
                }
            } catch (Exception e) {
                failure = e;
                prefs(app).edit()
                        .putLong(PREF_LAST_CHECK, System.currentTimeMillis())
                        .putString(PREF_ERROR, readableError(e))
                        .apply();
            } finally {
                if (connection != null) connection.disconnect();
                if (callback != null) callback.complete(info, failure);
            }
        }, "ScreenMe update check").start();
    }

    static UpdateInfo available(Context context) {
        SharedPreferences prefs = prefs(context);
        long remote = prefs.getLong(PREF_REMOTE_CODE, 0);
        String name = prefs.getString(PREF_REMOTE_NAME, "");
        String url = prefs.getString(PREF_APK_URL, "");
        if (remote <= installedVersionCode(context) || name.isEmpty() || url.isEmpty()) return null;
        return new UpdateInfo(remote, name, url);
    }

    static long installedVersionCode(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    static boolean canNotify(Context context) {
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return false;
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (!manager.areNotificationsEnabled()) return false;
        NotificationChannel channel = manager.getNotificationChannel(CHANNEL_ID);
        return channel == null || channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
    }

    static void openDownload(Context context, String url) {
        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (!(context instanceof android.app.Activity)) view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(view);
    }

    static String lastStatus(Context context) {
        SharedPreferences prefs = prefs(context);
        UpdateInfo update = available(context);
        if (update != null) return "Na voljo je ScreenMe " + update.versionName + ".";
        String error = prefs.getString(PREF_ERROR, "");
        if (!error.isEmpty()) return "Zadnje preverjanje ni uspelo: " + error;
        long checked = prefs.getLong(PREF_LAST_CHECK, 0);
        if (checked == 0) return "Posodobitve še niso bile preverjene.";
        return "ScreenMe je posodobljen. Zadnje preverjanje: "
                + android.text.format.DateFormat.format("dd. MM. HH:mm", checked) + ".";
    }

    private static boolean postNotification(Context context, UpdateInfo info) {
        if (!canNotify(context)) return false;
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL_ID, "Posodobitve ScreenMe", NotificationManager.IMPORTANCE_HIGH));
        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl));
        PendingIntent pending = PendingIntent.getActivity(context, 92, view,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_screenme_mono)
                .setContentTitle("Na voljo je ScreenMe " + info.versionName)
                .setContentText("Dotakni se za varen prenos posodobitve")
                .setCategory(Notification.CATEGORY_STATUS)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build();
        manager.notify(92, notification);
        return true;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("screenme", Context.MODE_PRIVATE);
    }

    private static String readableError(Exception error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? error.getClass().getSimpleName() : message.trim();
    }

    private static byte[] read(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) > 0) output.write(buffer, 0, count);
        return output.toByteArray();
    }
}
