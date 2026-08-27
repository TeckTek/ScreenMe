package si.screenme.app;

import org.json.JSONObject;

final class UpdateInfo {
    final long versionCode;
    final String versionName;
    final String apkUrl;

    UpdateInfo(long versionCode, String versionName, String apkUrl) {
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.apkUrl = apkUrl;
    }

    static UpdateInfo parse(String raw) throws Exception {
        JSONObject json = new JSONObject(raw);
        long versionCode = json.getLong("versionCode");
        String versionName = json.getString("versionName").trim();
        String apkUrl = json.getString("apkUrl").trim();
        if (versionCode < 1 || versionName.isEmpty()
                || !(apkUrl.startsWith("https://") || apkUrl.startsWith("http://"))) {
            throw new IllegalArgumentException("Neveljaven manifest posodobitve");
        }
        return new UpdateInfo(versionCode, versionName, apkUrl);
    }
}
