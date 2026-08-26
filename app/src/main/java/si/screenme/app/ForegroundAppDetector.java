package si.screenme.app;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Process;

final class ForegroundAppDetector {
    private static final long LOOKBACK_MS = 60_000L;

    private ForegroundAppDetector() {}

    static boolean hasAccess(Context context) {
        AppOpsManager ops = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (ops == null) return false;
        int mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    static String selectForegroundProject(Context context) {
        if (!context.getSharedPreferences("screenme", 0)
                .getBoolean("autoProject", false) || !hasAccess(context)) return null;
        String packageName = foregroundPackage(context);
        if (packageName == null) return null;
        String label = appLabel(context, packageName);
        ProjectStore.selectForPackage(context, packageName, label);
        return label;
    }

    static String foregroundPackage(Context context) {
        UsageStatsManager manager = (UsageStatsManager)
                context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager == null) return null;
        long end = System.currentTimeMillis();
        UsageEvents events = manager.queryEvents(end - LOOKBACK_MS, end);
        UsageEvents.Event event = new UsageEvents.Event();
        String latest = null;
        long latestAt = 0;
        String home = homePackage(context);
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            int type = event.getEventType();
            if (type != UsageEvents.Event.ACTIVITY_RESUMED
                    && type != UsageEvents.Event.MOVE_TO_FOREGROUND) continue;
            String candidate = event.getPackageName();
            if (candidate == null || candidate.equals(context.getPackageName())
                    || candidate.equals("com.android.systemui") || candidate.equals(home)) continue;
            if (event.getTimeStamp() >= latestAt) {
                latest = candidate;
                latestAt = event.getTimeStamp();
            }
        }
        return latest;
    }

    static String appLabel(Context context, String packageName) {
        try {
            PackageManager manager = context.getPackageManager();
            ApplicationInfo info = manager.getApplicationInfo(packageName, 0);
            CharSequence label = manager.getApplicationLabel(info);
            if (label != null && !label.toString().trim().isEmpty()) return label.toString().trim();
        } catch (PackageManager.NameNotFoundException ignored) {}
        int dot = packageName.lastIndexOf('.');
        String fallback = dot >= 0 ? packageName.substring(dot + 1) : packageName;
        return fallback.isEmpty() ? packageName : fallback;
    }

    private static String homePackage(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        android.content.pm.ResolveInfo info = context.getPackageManager().resolveActivity(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return info == null || info.activityInfo == null ? null : info.activityInfo.packageName;
    }
}
